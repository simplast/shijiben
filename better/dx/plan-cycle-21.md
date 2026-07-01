# Plan: dx cycle 21 — F021

## Finding
- ID: F021
- 标题：依赖版本管理从 `rootProject.extra` 迁移到 Gradle version catalog（`libs.versions.toml`），获得 IDE 自动补全 + 类型安全访问
- evidence：`build.gradle.kts:9`（`extra["coreKtx"] = "1.13.1"` — 旧式字符串键版本管理，无 IDE 补全/类型安全）
- impact：M（开发者下次增删/升级依赖时立即感受到：IDE 补全、点击跳转、编译期检查拼写，而非记忆 `extra["key"]` 字符串键）
- effort：M

## 现状
- `build.gradle.kts`（root）用 `extra["<key>"] = "<version>"` 定义 14 个版本（生产 10 + 测试 4）
- `app/build.gradle.kts` 用 `rootProject.extra["<key>"]` 引用，共 21 处
- F011 已把测试依赖硬编码版本收敛到 `extra`，但 `extra` 本身仍是字符串键方案——无 IDE 补全、无类型安全、无点击跳转、拼写错误只能运行时发现
- Gradle 8.9 + AGP 8.7.3 完整支持 version catalog（`gradle/libs.versions.toml` 自动加载，Kotlin DSL 生成 `libs` 类型安全访问器）

## 修复方案
迁移到 Gradle version catalog：
1. 新建 `gradle/libs.versions.toml`，集中所有版本（`[versions]`）、库坐标（`[libraries]`）、插件（`[plugins]`）
2. `build.gradle.kts`（root）：删除 `extra` block，`plugins` block 改用 `alias(libs.plugins.xxx) apply false`
3. `app/build.gradle.kts`：`plugins` block 改用 `alias`，`dependencies` block 改用 `libs.xxx.yyy` 访问器
4. 所有版本号与迁移前完全一致——零行为变化，仅管理方式现代化

## 收益（开发者会感受到的）
- **IDE 自动补全**：输入 `libs.` 即列出所有依赖；输入 `libs.androidx.core.` 补全 `ktx`
- **类型安全**：访问器在 Kotlin DSL 编译期生成，拼写错误立即红线，而非 gradle 解析时报错
- **点击跳转**：从 `libs.xxx` 跳转到 toml 定义；从 build 脚本跳转到依赖声明
- **集中可视**：所有版本在一个标准 toml 文件，结构化（versions/libraries/plugins 分区），比扁平 `extra` 字符串键更易扫读
- **与现代 Android/Gradle 文档对齐**：官方文档与所有新项目模板默认使用 version catalog

## In-scope files
1. `gradle/libs.versions.toml`（新建——version catalog 标准位置，自动加载无需 settings 改动）
2. `build.gradle.kts`（root）— 删除 `extra` block（14 行），`plugins` block 5 处改 `alias`
3. `app/build.gradle.kts` — `plugins` block 5 处改 `alias`，`dependencies` block 21 处 `rootProject.extra` 改 `libs.xxx`
4. `AGENT.md` — executor 末步：`Recent changes` 加一行 F021 记录；如超 120 行按 FIFO 裁剪

## Steps

### Step 1 — 新建 `gradle/libs.versions.toml`
完整内容：
```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
coreKtx = "1.13.1"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.10.01"
navigationCompose = "2.8.5"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
coroutines = "1.8.1"
splashscreen = "1.0.1"
junit = "4.13.2"
robolectric = "4.13"
androidxTestCore = "1.6.1"
truth = "1.4.4"

[libraries]
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "splashscreen" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### Step 2 — `build.gradle.kts`（root）
替换整个文件内容为：
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```
（删除全部 `extra[...]` block——版本已移入 toml）

### Step 3 — `app/build.gradle.kts` plugins block
将：
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
```
改为：
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
```

### Step 4 — `app/build.gradle.kts` dependencies block
将 `dependencies` block 中 21 处 `rootProject.extra["..."]` 引用改为 `libs.xxx` 访问器。完整新 `dependencies` block：
```kotlin
dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
```

### Step 5 — 运行 gate 验证
```bash
./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease
```
全绿即过。已知 baseline 失败（HeatmapYearViewModelTest / TimelineViewModelTest 3 项）按 baseline 视为 PASS。

### Step 6 — 更新 AGENT.md（executor 末步）
在 `## Recent changes (better cycles)` 列表末尾追加一行：
```
- dx: 依赖版本管理迁移到 Gradle version catalog（libs.versions.toml）——root/app build 脚本删除 14 处 extra 定义 + 21 处 rootProject.extra 引用，改用类型安全 libs.xxx 访问器，IDE 自动补全 + 编译期拼写检查，零行为变化
```
若 AGENT.md 超 120 行，按 FIFO 从 `Recent changes` 顶部裁剪最旧条目。

## Acceptance criteria
- gate 全绿（COMPILE / TEST / ASSEMBLE / RELEASE，baseline 已知失败视为 PASS）
- `gradle/libs.versions.toml` 存在且包含全部 17 个版本 + 26 个库 + 5 个插件
- `build.gradle.kts`（root）不再含任何 `extra[`
- `app/build.gradle.kts` 不再含任何 `rootProject.extra`
- 所有依赖坐标与版本号与迁移前完全一致（仅访问方式改变）
- BOM 仍以 `platform()` 引用，`debugImplementation`/`testImplementation`/`ksp` 配置不变

## 零行为变化说明
- 所有 group:name:version 三元组完全不变 → 解析到的 artifact 完全相同
- 插件 id 与版本完全不变 → 构建行为完全相同
- `compose-bom` 仍以 `platform()` 引用 → BOM 管理的 Compose 库版本不变
- `ksp` arg（room.schemaLocation）不变 → Room schema 生成不变
- 构建产物、测试结果、APK 完全一致
- 迁移后 IDE 首次 sync 会重新生成 `libs` 访问器（自动）

## 风险与缓解
- **风险**：toml 中 alias 命名与生成访问器大小写不匹配（如 `core-ktx` → `libs.core.ktx`，`androidx-test-core` → `libs.androidx.test.core`）
  - **缓解**：Gradle 访问器生成规则为 `-` → `.`，camelCase key 保留；上面 toml 已按规则命名；gate 的 compileDebugKotlin 会立即捕获任何访问器拼写错误
- **风险**：遗漏某处 `rootProject.extra` 引用未迁移
  - **缓解**：grep 已确认仅 2 个代码文件引用 extra（root + app build.gradle.kts），共 35 处；步骤 2/4 完整覆盖；compileDebugKotlin 会捕获遗漏
