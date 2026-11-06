plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val vCode: Int = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
val vName: String = (project.findProperty("versionName") as? String) ?: "1.0.0"

android {
    namespace = "com.calculator.landwood"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.calculator.landwood"
        minSdk = 24
        targetSdk = 34
        versionCode = vCode
        versionName = vName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("persistentKey") {
            storeFile = file("release.keystore")
            storePassword = "android123"
            keyAlias = "calculatorKey"
            keyPassword = "android123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("persistentKey")
        }
        debug {
            signingConfig = signingConfigs.getByName("persistentKey")
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
}
