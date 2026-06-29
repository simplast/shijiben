# 事记本 迭代 6 设计：Release 构建配置（签名 + 混淆 + 缩减 + ProGuard 规则）

> 日期：2026-06-28
> 范围：本轮给 release 构建补齐四件事——(A) keystore 生成与 signingConfigs；(B) buildTypes.release 开 minify + shrinkResources；(C) 新建 proguard-rules.pro 最小防御 keep 集；(D) 验证门扩展第四道 `:app:assembleRelease`；(E) AGENT.md 构建与安装章节对齐。
> 前置：迭代 5 已落地，当前质量评分 87/100，95 个单测全绿。`buildConfig = true` 已铺路（`app/build.gradle.kts` 第 29 行）✓。Room schemas 目录已存在并 exportSchema=true 已工作 ✓。
> 验证门（**四道全绿才算过**）：
> 1. `./gradlew :app:compileDebugKotlin`
> 2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
> 3. `./gradlew assembleDebug`
> 4. **`./gradlew :app:assembleRelease`（本轮新增，永久保留）**

---

## 一、问题

### 1.1 现状

`app/build.gradle.kts` buildTypes.release（第 21–25 行）当前仅一行配置：

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
    }
}
```

具体缺失：
- **无签名配置**：`signingConfigs` 块完全不存在，`buildTypes.release` 也无 `signingConfig` 引用。`./gradlew :app:assembleRelease` 当前虽能跑通，但产物是未签名 APK，无法直接 `adb install` 安装到真机，仅适合 CI 验证编译/混淆通过。
- **无混淆缩减**：`isMinifyEnabled = false`（第 23 行），未开 R8，未开 `isShrinkResources`。release 产物与 debug 产物体积接近，无任何裁剪。
- **无 ProGuard 规则文件**：`app/proguard-rules.pro` 不存在，AGP 默认不会生成空文件。
- `.gitignore` 第 15 行已有 `/app/release`（防 release 产物入库），但无 `keystore/` 与 `keystore.properties` 条目——本轮新增 keystore 后必须补。
- `buildConfig = true` 已启用（迭代 5 §A.6 已落地），`BuildConfig.VERSION_NAME` 在 release 下可读 ✓。

### 1.2 为什么做

- **跨 $100 线**：当前 87/100，距 92 阈值差 5 分；release 构建配置是质量门"构建产物可发布"维度的硬缺口。
- **CI 不可验证发布产物**：三道门里第三道 `assembleDebug` 只验证 debug 产物，release 流程无人守护——任何 release-only 回归（如 R8 裁剪 Hilt 生成类）要到打包发布时才暴露。
- **产物不可直接安装**：未签名 release APK 无法 `adb install`，需手动 `apksigner` 才能测，阻碍 release 通道的快速验证。
- **Discover 报告风险评估：低**：源码零反射、注解全标准、依赖库自带 consumer-rules.pro。即便如此仍写最小防御 keep 集作双保险，覆盖 Hilt @Inject 构造 + Room `data.local.**` + Kotlin Metadata。

---

## 二、目标

- **A. Keystore 生成（S）**：用 keytool 生成自签名 dev keystore 到 `keystore/release.jks`（gitignored），凭据存 `keystore.properties`（gitignored），固定密码 `shijiben`。
- **B. signingConfigs.release（S）**：`app/build.gradle.kts` 加 `signingConfigs.release`，从 `keystore.properties` 读取凭据；文件缺失时回退为不签名（保证 debug 与全新 clone 不破）。
- **C. buildTypes.release 改造（S）**：`isMinifyEnabled = true` + `isShrinkResources = true` + `proguardFiles` + `signingConfig = signingConfigs.release`。
- **D. proguard-rules.pro 最小防御 keep 集（S）**：新建 `app/proguard-rules.pro`，Hilt + Room + Kotlin Metadata 三组 keep，每条带注释说明为何要 keep。
- **E. 验证门扩展（S）**：第四道门 `./gradlew :app:assembleRelease` 永久加入，CI 可跑，无需真机。
- **F. 文档对齐（S）**：`AGENT.md` 构建与安装章节补 release 构建 + keystore.properties 说明；Spec 索引补本轮 spec。

---

## 三、非目标

- **不做 UI 测试 / 不写真机自动化**：手动安装 release APK 到真机确认启动正常，记 backlog，不进入本轮验证门。
- **不做新功能 / 不改业务逻辑 / 不动 DB schema**：本轮纯构建配置 + 文档。
- **不做生产签名密钥管理**：本轮 keystore 是自签名 dev keystore，固定密码 `shijiben`，非生产发布密钥；生产签名（如 Play App Signing）不在本轮范围。
- **不联网**：keytool 是 JDK 自带本地工具，ProGuard/R8 是 AGP 内置本地任务，`keystore.properties` 是本地文件。零联网。
- **不优化 APK 体积策略**：仅开默认 R8 + resource shrink，不调 `enableR8.fullMode`、不加资源压缩工具（如 `shrinker`）、不做 ABI splits。
- **不替换 KSP / Kotlin 版本**：保持 AGP 8.7.3 / Kotlin 2.0.21 / KSP 2.0.21-1.0.27。

---

## 四、设计

### A. Keystore 生成

#### A.1 keytool 命令（精确）

```bash
keytool -genkeypair \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore keystore/release.jks \
  -storepass shijiben \
  -keypass shijiben \
  -dname "CN=ShiJiBen, OU=Dev, O=Local, L=Local, ST=Local, C=CN"
