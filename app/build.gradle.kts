plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing is read from environment variables (GitHub Actions secrets).
// Nothing secret is stored in the repo. If they are missing, the release build is unsigned.
val ksPath: String? = System.getenv("LUDOMAX_KEYSTORE_PATH")
val ksPassword: String? = System.getenv("LUDOMAX_KEYSTORE_PASSWORD")
val ksAlias: String? = System.getenv("LUDOMAX_KEY_ALIAS")
val ksKeyPassword: String? = System.getenv("LUDOMAX_KEY_PASSWORD")
val hasReleaseSigning = !ksPath.isNullOrBlank() && file(ksPath).exists() &&
    !ksPassword.isNullOrBlank() && !ksAlias.isNullOrBlank() && !ksKeyPassword.isNullOrBlank()

android {
    namespace = "com.ludomax.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ludomax.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
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
    androidResources {
        noCompress += listOf("ogg", "mp3", "wav")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
