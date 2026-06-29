# Plan: dx cycle 11 — F011

## Finding
- ID: F011
- 标题：测试依赖版本硬编码，与生产依赖的 `rootProject.extra` 统一管理不一致
- evidence：`app/build.gradle.kts` 的 `dependencies` block 中 `testImplementation` 块
- impact：S（版本升级时容易漏改测试依赖，coroutines-test 已与生产 coroutines 重复定义）
- effort：S

## 现状
- 生产依赖统一走 `rootProject.extra["<key>"]`（如 `coreKtx`、`coroutines`、`room`）
- 测试依赖有 5 处硬编码版本：
  - `testImplementation("junit:junit:4.13.2")`
  - `testImplementation("org.robolectric:robolectric:4.13")`
  - `testImplementation("androidx.test:core:1.6.1")`
  - `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")` — 与生产 `extra["coroutines"] = "1.8.1"` 重复定义
  - `testImplementation("com.google.truth:truth:1.4.4")`
- `testImplementation("androidx.room:room-testing:${rootProject.extra["room"]}")` 已用 extra（一致）

## 修复方案
将 5 处硬编码版本提取到 `build.gradle.kts`（root）的 `extra`，保持版本管理一致性。

## In-scope files
1. `build.gradle.kts`（root）— 新增 4 个 extra key（junit / robolectric / androidxTestCore / truth）
2. `app/build.gradle.kts`— 5 处 testImplementation 改用 extra 引用（coroutines-test 复用既有 `coroutines` key）

## Steps
1. `build.gradle.kts`（root）：在 `extra` block 末尾追加：
   - `extra["junit"] = "4.13.2"`
   - `extra["robolectric"] = "4.13"`
   - `extra["androidxTestCore"] = "1.6.1"`
   - `extra["truth"] = "1.4.4"`
2. `app/build.gradle.kts`：替换 5 处 testImplementation：
   - `testImplementation("junit:junit:${rootProject.extra["junit"]}")`
   - `testImplementation("org.robolectric:robolectric:${rootProject.extra["robolectric"]}")`
   - `testImplementation("androidx.test:core:${rootProject.extra["androidxTestCore"]}")`
   - `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutines"]}")`
   - `testImplementation("com.google.truth:truth:${rootProject.extra["truth"]}")`
3. 运行 gate 验证零行为变化

## Acceptance criteria
- gate 全绿（COMPILE / TEST / ASSEMBLE）
- 5 处 testImplementation 全部使用 extra 引用
- 版本号与修复前完全一致（仅管理方式改变）

## 零行为变化说明
版本号完全不变，仅从硬编码改为 extra 引用。构建产物、测试结果、APK 完全一致。