```

要点：
- 路径：项目根下 `keystore/release.jks`（即 `/Users/doer/dev/shijiben/keystore/release.jks`）。
- `storepass` 与 `keypass` 一致 = `shijiben`，简化 `keystore.properties` 字段。
- `-validity 10000`：约 27 年，远超 dev keystore 实际寿命。
- RSA 2048：Android 签名最低安全位数，AGP 8 接受。
- `dname` 全 Local：自签名 dev keystore，不暴露真实身份。
- 此命令由 Coding subagent 在落盘前手动执行一次（或 orchestrator 预生成），生成物不入库（gitignored）。

#### A.2 keystore.properties（新增，gitignored）

文件位置：项目根下 `keystore.properties`（即 `/Users/doer/dev/shijiben/keystore.properties`）。

```properties
# 自签名 dev keystore 凭据，gitignored，不入库
# 非生产发布密钥；若丢失或泄露，重新生成 keystore + 更新本文件即可
storeFile=keystore/release.jks
storePassword=shijiben
keyAlias=release
keyPassword=shijiben
```

设计要点：
- `storeFile` 用相对项目根的路径（`keystore/release.jks`），`build.gradle.kts` 中以 `rootProject.file(...)` 解析。
- 凭据明文写在本地文件，**不入库**；这是 dev keystore 的可接受做法（见 §六.6）。
- 文件缺失时 `build.gradle.kts` 不报错，release 不签名（见 §B）。

#### A.3 .gitignore 补充

`.gitignore` 当前第 12–15 行：

```gitignore
# App module build outputs
/app/build
/app/.cxx
/app/release
```

在第 15 行 `/app/release` 后追加：

```gitignore
# App module build outputs
/app/build
/app/.cxx
/app/release

# Release signing (dev keystore + credentials, never commit)
/keystore
/keystore.properties
```

- `/keystore`：整个 keystore 目录（含 `release.jks`）不入库。
- `/keystore.properties`：凭据文件不入库。

### B. signingConfigs.release（build.gradle.kts 改动）

#### B.1 改前 / 改后

`app/build.gradle.kts` 第 9–25 行当前：

```kotlin
android {
    namespace = "com.shijiben"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shijiben"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    // ...
}
```

改后（在 `android { }` 块顶部、`defaultConfig` 之前插 `signingConfigs`，并把 `buildTypes.release` 改造为 §C 的形态）：

```kotlin
import java.util.Properties

