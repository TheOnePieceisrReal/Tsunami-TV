import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime); implementation(compose.foundation)
            implementation(compose.material3); implementation(compose.materialIconsExtended)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("io.ktor:ktor-client-core:3.0.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
            implementation("app.cash.sqldelight:runtime:2.0.2")
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.media3:media3-exoplayer:1.4.1")
            implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
            implementation("androidx.media3:media3-ui:1.4.1")
            implementation("io.ktor:ktor-client-okhttp:3.0.0")
            implementation("app.cash.sqldelight:android-driver:2.0.2")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
                implementation("io.ktor:ktor-client-cio:3.0.0")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
                implementation("uk.co.caprica:vlcj:4.8.3")   // requires VLC 3.x installed (64-bit) on Windows
            }
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

android {
    namespace = "app.cinestream"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.cinestream"
        minSdk = 24; targetSdk = 35
        versionCode = 1; versionName = "1.0.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

sqldelight {
    databases { create("CineDb") { packageName.set("app.cinestream.db") } }
}

compose.desktop {
    application {
        mainClass = "app.cinestream.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "TsunamiTV"
            packageVersion = "1.0.0"
            windows { menuGroup = "Tsunami TV"; shortcut = true; dirChooser = true; iconFile.set(project.file("icon.ico")) }
        }
    }
}
