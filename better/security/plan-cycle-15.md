# Plan: security cycle 15 — F015

## Finding (F015)
`AndroidManifest.xml` 设置 `android:allowBackup="true"`，违反 App 自己在 `AboutScreen` 公开的隐私政策承诺："数据不离开本设备，无任何上传、共享、分析行为" 与 "卸载即清除：卸载 App 后所有数据随之删除，无残留、无备份"。

### 信任边界
- Android OS（Auto Backup）+ 任何持有 USB 调试 / `adb backup` 权限的主体
- App 默认导出（无 `android:fullBackupContent` / `android:dataExtractionRules`）→ 走系统默认行为 = 全量备份

### 证据（symbol-level 引用）
- `app/src/main/AndroidManifest.xml:6`：`android:allowBackup="true"`
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt:122-128`：隐私政策明文承诺
  - L124：`"不分享、不分析：数据不离开本设备，无任何上传、共享、分析行为。"`
  - L126：`"卸载即清除：卸载 App 后所有数据随之删除，无残留、无备份。"`
  - L128：`"敏感数据说明：生日与假设寿命仅用于...存于本机 SharedPreferences，不出设备。"`
- `app/src/main/java/com/shijiben/data/local/AppDatabase.kt:22`：Room DB 文件 `shijiben.db`，包含全部 events（含可选 note）+ notes（个人随笔/日记内容）
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizPrefs.kt:39,46`：SharedPreferences 存储生日（PII）

### 影响（行为级，非推测）
1. **Auto Backup（Android 6+ 默认开启）**：系统在设备空闲+充电+连 WiFi 时（约每日一次）把 app 全部 internal 数据（Room DB + SharedPreferences）上传到用户 Google Drive（25 MB/app 配额，不计入用户存储配额）。
   - 直接违反隐私政策 L124「数据不离开本设备，无任何上传」
   - 用户未被告知也未经显式同意（系统层行为，App 内无任何提示）
2. **`adb backup -apk com.shijiben`**：在 allowBackup=true 时可把 app 全部数据拉到任何连接的 PC，无需 root。任何短暂接触设备 + USB 调试的人（修车店、共充电的同事、家庭成员）都可离线读取用户的时间记录/随笔/生日。
3. **卸载后云端副本仍可恢复**：违反隐私政策 L126「卸载即清除...无备份」。用户卸载后系统会在新设备/重装时弹出"恢复 app 数据？"提示，云端副本默认保留 1 年+。

### 现有先例（同 codebase 已有安全防御）
- `DataImportManager.parseEvents` 已对 `status` 做信任边界校验（F008）——同源 spec 一致：「导入来源不可信 → 拒绝」
- 此处镜像逻辑：「OS 默认会做不可信行为（备份/上传）→ 显式 opt-out」
- AGENTS.md 项目概述明文："**纯本地的时间记录应用**" + "核心理念：单纯记录生活"

## 修复
**单行改动**：`AndroidManifest.xml` 第 6 行 `android:allowBackup="true"` → `android:allowBackup="false"`。

### 改动点
```xml
<application
    android:name=".ShiJiBenApplication"
    android:allowBackup="false"   <!-- was: true -->
    ...>
```

### 副作用说明
- `allowBackup="false"` 同时关闭：
  - Auto Backup（Android 6+）→ 不再上传到 Google Drive
  - `adb backup` → 不再可被 USB 拉取
  - 在 Android 12+ 同时关闭 `dataExtractionRules` 默认行为（无需另写规则文件）
- **不影响**应用内自己的「数据导出」功能（`DataExportManager` 通过 SAF `CreateDocument` 写用户主动选定的 Uri，与系统备份机制完全无关）
- **不影响**应用内自己的「数据导入」功能
- **不影响** `adb` 之外的开发流程（`adb install -r`、logcat、`adb shell run-as` 调试 DB 均不依赖 allowBackup）

## 风险评估
- 影响面：1 行 XML，零 Kotlin 代码改动
- 不改 DB schema、不改 Repository、不改 ViewModel
- 不改 export/import 路径（用户主动导出/导入仍可用）
- 现有测试均不依赖 allowBackup=true → 不受影响
- 用户视角：从此再无 Google Drive 自动备份该 app 数据；用户仍可主动用「数据导出」生成 JSON 自行保管

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `HeatmapYearViewModelTest > yearGrid_updatesWhenRepoEmitsNewData`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`

## 不审计的目录
- `feature/heatmap/`、`feature/notes/`、`feature/recording/`、`feature/timeline/`、`feature/timeviz/`（除 TimeVizPrefs / TimeVizViewModel 外）
- `ui/theme/`、`ui/debug/DebugOverlay.kt`、`di/DispatchersModule.kt`、`navigation/AppNavHost.kt`
- `app/src/test/`（仅读 `DataImportManagerTest` 作为 F008 测试风格参照）
- `app/schemas/`（Room schema 历史 JSON）
- `docs/`（仅读 AGENTS.md / baseline.md / 既有 security findings / plan-cycle-8）
