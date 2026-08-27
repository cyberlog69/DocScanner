plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DocScannerKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.biometric)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.mlkit.text.recognition.devanagari)
            implementation(libs.mlkit.text.recognition.chinese)
            implementation(libs.mlkit.text.recognition.japanese)
            implementation(libs.mlkit.text.recognition.korean)
            implementation(libs.mlkit.document.scanner)
            implementation(libs.itext.core)
            implementation(libs.itext.bouncy.castle)
        }
        iosMain.dependencies {
            // Native iOS frameworks (Vision, VisionKit, LocalAuthentication, PDFKit) are provided by Kotlin/Native
        }
    }
}

android {
    namespace = "com.example.docscanner.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
