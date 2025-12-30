plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.brogaming.trackmyauto"
    compileSdk = 36 // FIXED: Stable version for Android 15

    defaultConfig {
        applicationId = "com.brogaming.trackmyauto"
        minSdk = 24
        targetSdk = 36 // FIXED: Stable version
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // FIX for Hilt/Kotlin 2.1 Metadata error
    kapt("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")

    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Firebase (BOM)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation ("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In & Location
    implementation("com.google.android.gms:play-services-auth:21.4.0") // or newer
    implementation("com.google.android.gms:play-services-location:21.3.0") // or newer
    //    implementation("com.google.android.gms:play-services-location:21.3.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.0")


    // --- MAPLIBRE FIXED SECTION ---
    // Use stable versions that actually exist on Maven Central
    implementation("org.maplibre.gl:android-sdk:12.3.1")

    // FIXED: The correct name is 'android-plugin-annotation-v9' (removed '-sdk-')
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")    // ------------------------------

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Compose
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
}