pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lässt Gradle ein JDK 17 für die Kotlin-Toolchain automatisch herunterladen,
// falls lokal keines installiert ist (Standard in aktuellen Android-Studio-
// Vorlagen). Der Gradle-Daemon selbst läuft problemlos mit JDK 21.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HylSpeedReader"
include(":composeApp")
