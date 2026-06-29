import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

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
            // 仅当 keystore.properties 存在且含 storeFile 时才挂签名配置；
            // 否则 release 走 AGP 默认行为（产出 unsigned APK，构建成功）
            if (keystoreProperties.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${rootProject.extra["composeBom"]}")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:${rootProject.extra["coreKtx"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${rootProject.extra["lifecycle"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${rootProject.extra["lifecycle"]}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${rootProject.extra["lifecycle"]}")
    implementation("androidx.activity:activity-compose:${rootProject.extra["activityCompose"]}")
    implementation("androidx.core:core-splashscreen:${rootProject.extra["splashscreen"]}")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.navigation:navigation-compose:${rootProject.extra["navigationCompose"]}")

    implementation("com.google.dagger:hilt-android:${rootProject.extra["hilt"]}")
    ksp("com.google.dagger:hilt-compiler:${rootProject.extra["hilt"]}")
    implementation("androidx.hilt:hilt-navigation-compose:${rootProject.extra["hiltNavigationCompose"]}")

    implementation("androidx.room:room-runtime:${rootProject.extra["room"]}")
    implementation("androidx.room:room-ktx:${rootProject.extra["room"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room"]}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${rootProject.extra["coroutines"]}")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:${rootProject.extra["junit"]}")
    testImplementation("org.robolectric:robolectric:${rootProject.extra["robolectric"]}")
    testImplementation("androidx.test:core:${rootProject.extra["androidxTestCore"]}")
    testImplementation("androidx.room:room-testing:${rootProject.extra["room"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutines"]}")
    testImplementation("com.google.truth:truth:${rootProject.extra["truth"]}")
}