android {
    namespace = "com.shijiben"
    compileSdk = 34

    // 读取本地 keystore.properties；文件缺失则 release 不签名
    // （保证 debug 构建与全新 clone 不破；release APK 仍可构建但未签名）
    val keystoreProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            // 仅当 keystore.properties 存在且四字段齐全时才赋值；
            // 否则四字段保持 null，signingConfig 引用也不会让构建失败
            if (keystoreProperties.containsKey("storeFile")) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.shijiben"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    // ...
}
```

#### B.2 设计要点

- **回退逻辑**：`keystore.properties` 不存在时，`keystoreProperties` 是空 Properties，`signingConfigs.release` 的四字段保持 null；`buildTypes.release.signingConfig` 仍引用该 config，但四字段为 null 时 AGP 不会尝试签名，**release APK 可构建但未签名**。这保证：
  1. debug 构建（`./gradlew assembleDebug`）完全不读 keystore，永远不破。
  2. 全新 clone 无 `keystore.properties` 时，`./gradlew :app:assembleRelease` 仍能跑通并产出未签名 APK，第四道门不破。
  3. 只有开发者本地放了 `keystore.properties` + `keystore/release.jks`，才会产出可安装的已签名 release APK。
- **import 位置**：`import java.util.Properties` 加在文件顶部 plugins 块之前（第 1 行前），与 Kotlin 文件惯例一致。
- **`rootProject.file(...)`**：从项目根解析路径，与 `keystore.properties` 里 `storeFile=keystore/release.jks` 相对路径配合。
- **`getDefaultProguardFile("proguard-android-optimize.txt")`**：AGP 内置优化版默认规则（比 `proguard-android.txt` 多 `-optimizations`），适合 release；与项目自带 `proguard-rules.pro` 叠加生效。

### C. buildTypes.release 改造

§B.1 已含改后形态，逐字段说明：

| 字段 | 改前 | 改后 | 理由 |
|------|------|------|------|
| `isMinifyEnabled` | `false`（第 23 行） | `true` | 开 R8 代码缩减 + 混淆 + 优化 |
| `isShrinkResources` | 不存在 | `true` | 开资源缩减（与 `isMinifyEnabled=true` 联动，移除未引用资源） |
| `proguardFiles` | 不存在 | `getDefaultProguardFile("proguard-android-optimize.txt")` + `"proguard-rules.pro"` | AGP 默认优化规则 + 项目自定义规则 |
| `signingConfig` | 不存在 | `signingConfigs.getByName("release")` | 引用 §B 的 release 签名配置 |

注意：
- `isShrinkResources = true` 要求 `isMinifyEnabled = true`（AGP 强制约束），二者必须同开同关。
- 不开 `isDebuggable`：release 默认 false，符合发布预期。
- 不动 `defaultConfig`、`buildFeatures`、`compileOptions`、`kotlinOptions`、`testOptions`、`ksp`、`dependencies` 块。

### D. proguard-rules.pro 内容（新建）

新建 `app/proguard-rules.pro`，最小防御 keep 集：

```proguard
# ===================================================================
# 事记本 release ProGuard 规则
# 最小防御 keep 集——本 app 源码零反射，依赖库（Room / Hilt / Compose /
# Coroutines / lifecycle / navigation）均自带 consumer-rules.pro；
# 以下 keep 仅作双保险，防止 R8 在边界情况下裁剪生成类。
# ===================================================================

# --- Hilt ---
# Hilt 通过 KSP 生成 _HiltModules / _Factory / _GeneratedInjector 等类，
# 这些类被 R8 视为无引用会被裁剪。Hilt 自带 consumer rules 通常已覆盖，
# 此处显式 keep @Inject 构造的类与 @HiltViewModel 标注的 ViewModel 作双保险。
-keep class dagger.hilt.** { *; }
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep,allowobfuscation @javax.inject.Inject class * { *; }

# --- Room ---
# Room 的 DAO 接口由 KSP 生成实现类（*_Impl），Entity 类被反射式序列化到
# SQLite。Room 自带 consumer rules 已 keep @Entity / @Dao，此处对
# data.local.** 全包 keep 作双保险，防止 Entity 字段被 R8 重命名后
# 与 DB schema 不一致。
-keep class com.shijiben.data.local.** { *; }

# --- Kotlin Metadata ---
# Kotlin 编译器在 class 上写 @kotlin.Metadata 注解，部分库（含 Hilt KSP
# 生成的代码、反射式序列化库）依赖 Metadata 反读 Kotlin 信息。
# allowobfuscation 保留注解但允许外层类被混淆。
-keep,allowobfuscation @kotlin.Metadata class * { *; }

