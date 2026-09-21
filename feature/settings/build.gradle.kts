plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.expensevault.feature.settings"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    
    buildFeatures {
        compose = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":platform:notification"))
    implementation(project(":platform:security"))
    implementation(libs.biometric)
    
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.core.ktx)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.kotlinx.datetime)
}
