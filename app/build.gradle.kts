plugins {
    id("com.android.application")
}

android {
    namespace = "com.rubcut.couchyappsmenu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rubcut.couchyappsmenu"
        minSdk = 21
        targetSdk = 34
        // The release workflow supplies a monotonically increasing run number.
        versionCode = (System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1)
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
    }

    val signingStore = System.getenv("ANDROID_KEYSTORE_FILE")
    val signingStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val signingAlias = System.getenv("ANDROID_KEY_ALIAS")
    val signingKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        signingStore,
        signingStorePassword,
        signingAlias,
        signingKeyPassword,
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        // The keystore lives only in GitHub Actions' temporary directory. It is
        // never committed and is injected through repository secrets.
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingStore!!)
                storePassword = signingStorePassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