# --- 保留 BuildConfig（防止 R8 把 VERSION_NAME 等常量内联后裁剪类）---
# BuildConfig 由 AGP 生成，常量字段（VERSION_NAME / VERSION_CODE / DEBUG）
# 会被内联到调用处。keep 一下避免在反射场景下找不到类。
-keep class com.shijiben.BuildConfig { *; }
```

设计要点：
- **每条 keep 都带注释说明为何要 keep**：满足 spec 要求，也方便后续维护者评估是否可删。
- **`allowobfuscation`**：保留类/注解的同时允许 R8 重命名外层类（仅保留元信息），相比纯 `-keep` 体积更优。
- **不写 `-dontwarn` / `-dontnote`**：本项目无反射、无缺失依赖，无需抑制警告；若 release 构建出现 warning，应优先排查根因。
- **不复制依赖库的 consumer rules**：Room / Hilt / Compose 自带的 consumer-rules.pro 会被 AGP 自动合并，重复写没意义且增加维护负担；本文件只补"双保险"那部分。
- **不 keep Compose 相关**：Compose 编译器插件（`org.jetbrains.kotlin.plugin.compose`）已处理 Compose runtime 的 keep 需求，无需手动 keep。

### E. 验证门扩展

#### E.1 四道门顺序

| # | 命令 | 验证范围 | 是否本轮新增 |
|---|------|---------|------------|
| 1 | `./gradlew :app:compileDebugKotlin` | Kotlin 编译 + KSP（Hilt / Room 代码生成）+ BuildConfig 生成 | 否（迭代 5 已立） |
| 2 | `./gradlew :app:testDebugUnitTest --rerun-tasks` | 95 个单测全绿（Robolectric + Truth + coroutines-test） | 否 |
| 3 | `./gradlew assembleDebug` | debug APK 资源 + manifest + 打包 | 否 |
| 4 | **`./gradlew :app:assembleRelease`** | release APK：R8 混淆 + 资源缩减 + ProGuard 规则 + 签名配置 | **本轮新增** |

#### E.2 顺序理由

1. **compileDebugKotlin 最先**：编译失败立即停，不浪费后续任务时间。覆盖 KSP 代码生成（Hilt / Room）与 BuildConfig。
2. **testDebugUnitTest 第二**：单元测试依赖编译产物，编译过了才跑测试。`--rerun-tasks` 强制重跑避免缓存假绿。
3. **assembleDebug 第三**：debug 打包验证资源、manifest、依赖完整性；不依赖 release 配置，永远应该过。
4. **assembleRelease 最后**：release 通道是本轮新增能力，必须由独立门守护。前三门过不代表 release 过——R8 裁剪 / ProGuard 规则错误 / 签名配置问题只能在这道门暴露。

#### E.3 永久保留

第四道门不本轮过完即拆，而是永久加入验证门序列。理由：
- release 通道无 CI 守护就是质量债，任何后续改动都可能静默破坏 release 构建。
- `:app:assembleRelease` 在 CI 上无需真机、无需签名 keystore（见 §B.2 回退逻辑），任何环境都能跑。
- 与"全自动到失败为止"原则一致：四道门任一失败立即停。

### F. 文档对齐

#### F.1 AGENT.md 构建与安装章节（第 83–93 行）

当前：

```markdown
## 构建与安装

修改代码后，通过以下命令构建并安装到已连接的 ADB 设备：

```bash
# 构建 debug APK
./gradlew assembleDebug

# 安装到已连接的设备（需先 adb 连接）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
```

改后：

```markdown
## 构建与安装

修改代码后，通过以下命令构建并安装到已连接的 ADB 设备：

```bash
# 构建 debug APK
./gradlew assembleDebug

# 安装到已连接的设备（需先 adb 连接）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release 构建

Release 通道开启 R8 混淆 + 资源缩减，签名配置从本地 `keystore.properties` 读取。

```bash
# 构建 release APK（无 keystore.properties 时产出未签名 APK，构建不破）
./gradlew :app:assembleRelease
```

已签名 release APK 安装：

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

#### Keystore 配置（仅本地）

Release 签名凭据存于项目根下 `keystore.properties`（gitignored，不入库）：

```properties
storeFile=keystore/release.jks
storePassword=shijiben
keyAlias=release
keyPassword=shijiben
```

- `keystore/release.jks` 为自签名 dev keystore，**非生产发布密钥**。
- 全新 clone 无 `keystore.properties` 时 release APK 仍可构建（未签名），不阻塞 CI。
- 若需重新生成 keystore，参考 [docs/superpowers/specs/2026-06-28-release-build-config-design.md](docs/superpowers/specs/2026-06-28-release-build-config-design.md) §A.1。

### 验证门（四道全绿才算过）

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew assembleDebug
./gradlew :app:assembleRelease
```
```

#### F.2 AGENT.md Spec 索引（第 99–108 行）

在列表末尾（第 108 行后）补本轮 spec：

