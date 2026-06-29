# 事记本 迭代 7 设计：CI/构建卫生清理轮（修 fallback bug + 清 deprecated Icons）

> 日期：2026-06-28
> 范围：本轮两件事——(A) 修 `assembleRelease` 在 `keystore.properties` 缺失场景下的 fallback bug；(B) 清 5 文件 7 处 deprecated `Icons.Filled.KeyboardArrowLeft/Right`，统一替换为 `Icons.AutoMirrored.Filled.*` 变体。
> 前置：迭代 1–6 已落地，当前质量评分 92/100（迭代 6 结束，✅ 技术性跨越 $100 线），95 个单测全绿。
> 验证门（四道全绿才算过）：
> 1. `./gradlew :app:compileDebugKotlin`
> 2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
> 3. `./gradlew assembleDebug`
> 4. `./gradlew :app:assembleRelease`（含 fallback 验证：临时 mv keystore.properties 跑一次 + 恢复后跑一次）

---

## 一、问题

### 1.1 fallback bug（assembleRelease 在缺 keystore 时 fail）

`app/build.gradle.kts` 第 24–35 行的 `signingConfigs.release` 块**已含**守卫式赋值——仅当 `keystoreProperties.containsKey("storeFile")` 为真时才给 `storeFile/storePassword/keyAlias/keyPassword` 四字段赋值，否则四字段保持 null：

```kotlin
// app/build.gradle.kts 第 24–35 行（现状，已含守卫）
signingConfigs {
    create("release") {
        if (keystoreProperties.containsKey("storeFile")) {
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
}
```

但第 53 行的 `buildTypes.release` 块**无条件**引用了这个 release signingConfig：

```kotlin
// app/build.gradle.kts 第 45–55 行（现状）
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")   // ← 第 53 行，无条件引用
    }
}
```

**根因**：AGP 8.7.3 在 `packageRelease` task 执行时，见到 `buildTypes.release.signingConfig` 被显式赋值，就会尝试用该 signingConfig 签名；此时若 `storeFile` 等四字段为 null（即 `keystore.properties` 缺失），AGP 直接 fail，而不是回落到"产出 unsigned APK"。即第 53 行的**无条件引用**是 fail 的直接原因，第 28 行的字段守卫只能让字段保持 null，无法阻止 AGP 尝试签名。

**实际影响**：
- 全新 clone（无 `keystore.properties`）跑 `./gradlew :app:assembleRelease` → fail。
- CI 环境（无 keystore）跑 `assembleRelease` → fail。
- 本机有 `keystore.properties` 时不暴露此 bug，故迭代 1–6 的四道门（含 `assembleRelease`）一直绿——但这是"幸运绿"，非"健壮绿"。

### 1.2 deprecated Icons（5 文件 7 处）

Compose Material Icons 在新版本（含本项目所用 Compose BOM 2024.10.01）已把方向性图标（`KeyboardArrowLeft/Right`、`ArrowBack` 等）迁到 `Icons.AutoMirrored.Filled.*` 命名空间，旧路径 `Icons.Filled.KeyboardArrowLeft/Right`（即 `Icons.Default.KeyboardArrowLeft/Right`）标 `@Deprecated`。

实扫结果（5 文件，import + 使用共 7 组，使用处共 7 个）：

| 文件 | import 行 | 使用行 |
|------|-----------|--------|
| `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt` | 18（Left） | 57 |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` | 20（Left）、21（Right） | 67、150、166 |
| `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` | 17（Left） | 55 |
| `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` | 20（Left） | 107 |
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` | 21（Left） | 84 |

替换为 `Icons.AutoMirrored.Filled.*` 后：LTR 下视觉零变化；RTL 下箭头自动镜像，更正确。minSdk 26 充分支持（AutoMirrored 变体在 Compose BOM 2024.10.01 已稳定）。

## 二、目标

