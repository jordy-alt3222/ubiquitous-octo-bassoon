plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.spelunkrtv"
    compileSdk = 37

    defaultConfig {
        // Change this before distributing anything: see README "Branding".
        applicationId = "com.example.spelunkrtv"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    // Release signing comes from Gradle properties (~/.gradle/gradle.properties or -P flags), never
    // from the repo: spelunkr.keystore, spelunkr.keystore.password, spelunkr.key.alias,
    // spelunkr.key.password. Without them, assembleRelease builds an unsigned APK.
    val keystore = providers.gradleProperty("spelunkr.keystore").orNull
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.gradleProperty("spelunkr.keystore.password").get()
                keyAlias = providers.gradleProperty("spelunkr.key.alias").get()
                keyPassword = providers.gradleProperty("spelunkr.key.password").get()
            }
        }
    }

    buildTypes {
        release {
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