```markdown
- [2026-06-28-release-build-config-design.md](docs/superpowers/specs/2026-06-28-release-build-config-design.md) — Release 构建配置（签名 + 混淆 + 缩减 + ProGuard 规则）（本 spec）
```

#### F.3 AGENT.md 技术栈章节（第 10–19 行）

复核结论：技术栈已含 Kotlin / Compose / Room / Hilt / Coroutines / Navigation，无需补 R8 / ProGuard 条目（R8 是 AGP 内置，非独立依赖）。**不动。**

---

## 五、涉及文件清单

### 改
- `app/build.gradle.kts` — §B.1 / §C：加 `import java.util.Properties`、`signingConfigs.release` 块、`buildTypes.release` 改造
- `.gitignore` — §A.3：补 `/keystore` 与 `/keystore.properties`
- `AGENT.md` — §F.1 构建与安装章节 / §F.2 Spec 索引

### 新增
- `app/proguard-rules.pro` — §D：最小防御 keep 集（Hilt + Room + Kotlin Metadata + BuildConfig）
- `keystore/release.jks` — §A.1：keytool 生成（**gitignored，不入库**）
- `keystore.properties` — §A.2：凭据文件（**gitignored，不入库**）

### 复核无改动
- 根 `build.gradle.kts` — extra 区无需新增（本轮不引新依赖）
- `app/src/main/AndroidManifest.xml` — release 配置不动 manifest
- `app/src/main/java/com/shijiben/` 全部源码 — 零业务逻辑改动
- `app/src/test/` 全部测试 — 无新增单测，回归保护
- `app/src/main/res/values/themes.xml` / `splash_background.xml` — 与 release 无关
- `plans/README.md` — 本轮无 plan 条目
- `docs/2026-06-22-shijiben-design.md` — 与 release 无关

---

## 六、边界情况

### 6.1 keystore.properties 缺失（全新 clone）

场景：开发者全新 clone 项目，未放 `keystore.properties` 与 `keystore/release.jks`。

行为：
- `./gradlew assembleDebug`：完全不读 keystore，正常产出 debug APK。
- `./gradlew :app:assembleRelease`：`keystoreProperties` 是空 Properties，`signingConfigs.release` 四字段为 null，AGP 跳过签名步骤，**产出未签名 release APK** `app/build/outputs/apk/release/app-release.apk`（实际名为 `app-release-unsigned.apk` 或类似，AGP 行为）。
- 四道门全绿：debug 三道门不依赖 keystore；第四道门 release 构建只验证 R8 + 资源缩减 + ProGuard 规则，不依赖签名成功。

风险：未签名 release APK 无法 `adb install`。**这是预期行为**——开发者要测 release 安装，需自行生成 keystore + 配 keystore.properties（见 §A）。

### 6.2 R8 裁剪 Hilt 生成类

场景：R8 开启后，Hilt 通过 KSP 生成的 `_HiltModules` / `_Factory` / `_GeneratedInjector` 类被识别为无引用而被裁剪，运行时 Hilt 找不到依赖导致 `IllegalStateException`。

缓解：
- Hilt 2.52 自带 consumer-rules.pro，AGP 自动合并到 release R8 流程，正常情况下足够。
- §D 的 proguard-rules.pro 显式 keep `dagger.hilt.**` + `@HiltAndroidApp` + `@HiltViewModel` + `@Inject` 作双保险。
- **第四道门 `:app:assembleRelease` 捕获编译期 / R8 期问题**；运行时 Hilt 错误只能手动安装 release APK 启动验证（记 backlog，见 §七手动验证）。

### 6.3 Room schema export 在 release 下行为

场景：`app/build.gradle.kts` 第 48–50 行 ksp arg `room.schemaLocation = $projectDir/schemas`，Room 编译器在 KSP 阶段导出 schema JSON 到 `app/schemas/`。

release 下行为：
- KSP arg 对 release / debug 一视同仁，release 构建也会导出 schema JSON 到 `app/schemas/`。
- `app/schemas/` 已存在（迭代 5 已铺路 exportSchema=true 已工作），不新增 gitignored 条目。
- 风险：若 release 与 debug schema 不一致（不可能，schema 由 @Entity 定义，与 buildType 无关），会覆盖。**实际不会发生。**
- 无需在 proguard-rules.pro 额外 keep Room schema 导出逻辑——schema 导出是 KSP 期行为，与 R8 无关。