- **A. 修 fallback bug（S）**：把 `app/build.gradle.kts` 第 53 行改为条件引用——仅当 `keystore.properties` 含 `storeFile` 时才赋 `signingConfig`，否则 release 走 AGP 默认行为（产出 unsigned APK，构建成功）。验证方法：临时 `mv keystore.properties keystore.properties.bak` 跑 `assembleRelease` 应成功产出 unsigned APK；恢复后再跑应成功产出已签名 APK。
- **B. 清 deprecated Icons（S）**：5 文件 7 处 `Icons.Filled.KeyboardArrowLeft/Right` → `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right`，import 路径同步迁移。LTR 视觉零变化，无需手测。

## 三、非目标

- **不做 UI 测试**：Icons 替换视觉零变化，不引入 UI 测试 / 截图测试。
- **不碰业务逻辑**：5 个 Screen 文件只改 import + 图标引用，不动 composable 结构、状态、导航、ViewModel。
- **不做新功能**：本轮是 CI/构建卫生清理，零新功能。
- **不联网**：不引入任何网络依赖，不改网络相关代码（本来就没有）。
- **不动 keystore 本身**：不修改 `keystore.properties` 内容、不换签名密钥、不动 `signingConfigs.release` 块的字段赋值逻辑（第 24–35 行保持原样）。
- **不动 buildTypes.release 的其他字段**：只改第 53 行 `signingConfig` 那一行，`isMinifyEnabled/isShrinkResources/proguardFiles` 不动。
- **不统一其他 deprecated 调用**：本轮只清 Discover 报告点名的 5 文件 7 处 Icons，不顺带扫全仓其他 deprecated（避免范围蔓延）。

## 四、设计

### A. 修 fallback bug

#### A.1 改动点：`app/build.gradle.kts` 第 53 行

**改前**（第 45–55 行）：
```kotlin
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
```

