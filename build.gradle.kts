plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

extra["coreKtx"] = "1.13.1"
extra["lifecycle"] = "2.8.7"
extra["activityCompose"] = "1.9.3"
extra["composeBom"] = "2024.10.01"
extra["navigationCompose"] = "2.8.5"
extra["hilt"] = "2.52"
extra["hiltNavigationCompose"] = "1.2.0"
extra["room"] = "2.6.1"
extra["coroutines"] = "1.8.1"
extra["splashscreen"] = "1.0.1"
