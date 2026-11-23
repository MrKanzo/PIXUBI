plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.pixubi.game"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.pixubi.game.stable"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.8.0"
    }
    // Release uses a permanent package ID. Debug is installed alongside it.
    signingConfigs {
        create("production") {
            storeFile = file(System.getenv("PIXUBI_KEYSTORE_PATH") ?: "pixubi-release.jks")
            storePassword = System.getenv("PIXUBI_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("PIXUBI_KEY_ALIAS") ?: "pixubi"
            keyPassword = System.getenv("PIXUBI_KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        getByName("debug") { applicationIdSuffix = ".mystery" }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("production")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    testImplementation("junit:junit:4.13.2")
}
