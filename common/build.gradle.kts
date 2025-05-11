plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("com.google.firebase:firebase-firestore:24.10.0")  // For Firestore annotations/compatibility
    implementation("com.google.firebase:firebase-storage:20.3.0")    // For storage references if needed

    // For JSON serialization/deserialization if used with Firebase
    implementation("com.google.code.gson:gson:2.10.1")
}