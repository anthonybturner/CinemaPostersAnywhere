plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt") // For annotation processing
}

android {
    namespace = "com.anthonybturner.cinemapostersanywhere"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anthonybturner.cinemapostersanywhere"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://web-production-89879.up.railway.app/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://web-production-89879.up.railway.app/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
        compose = true
        dataBinding = true // Enables Data Binding
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging.resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.androidx.core)

    // Jetpack Compose and UI dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.activity.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Networking libraries
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // Image loading
    implementation(libs.glide)

    // Volley for networking
    implementation(libs.volley)

    // LocalBroadcastManager
    implementation(libs.androidx.localbroadcastmanager)

    // Material Components
    implementation(libs.material)

    // Testing libraries
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation (libs.socket.io.client.v210)

}
