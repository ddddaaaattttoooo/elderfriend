import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ge.elderfriend.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "ge.elderfriend.launcher"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release signing: put keystore.properties next to this file (NOT committed)
    // or provide KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD.
    val ksProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun secret(name: String): String? =
        ksProps.getProperty(name) ?: System.getenv(name)

    signingConfigs {
        if (secret("KEYSTORE_FILE") != null) {
            create("release") {
                storeFile = file(secret("KEYSTORE_FILE")!!)
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Framework-only app: no AndroidX, no dependency downloads at build time.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
