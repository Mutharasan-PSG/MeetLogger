
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.MeetLogger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.MeetLogger"
        minSdk = 27
        targetSdk = 34
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
        viewBinding = true
        dataBinding = true
    }
}
dependencies {

// https://mvnrepository.com/artifact/io.github.webrtc-sdk/android
    implementation("io.github.webrtc-sdk:android:125.6422.06.1")



    implementation(libs.firebase.auth.ktx)
    implementation ("com.squareup.picasso:picasso:2.71828")

    implementation ("com.google.code.gson:gson:2.10.1")

        // Import the BoM for the Firebase platform
    implementation (platform("com.google.firebase:firebase-bom:33.7.0"))

    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation ("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation ("androidx.navigation:navigation-ui-ktx:2.8.5")

    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    implementation ("com.google.firebase:firebase-firestore:25.1.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}