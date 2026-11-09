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
        minSdk = 21
        targetSdk = 34
        versionCode = vCode
        versionName = vName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keyFile = file("release.keystore")
            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = "android123"
                keyAlias = "calculatorKey"
                keyPassword = "android123"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            val keyFile = file("release.keystore")
            if (keyFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            val keyFile = file("release.keystore")
            if (keyFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        ignoreWarnings = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