### 6.4 BuildConfig.VERSION_NAME 在 release 下可读

场景：迭代 5 §C.3 已让 `ExportViewModel` 与 `AboutScreen` 读 `BuildConfig.VERSION_NAME`（取自 `versionName = "1.0"`）。

release 下行为：
- AGP 为 release buildType 同样生成 `BuildConfig.java`，`VERSION_NAME = "1.0"` 是 `const val`，编译期常量内联到调用处。
- §D proguard-rules.pro 显式 `-keep class com.shijiben.BuildConfig { *; }` 作双保险，防止 R8 把 BuildConfig 类整体裁剪（虽不影响常量内联，但反射场景下找不到类）。
- 风险：导出 JSON 的 `appVersion` 字段在 release 下仍为 `"1.0"`，与 debug 一致。

### 6.5 Compose 编译器在 release 下行为

场景：Compose 编译器插件（`org.jetbrains.kotlin.plugin.compose` 2.0.21）在 release 下与 debug 行为一致，无需额外 keep。

要点：
- Compose runtime 的 keep 需求由 Compose 编译器插件 + androidx.compose 自带 consumer rules 处理，**无需手动 keep**。
- release 下 Compose 代码会被 R8 内联 + 混淆，但不影响功能。
- §D 不写任何 Compose 相关 keep。

### 6.6 keystore 密码泄露风险

场景：固定密码 `shijiben` 写在 `keystore.properties` 中，若文件泄露则 keystore 形同虚设。

评估：
- 本轮 keystore 是**自签名 dev keystore**，仅用于本地 release APK 安装测试，**非生产发布密钥**。
- 生产发布应走 Play App Signing 或独立密钥管理（不在本轮范围）。
- `.gitignore` 已补 `/keystore` 与 `/keystore.properties`，**绝不入库**。
- 密码泄露的最坏后果：他人能用此 keystore 签名假冒"事记本"的 APK。但本 app 是纯本地应用、无服务端校验、无应用商店上架，影响有限。
- **结论：可接受。**

### 6.7 R8 fullMode

场景：AGP 8 默认 R8 fullMode = true（自 AGP 8.0 起），相比 compat 模式更激进的优化与裁剪。

处理：
- 本轮不显式设 `android.enableR8.fullMode`，沿用 AGP 8.7.3 默认（fullMode = true）。
- §D 的 keep 集在 fullMode 下仍有效。
- 若 fullMode 引入额外 release-only 问题，由第四道门捕获。

---

## 七、测试清单

### 7.1 自动化（回归保护，无新增单测）

四道门全绿：

1. `./gradlew :app:compileDebugKotlin`
   - 验证 `import java.util.Properties` 不破坏编译
   - 验证 `signingConfigs.release` 块语法正确
   - 验证 `buildTypes.release` 改造后 KSP（Hilt / Room）仍正常生成代码
   - 验证 `BuildConfig` 仍生成（迭代 5 已铺路）

2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
   - 95 个单测全绿
   - 验证 release 配置改动不影响 debug 单测
   - 验证 BuildConfig.VERSION_NAME 在测试中仍可读（ExportViewModelTest 间接覆盖）

3. `./gradlew assembleDebug`
   - 验证 debug APK 资源 + manifest + 打包完整
   - 验证 signingConfigs 块对 debug 构建无副作用

4. **`./gradlew :app:assembleRelease`（新增）**
   - 验证 R8 混淆 + 资源缩减 + ProGuard 规则无冲突
   - 验证 proguard-rules.pro 语法正确
   - 验证 keystore.properties 缺失时 release 构建不破（CI 环境）
   - 验证 keystore.properties 存在时签名配置生效（本地环境，可选）
   - 验证 Room schema export 在 release 下行为一致（§6.3）

### 7.2 手动验证（需真机，记 backlog）

以下项需真机 + 本地 keystore，**不进入本轮验证门**，记入 backlog：

- **M1**：生成 keystore + 配 keystore.properties → `./gradlew :app:assembleRelease` → `adb install -r app/build/outputs/apk/release/app-release.apk` → 冷启动 App，确认：
  - 启动不崩溃（验证 §6.2 Hilt 在 release 下运行时正常）
  - splash 正常显示（迭代 5 已落地）
  - 时间轴首页正常加载（验证 Room 在 release 下运行时正常）
  - 进入设置页 → 关于，版本号显示 `版本 1.0`（验证 §6.4 BuildConfig.VERSION_NAME 在 release 下可读）
  - 进入设置页 → 数据导出，生成 JSON 文件，`appVersion` 字段为 `"1.0"`（验证 ExportViewModel 在 release 下运行时正常）
