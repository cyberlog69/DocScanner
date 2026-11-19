import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(key: String, envVar: String): String? =
    (keystoreProperties.getProperty(key) ?: System.getenv(envVar))?.takeIf { it.isNotBlank() }

val releaseKeystoreFile = signingValue("storeFile", "DOCSCANNER_KEYSTORE")?.let { file(it) }
val releaseStorePassword = signingValue("storePassword", "DOCSCANNER_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "DOCSCANNER_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "DOCSCANNER_KEY_PASSWORD")
val hasReleaseSigning = releaseKeystoreFile?.exists() == true &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.example.docscanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cyberlog.docscanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "1.9.0"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            applicationId = "com.cyberlog.docscanner"
            buildConfigField("Boolean", "ENABLE_GITHUB_UPDATER", "false")
        }
        create("github") {
            dimension = "distribution"
            applicationId = "com.cyberlog.docscanner"
            buildConfigField("Boolean", "ENABLE_GITHUB_UPDATER", "true")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Unsigned when no keystore is configured; CI supplies one via secrets.
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            // Uses the standard debug keystore so debug builds never touch release keys.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES"
            )
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Kotlin Multiplatform Shared Core Module
    implementation(project(":shared"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android & Security
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)

    // Koin Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ML Kit (bundled multi-language OCR + Document Scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.devanagari)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.mlkit.document.scanner)

    // Coil
    implementation(libs.coil.compose)

    // Apache PDFBox for Android (Apache 2.0)
    implementation(libs.pdfbox)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Debug & Test
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
