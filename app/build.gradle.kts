plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.secrets)
}

android {
    namespace = "com.alex.monitorsanatate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alex.monitorsanatate"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SUPABASE_URL", "\"\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Fisierele .ptl (PyTorch Mobile) nu trebuie comprimate
    androidResources {
        noCompress += "ptl"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Activity
    implementation(libs.activity.compose)

    // Core
    implementation(libs.core.ktx)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // OkHttp (WebSocket)
    implementation(libs.okhttp)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // DataStore
    implementation(libs.datastore.preferences)

    // Gson
    implementation(libs.gson)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.android)
    implementation(libs.ktor.core)

    // PyTorch Mobile Lite (inferenta model CNN_ECG)
    // IMPORTANT: versiunea trebuie sa corespunda cu versiunea PyTorch folosita la conversie!
    // Ruleaza: python -c "import torch; print(torch.__version__)" si potriveste versiunea.
    // Versiuni disponibile: 2.1.0, 2.2.0, 2.3.0 — alege versiunea MAJORA a PyTorch-ului tau.
    val pytorchVersion = "2.1.0"
    implementation("org.pytorch:pytorch_android_lite:$pytorchVersion")
    implementation("org.pytorch:pytorch_android_torchvision_lite:$pytorchVersion")

    // Coil — incarcare si afisare imagini in Compose
    implementation("io.coil-kt:coil-compose:2.6.0")
}
