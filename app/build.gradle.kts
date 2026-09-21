plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.expensevault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.expensevault"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.2"
    }
    
    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("release") {
            val rootKeystore = File(project.rootDir, "kite-release.keystore")
            val keystoreFile = project.findProperty("KEYSTORE_FILE")?.toString()
                ?: System.getenv("KEYSTORE_FILE")
                ?: if (rootKeystore.exists()) rootKeystore.absolutePath else null

            if (keystoreFile != null && file(keystoreFile).exists()) {
                storeFile = file(keystoreFile)
                storePassword = project.findProperty("KEYSTORE_PASSWORD")?.toString()
                    ?: System.getenv("KEYSTORE_PASSWORD")
                    ?: "kiteandroid"
                keyAlias = project.findProperty("KEY_ALIAS")?.toString()
                    ?: System.getenv("KEY_ALIAS")
                    ?: "kite"
                keyPassword = project.findProperty("KEY_PASSWORD")?.toString()
                    ?: System.getenv("KEY_PASSWORD")
                    ?: "kiteandroid"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null && releaseConfig.storeFile!!.exists()) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
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
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":platform:security"))
    implementation(project(":platform:widget"))
    implementation(project(":platform:notification"))
    implementation(project(":feature:home"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:insights"))
    implementation(project(":feature:debts"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:recurring"))
    
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.biometric)
    
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    
    implementation(libs.kermit)
    implementation(libs.room.runtime)
    implementation(libs.kotlinx.serialization.json)
}
