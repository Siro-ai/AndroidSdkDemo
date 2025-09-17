plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlinAndroid)
}

repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/Siro-ai/AndroidSdkDist")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

android {
    namespace = "com.siro.demo"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.siro.demo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        debug {
            // no-op
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // >> local import for development
//     implementation(project(":recorder"))

    // >> import from github packages (ie client consumer)
    implementation("com.siro.recorder:siro-sdk:0.0.45")

    // >> local import from with minified build - useful when testing proguard and obfuscation
    // implementation(files("libs/recorder-release.aar"))
    // implementation(libs.androidx.lifecycle.service)
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // implementation(libs.retrofit)
    // implementation(libs.okhttp)
    // implementation(libs.androidx.work.runtime.ktx)
    // end local testing

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    implementation(libs.converter.gson) // TODO only used for debug purposes

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