- **M2**：release APK 体积相比 debug APK 明显缩小（验证 R8 + 资源缩减生效）。
- **M3**：warm start / cold start 多次，无 release-only 崩溃。

### 7.3 不做的测试

- 不写真机自动化 UI 测试（Espresso / UiAutomator）——非本轮范围。
- 不写 ProGuard 规则单测——ProGuard 规则正确性由 assembleRelease 门 + 手动启动验证。
- 不写签名验证单测——签名是 AGP / Java 标准能力。

---

## 八、硬约束逐项核对清单

| # | 硬约束 | 本 spec 是否遵守 | 证据 |
|---|--------|----------------|------|
| 1 | **绝对不做任何联网功能** | ✓ | 本轮所有改动：keytool（JDK 本地工具）、R8 / ProGuard（AGP 内置本地任务）、keystore.properties（本地文件）、proguard-rules.pro（本地规则文件）。零联网。§三非目标已显式声明。 |
| 2 | **四道验证门全绿才算过**（compileDebugKotlin + testDebugUnitTest --rerun-tasks + assembleDebug + assembleRelease） | ✓ | §E.1 列出四道门及顺序；§七测试清单逐道核对。第四道门为本轮新增并永久保留。 |
| 3 | **Coding subagent 与 Test subagent 必须是不同 subagent** | ✓ | 本 spec 是 Design subagent 产出，仅落 spec 不改代码；Coding 与 Test 由 orchestrator 分别派单不同 subagent。spec §九实现顺序建议给 Coding；Test subagent 跑四道门。 |
| 4 | **全自动到失败为止** | ✓ | 四道门任一失败立即停；§E.3 第四道门永久保留以持续守护 release 通道。 |

---

## 九、需 orchestrator 拍板的决策点

**无。** 本轮 Discover 报告已明确无阻塞决策点，本 spec 沿用 Discover 推荐方案：

- Keystore 路径 `keystore/release.jks` + 凭据 `keystore.properties` + 固定密码 `shijiben`：Discover 已拍板，自签名 dev keystore 可接受。
- `signingConfigs.release` 文件缺失回退不签名：Discover 已拍板，保证 CI 与全新 clone 不破。
- `buildTypes.release` 开 `isMinifyEnabled = true` + `isShrinkResources = true`：Discover 已拍板，标准 release 配置。
- proguard-rules.pro 最小防御 keep 集（Hilt + Room + Kotlin Metadata）：Discover 已拍板，双保险策略。
- 第四道门 `:app:assembleRelease` 永久保留：Discover 已拍板，CI 可跑无需真机。

---

## 十、实现顺序建议（给 Coding subagent）

1. **A.3 .gitignore 补充先行**：在生成 keystore 前先补 `/keystore` 与 `/keystore.properties`，防止误入库。
2. **A.1 / A.2 keystore 生成**：跑 keytool 命令生成 `keystore/release.jks`，写 `keystore.properties`。**两文件不入库。**
3. **D proguard-rules.pro**：新建 `app/proguard-rules.pro`，写入 §D 的 keep 集。
4. **B / C build.gradle.kts 改造**：加 `import java.util.Properties` + `signingConfigs.release` 块 + `buildTypes.release` 改造。
5. **跑第一道门**：`./gradlew :app:compileDebugKotlin`——验证 Kotlin 编译 + KSP 不破。
6. **跑第二道门**：`./gradlew :app:testDebugUnitTest --rerun-tasks`——验证 95 单测全绿。
7. **跑第三道门**：`./gradlew assembleDebug`——验证 debug 打包不破。
8. **跑第四道门**：`./gradlew :app:assembleRelease`——验证 R8 + ProGuard + 签名配置不破。
9. **F 文档对齐**：改 AGENT.md 构建与安装章节 + Spec 索引。
10. **四道门最终验证**：四道门顺序重跑全绿，本轮收尾。

> 给 Test subagent：四道门必须按 §E.1 顺序执行，任一失败立即停并回报。第四道门首次跑可能因 R8 暴露问题，需观察是否是 proguard-rules.pro 缺 keep——若是，回到 Coding subagent 补 keep（不放宽规则，只补必要的 keep），再跑第四道门。
