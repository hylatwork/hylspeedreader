import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    // `expect object Platform` is an expect/actual *class*; opt in explicitly so
    // the build stays warning-clean under Kotlin 2.0.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("com.russhwolf:multiplatform-settings:1.1.1")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.core:core-ktx:1.15.0")
        }
        val androidUnitTest by getting
        androidUnitTest.dependencies {
            implementation("junit:junit:4.13.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
        val androidInstrumentedTest by getting
        androidInstrumentedTest.dependencies {
            implementation("androidx.compose.ui:ui-test-junit4:1.7.5")
            implementation("androidx.compose.ui:ui-test-manifest:1.7.5")
            implementation("androidx.test.ext:junit:1.3.0")
            implementation("androidx.test:runner:1.7.0")
        }
    }

    jvmToolchain(17)
}

android {
    namespace = "eu.hylatwork.hylspeedreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "eu.hylatwork.hylspeedreader"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    packageOfResClass = "eu.hylatwork.hylspeedreader.resources"
}

// Copies the assembled release APK into the repository's sys-src/dist drop-point.
// `assembleRelease` is finalized by this task, so any release build refreshes it.
val copyReleaseApkToDist = tasks.register<Copy>("copyReleaseApkToDist") {
    description = "Copies the built release APK into sys-src/dist."
    group = "build"
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
    }
    into(rootProject.layout.projectDirectory.dir("dist"))
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(copyReleaseApkToDist)
}
