// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false

    // Dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.1" apply false
}