**改后**：
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        // 仅当 keystore.properties 存在且含 storeFile 时才挂签名配置；
        // 否则 release 走 AGP 默认行为（产出 unsigned APK，构建成功）
        if (keystoreProperties.containsKey("storeFile")) {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

**改动说明**：
- 仅把第 53 行那一行包进 `if (keystoreProperties.containsKey("storeFile")) { ... }`，其余字段不动。
- `keystoreProperties` 是第 17–22 行定义的顶层 `val`，在 `buildTypes` 块作用域内可见，可直接引用。
- 与第 28 行的守卫条件**完全一致**（都用 `keystoreProperties.containsKey("storeFile")`），语义对称：第 28 行守卫"字段赋值"，本行守卫"挂载 signingConfig"，两者同真假，不会出现"挂了 signingConfig 但字段是 null"或"赋了字段但没挂 signingConfig"的错配。
- AGP 行为：`signingConfig` 未赋值时，`packageRelease` 不尝试签名，产出 `app-release-unsigned.apk`，构建成功；赋值且四字段齐全时，产出已签名 `app-release.apk`。

#### A.2 验证方法（fallback 双场景）

> 由 Coding subagent 在实现后、Test subagent 在四道门第四道执行。两场景都过 = 修复确认。

**场景 1：keystore.properties 缺失**
```bash
mv keystore.properties keystore.properties.bak
./gradlew :app:assembleRelease
# 期望：BUILD SUCCESSFUL，产出 app/build/outputs/apk/release/app-release-unsigned.apk
mv keystore.properties.bak keystore.properties   # 别忘了恢复
```

**场景 2：keystore.properties 存在（恢复后）**
```bash
./gradlew :app:assembleRelease
# 期望：BUILD SUCCESSFUL，产出 app/build/outputs/apk/release/app-release.apk（已签名）
```

**关键约束**：场景 1 跑完**必须立即恢复** `keystore.properties`，再跑场景 2。两场景之间若忘恢复，本机后续 release 构建会持续产出 unsigned APK，污染后续迭代。

### B. 清 deprecated Icons

**统一替换规则**：
- 使用处：`Icons.Default.KeyboardArrowLeft` → `Icons.AutoMirrored.Filled.KeyboardArrowLeft`；`Icons.Default.KeyboardArrowRight` → `Icons.AutoMirrored.Filled.KeyboardArrowRight`
  - 注：`Icons.Default` 是 `Icons.Filled` 的别名，替换时统一用全限定 `Icons.AutoMirrored.Filled.*` 形式（与 Discover 报告一致），更清晰。
- import 处：`androidx.compose.material.icons.filled.KeyboardArrowLeft` → `androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft`；Right 同理。
- `import androidx.compose.material.icons.Icons` 这一行**不动**（`Icons` 顶层对象本身没变，只是多了 `AutoMirrored` 子对象）。

#### B.1 `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`

**import（第 18 行）**：
```kotlin
// 改前
import androidx.compose.material.icons.filled.KeyboardArrowLeft
// 改后
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
```

**使用（第 57 行，顶栏返回按钮）**：
```kotlin
// 改前
Icons.Default.KeyboardArrowLeft,
// 改后
Icons.AutoMirrored.Filled.KeyboardArrowLeft,
```

#### B.2 `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt`

**import（第 20–21 行）**：
```kotlin
// 改前
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
// 改后
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
```

**使用 1（第 67 行，顶栏返回按钮）**：
```kotlin
// 改前
Icons.Default.KeyboardArrowLeft,
// 改后
Icons.AutoMirrored.Filled.KeyboardArrowLeft,
```

**使用 2（第 150 行，MonthSwitcher 上一月箭头，传给 PixelArrowBox 的 arrow 参数）**：
```kotlin
// 改前
arrow = Icons.Default.KeyboardArrowLeft,
// 改后
arrow = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
```

**使用 3（第 166 行，MonthSwitcher 下一月箭头）**：
```kotlin
// 改前
arrow = Icons.Default.KeyboardArrowRight,
// 改后
Icons.AutoMirrored.Filled.KeyboardArrowRight,
```

> 注：`PixelArrowBox` 的 `arrow` 参数类型是 `androidx.compose.ui.graphics.vector.ImageVector`（第 195 行），AutoMirrored 变体同样是 `ImageVector`，类型兼容，无需改 `PixelArrowBox` 签名。

#### B.3 `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt`

**import（第 17 行）**：
```kotlin
// 改前
import androidx.compose.material.icons.filled.KeyboardArrowLeft
// 改后
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
```

**使用（第 55 行，顶栏返回按钮）**：
```kotlin
// 改前
Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
// 改后
Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "返回")
```

#### B.4 `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`

**import（第 20 行）**：
```kotlin
// 改前
import androidx.compose.material.icons.filled.KeyboardArrowLeft
// 改后
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
```

**使用（第 107 行，顶栏返回按钮）**：
```kotlin
// 改前
Icons.Default.KeyboardArrowLeft,
// 改后
Icons.AutoMirrored.Filled.KeyboardArrowLeft,
```

#### B.5 `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt`

**import（第 21 行）**：
```kotlin
// 改前
import androidx.compose.material.icons.filled.KeyboardArrowLeft
// 改后
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
```

**使用（第 84 行，顶栏返回按钮）**：
```kotlin
// 改前
Icons.Default.KeyboardArrowLeft,
// 改后
Icons.AutoMirrored.Filled.KeyboardArrowLeft,
```

## 五、涉及文件清单

### 改（6 文件）
- `app/build.gradle.kts` — §A.1 第 53 行 `signingConfig` 改为条件引用（包进 `if (keystoreProperties.containsKey("storeFile"))`）
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt` — §B.1 import 第 18 行 + 使用第 57 行
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` — §B.2 import 第 20–21 行 + 使用第 67、150、166 行
- `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` — §B.3 import 第 17 行 + 使用第 55 行
- `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` — §B.4 import 第 20 行 + 使用第 107 行
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` — §B.5 import 第 21 行 + 使用第 84 行

### 新增
- 无

### 复核无改动
- `keystore.properties` — 不动内容（仅验证时临时 mv + 恢复）
- `signingConfigs.release` 块（第 24–35 行）— 不动，字段守卫逻辑保留
- `buildTypes.release` 其他字段（`isMinifyEnabled/isShrinkResources/proguardFiles`）— 不动

## 六、边界情况

1. **keystore.properties 缺失时 `assembleRelease` 行为**：改后第 53 行 `if` 不成立，`signingConfig` 不赋值，AGP `packageRelease` 不尝试签名，产出 `app-release-unsigned.apk`，**BUILD SUCCESSFUL**。这是修复的核心目标场景（全新 clone / CI）。
2. **keystore.properties 存在时 `assembleRelease` 行为**：改后第 53 行 `if` 成立，`signingConfig = signingConfigs.getByName("release")`，第 28–32 行四字段已赋值，AGP 正常签名，产出已签名 `app-release.apk`，**BUILD SUCCESSFUL**。本机迭代 1–6 一直走的就是这个路径，改后行为不变。
3. **AutoMirrored Icons 在 minSdk 26 的可用性**：`Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right` 在 Compose Material Icons 1.6.x 起稳定，本项目 Compose BOM 2024.10.01（对应 material-icons-core/material-icons-extended 1.7.x）已含。AutoMirrored 是纯 Kotlin 侧的命名空间重组，无新 native 依赖，minSdk 26 完全兼容。
4. **AutoMirrored Icons 在 LTR/RTL 下的视觉行为**：LTR 下 `KeyboardArrowLeft` 指左、`KeyboardArrowRight` 指右，与旧 `Filled` 变体像素级一致（同一 vector 资源）；RTL 下 AutoMirrored 变体自动镜像（Left 变体在 RTL 下指右），更符合 RTL 用户预期。本项目当前未做 RTL 本地化（全中文 UI），但迁移到 AutoMirrored 是"对未来 RTL 友好"的正确姿势，且零成本。
5. **fallback 修复不影响本机已签名 release APK 产出**：本机有 `keystore.properties`（含 `storeFile`），第 53 行 `if` 始终成立，行为与改前完全一致。即"本机有 keystore"这一常见开发场景零回归。
6. **第 28 行字段守卫与第 53 行挂载守卫的对称性**：两处都用 `keystoreProperties.containsKey("storeFile")`，真假必同——不会出现"挂了 signingConfig 但四字段 null"（会 fail）或"赋了四字段但没挂 signingConfig"（release 产出 unsigned，签名配置白赋值）的错配。这是设计上的关键不变量，Coding 实现时务必保持两处条件字面一致。
7. **`mv keystore.properties` 期间的开发中断风险**：fallback 验证场景 1 跑完到恢复之间，若开发者在同一终端跑别的 release 构建，会产出 unsigned APK。缓解：验证脚本明确"mv → 跑 → 立即恢复"三步连续执行，不中间停留。

## 七、测试清单

### 7.1 自动化（回归保护，无新增单测）
- 现有 95 个单测在四道门下全绿：
  - `./gradlew :app:compileDebugKotlin`（含 5 文件 Icons import 迁移后的编译；含 build.gradle.kts 改动后的配置评估）
  - `./gradlew :app:testDebugUnitTest --rerun-tasks`（95 测试全绿；Icons 改动不触及测试代码，无回归）
  - `./gradlew assembleDebug`（debug 构建不涉及 signingConfig，不受 fallback 修复影响；Icons 改动在 debug APK 里同样生效）
  - `./gradlew :app:assembleRelease`（本机有 keystore，走场景 2 已签名产出）

### 7.2 fallback 验证（新增，针对目标 A）
> 由 Test subagent 在四道门第四道前后执行（或 Coding 实现后自验，Test 复验）。

- **场景 1（缺 keystore）**：
  1. `mv keystore.properties keystore.properties.bak`
  2. `./gradlew :app:assembleRelease` → 期望 BUILD SUCCESSFUL，产出 `app-release-unsigned.apk`
  3. `mv keystore.properties.bak keystore.properties`（立即恢复）
- **场景 2（有 keystore，恢复后）**：
  1. `./gradlew :app:assembleRelease` → 期望 BUILD SUCCESSFUL，产出已签名 `app-release.apk`
- 两场景都过 = fallback 修复确认。

### 7.3 手动验证
- **无**。Icons 替换 LTR 视觉零变化（同一 vector 资源），无需手测；fallback 修复是构建期行为，由 7.2 的命令行验证覆盖，无需手测。

## 八、硬约束逐项核对清单

| 硬约束 | 核对 |
|--------|------|
| **绝对不做任何联网功能** | ✅ 本轮改动：build.gradle.kts signingConfig 守卫（纯本地构建配置）+ 5 文件 Icons import/引用迁移（纯本地 UI 资源）。零网络依赖，零网络代码。 |
| **验证门四道全绿** | ✅ ① compileDebugKotlin ② testDebugUnitTest --rerun-tasks ③ assembleDebug ④ assembleRelease（含 fallback 双场景）。spec §七已列清单。 |
| **Coding subagent 与 Test subagent 必须是不同 subagent** | ✅ 由 orchestrator 在派发时保证，spec 不越权。 |
| **全自动到失败为止** | ✅ 本轮无任何需人工介入的步骤（fallback 验证的 mv/恢复也是脚本化命令）。 |

## 九、风险与回退

| 风险 | 影响 | 缓解 / 回退 |
|------|------|------------|
| 第 53 行 `if` 条件与第 28 行不一致（拼写错 / 用错 key） | signingConfig 挂载与字段赋值错配，release fail 或白赋值 | spec §六.6 已明确两处必须字面一致用 `keystoreProperties.containsKey("storeFile")`。compileDebugKotlin 不暴露此问题，靠 assembleRelease 双场景验证（§七.2）暴露。回退：把第 53 行改回无条件引用即可。 |
| AutoMirrored 变体在当前 Compose BOM 不可用（假设） | compileDebugKotlin fail | Compose BOM 2024.10.01 已含 AutoMirrored（1.7.x 稳定），minSdk 26 兼容。第一道门 compileDebugKotlin 即可暴露。回退：5 文件改回 `Icons.Filled.*`。 |
| fallback 验证场景 1 跑完忘恢复 keystore.properties | 本机后续 release 持续产出 unsigned APK | spec §A.2、§六.7 已强调"立即恢复"。Test subagent 执行时三步连续脚本化。 |
| `assembleRelease` 在 minify+shrink 下因 Icons 迁移触发 proguard 规则变化（极低概率） | assembleRelease fail | Icons 是 Compose 内置 vector，proguard 规则不针对单个 icon。第四道门暴露。回退：5 文件 revert。 |

整体回退策略：A（build.gradle.kts）与 B（5 Icons 文件）完全独立，可分别 revert。若 A 出问题，单独回第 53 行；若 B 出问题，单独回 5 文件。两改动互不依赖。

## 十、附录：需 orchestrator 拍板的点

**无。** 本轮所有决策均沿用 Discover 报告推荐：
- fallback 修复采用"第 53 行条件引用"方案（Discover 报告方向 a），不采用"删 signingConfigs 块"或"换 signingConfig 引用方式"等其他方向。
- Icons 统一替换为 `Icons.AutoMirrored.Filled.*` 全限定形式（与 Discover 报告一致），不保留 `Icons.Default.*` 别名写法。
- 不新增单测（fallback 验证是构建期命令行验证，Icons 视觉零变化）。
- 不顺带扫全仓其他 deprecated（范围严格控制在本轮 Discover 点名的 5 文件 7 处）。

---

## 十一、实现顺序建议（给 Coding subagent）

1. **B 先行（Icons，纯文本替换，零风险）**：5 文件按 §B.1–B.5 逐个改 import + 使用处 → 跑 `./gradlew :app:compileDebugKotlin` 验证编译过（AutoMirrored 变体可用性在此暴露）。
2. **A（fallback，单行改动）**：改 `app/build.gradle.kts` 第 53 行为条件引用 → 跑 `./gradlew :app:compileDebugKotlin` 验证 Gradle 配置评估过。
3. **四道门最终验证**：
   - ① `./gradlew :app:compileDebugKotlin`
   - ② `./gradlew :app:testDebugUnitTest --rerun-tasks`（95 测试全绿）
   - ③ `./gradlew assembleDebug`
   - ④ `./gradlew :app:assembleRelease`（本机有 keystore，走已签名路径）
4. **fallback 双场景验证**（Test subagent 复验）：
   - 场景 1：`mv keystore.properties keystore.properties.bak` → `./gradlew :app:assembleRelease` → 期望 unsigned APK + BUILD SUCCESSFUL → `mv keystore.properties.bak keystore.properties` 立即恢复
   - 场景 2：`./gradlew :app:assembleRelease` → 期望已签名 APK + BUILD SUCCESSFUL
