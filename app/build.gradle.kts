plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.coconutautoplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.coconutautoplayer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1-prototype"
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
