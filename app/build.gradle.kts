import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.jellyfin.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.jellyfin.mobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose & Navigation
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.kotlinx.collections.immutable)

    // Media3 (ExoPlayer, Session, UI)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Dependency Injection (Lightweight Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Image Loading
    implementation(libs.coil.compose)

    // Networking & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// أتمتة أرشفة إصدارات الإنتاج المحلية
val releaseApkFile = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
val releasesTargetDir = rootProject.layout.projectDirectory.dir("releases").asFile
val releaseVersionName = android.defaultConfig.versionName ?: "1.0.0"
val releaseVersionCode = android.defaultConfig.versionCode ?: 1

tasks.register("archiveReleaseApk") {
    group = "distribution"
    description = "Archives the release APK to the releases directory with standard naming."

    val srcApkFile = releaseApkFile
    val targetDir = releasesTargetDir
    val vName = releaseVersionName
    val vCode = releaseVersionCode

    doLast {
        val srcApk = srcApkFile.get().asFile
        if (!srcApk.exists()) {
            logger.warn("Release APK not found at ${srcApk.absolutePath}, skipping archive.")
            return@doLast
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val targetFileName = "mdd_tv_v${vName}_b${vCode}_release_${dateStr}.apk"
        val targetFile = File(targetDir, targetFileName)

        srcApk.copyTo(targetFile, overwrite = true)

        logger.lifecycle("Release APK successfully archived to: ${targetFile.absolutePath}")
    }
}

afterEvaluate {
    tasks.findByName("assembleRelease")?.finalizedBy("archiveReleaseApk")
}

