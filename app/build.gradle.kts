plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt") // kapt for annotation processing
}

android {
    namespace = "com.anthonybturner.cinemapostersanywhere"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anthonybturner.cinemapostersanywhere"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)

    // Jetpack Compose and Room dependencies
    val room_version = "2.5.0"
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // For ViewModel with Coroutines support
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Gson for JSON parsing
    implementation(libs.gson)

    // Retrofit for networking and Gson integration
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson.converter)

    // Glide for image loading using KSP
    implementation(libs.glide)

    // Socket.IO for real-time communication
    implementation(libs.socket.io.client.v201)

    // OKHttp for HTTP networking
    implementation(libs.okhttp)

    // AndroidX Libraries
    implementation(libs.androidx.constraintlayout)
    implementation(libs.volley)

    // LocalBroadcastManager for broadcasting intents
    implementation(libs.androidx.localbroadcastmanager)

    // Testing libraries
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation (libs.androidx.drawerlayout)
    implementation (libs.material) // For Navigation Drawer

    implementation (libs.androidx.recyclerview.v130)


}

