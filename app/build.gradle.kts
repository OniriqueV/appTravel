import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.datn.apptravel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.datn.apptravel"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ===== Load local.properties =====
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }

        val tripServiceBaseUrl =
            localProperties.getProperty(
                "TRIP_SERVICE_BASE_URL",
                "http://192.168.1.147:8080/"
            )

        buildConfigField(
            "String",
            "API_KEY",
            "\"${localProperties.getProperty("API_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "GEOAPIFY_API_KEY",
            "\"${localProperties.getProperty("GEOAPIFY_API_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "GEOAPIFY_BASE_URL",
            "\"${localProperties.getProperty("GEOAPIFY_BASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "OSRM_BASE_URL",
            "\"${localProperties.getProperty("OSRM_BASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "NOMINATIM_BASE_URL",
            "\"${localProperties.getProperty("NOMINATIM_BASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "AUTH_BASE_URL",
            "\"${localProperties.getProperty("AUTH_BASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "TRIP_SERVICE_BASE_URL",
            "\"$tripServiceBaseUrl\""
        )

        buildConfigField(
            "String",
            "UPLOAD_BASE_URL",
            "\"${tripServiceBaseUrl}uploads/\""
        )

        buildConfigField(
            "String",
            "GOOGLE_API_BASE_URL",
            "\"${localProperties.getProperty("GOOGLE_API_BASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "GOOGLE_CUSTOM_SEARCH_API_KEY",
            "\"${localProperties.getProperty("GOOGLE_CUSTOM_SEARCH_API_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "GOOGLE_CUSTOM_SEARCH_CX",
            "\"${localProperties.getProperty("GOOGLE_CUSTOM_SEARCH_CX", "")}\""
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true   // ⭐ BẮT BUỘC
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

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {

    // ===== AndroidX =====
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // ===== Navigation =====
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ===== Lifecycle =====
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // ===== Coroutines =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // ===== Firebase (BOM) =====
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // ===== Retrofit / OkHttp =====
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.12")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.12")

    // ===== DI =====
    implementation("io.insert-koin:koin-android:3.5.3")

    // ===== Image =====
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // ===== DataStore =====
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ===== Maps =====
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // ===== Google Services =====
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(libs.androidx.activity)

    // ===== Testing =====
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
