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
