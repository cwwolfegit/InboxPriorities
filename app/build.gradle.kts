plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Add this line for Kotlin Serialization:
    kotlin("plugin.serialization") version "1.9.22" // Match your Kotlin version if needed
}

android {
    namespace = "com.chuckwolfe.inboxpriorities"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.chuckwolfe.inboxpriorities"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // Jetpack DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Kotlinx Serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Google Credential Manager & Play Services Auth
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Google ID support for Credential Manager
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Gmail API Client
    implementation("com.google.api-client:google-api-client-android:2.6.0")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20240520-2.0.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.browser:browser:1.8.0")

    implementation("com.android.billingclient:billing-ktx:8.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0") // (or match whatever lifecycle version your project uses)

}