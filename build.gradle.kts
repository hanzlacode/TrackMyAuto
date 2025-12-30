// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("org.jetbrains.kotlin.kapt") version "2.2.21" apply false // ✅ ensure this is here
    id("com.google.dagger.hilt.android") version "2.57.2" apply false

}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.4")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.6") // ✅ Required

    }
}

