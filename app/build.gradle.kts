plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

import java.util.Properties
import java.io.File

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun secret(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(prop)?.takeIf { it.isNotBlank() }

val releaseStorePath = secret("RELEASE_STORE_FILE", "storeFile")
val releaseStoreFile = releaseStorePath?.let { path ->
    val asIs = File(path)
    when {
        asIs.isAbsolute && asIs.exists() -> asIs
        rootProject.file(path).exists() -> rootProject.file(path)
        file(path).exists() -> file(path)
        else -> null
    }
}
val canSignRelease = releaseStoreFile != null &&
    !secret("RELEASE_STORE_PASSWORD", "storePassword").isNullOrBlank() &&
    !secret("RELEASE_KEY_ALIAS", "keyAlias").isNullOrBlank() &&
    !secret("RELEASE_KEY_PASSWORD", "keyPassword").isNullOrBlank()

android {
    namespace = "com.personal.timetracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.personal.timetracker"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2).coerceAtLeast(2)
        versionName = System.getenv("VERSION_NAME") ?: "1.1.0"
    }

    signingConfigs {
        create("release") {
            if (canSignRelease) {
                storeFile = releaseStoreFile
                storePassword = secret("RELEASE_STORE_PASSWORD", "storePassword")
                keyAlias = secret("RELEASE_KEY_ALIAS", "keyAlias")
                keyPassword = secret("RELEASE_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (canSignRelease) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        viewBinding = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.biometric:biometric:1.1.0")
}
