# Findings: dx

## F011 — DONE (cycle 11)
- 文件：`app/build.gradle.kts` 的 `dependencies` block 中 `testImplementation` 块
- 符号：5 处硬编码版本的 testImplementation（junit / robolectric / androidx.test:core / kotlinx-coroutines-test / truth）
- 问题：生产依赖统一走 `rootProject.extra`，但 5 处测试依赖硬编码版本，其中 coroutines-test 与生产 coroutines 重复定义版本号——版本升级时容易漏改，coroutines 主/测试版本不同步风险
- 修复：提取 4 个新 extra key（junit / robolectric / androidxTestCore / truth），coroutines-test 复用既有 `coroutines` key，5 处 testImplementation 全改用 extra 引用
- evidence：`app/build.gradle.kts` dependencies.testImplementation 块（junit:4.13.2 / robolectric:4.13 / androidx.test:core:1.6.1 / kotlinx-coroutines-test:1.8.1 / truth:1.4.4）
- impact：S

## F021 — 依赖版本管理迁移到 Gradle version catalog（libs.versions.toml）
- status: DONE (cycle 21)
- evidence: build.gradle.kts:9
- impact: M
