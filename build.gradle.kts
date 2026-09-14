import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

group = "com.alan.ximiearbuds"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Cross-platform JNA for Linux POSIX sockets and Windows Winsock2 Bluetooth
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("net.java.dev.jna:jna-platform:5.16.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(26)
}

compose.desktop {
    application {
        mainClass = "com.alan.ximiearbuds.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )
            packageName = "XimiEarbuds"
            packageVersion = "1.0.0"
            description = "Desktop Controller for Xiaomi, Redmi, and POCO Earbuds"
            copyright = "© 2026 Alan. Open Source."
            vendor = "XimiEarbuds"

            linux {
                iconFile.set(project.file("src/main/resources/icons/app_icon.png"))
                menuGroup = "Audio"
                appCategory = "AudioVideo"
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/app_icon.ico"))
                menuGroup = "XimiEarbuds"
            }
        }
    }
}
