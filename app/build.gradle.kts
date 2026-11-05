plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.benimtarayici.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.benimtarayici.app"
        minSdk = 26
        targetSdk = 34

        // Her GitHub derlemesinde sürüm kodu kendiliğinden artar.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0.0"

        // APK boyutunu küçük tutmak için sadece yaygın telefon işlemcileri.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // İmza bilgileri koda yazılmaz, GitHub gizli değerlerinden gelir.
    signingConfigs {
        create("release") {
            val anahtarYolu = System.getenv("KEYSTORE_PATH")
            if (anahtarYolu != null) {
                storeFile = file(anahtarYolu)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Küçültme kapalı: daha büyük ama hata riski düşük.
            isMinifyEnabled = false
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.wireguard.android:tunnel:1.0.20230706")
}
