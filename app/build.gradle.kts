import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

    val debugBackendBaseUrl = providers.gradleProperty("SPO_SWITCH_API_BASE_URL")
    .orElse("http://127.0.0.1:8080")
    .get()
val releaseBackendBaseUrl = providers.gradleProperty("SPO_SWITCH_RELEASE_API_BASE_URL")
    .orElse(providers.environmentVariable("SPO_SWITCH_RELEASE_API_BASE_URL"))
    .orElse("")
    .get()

// Kakao Map native app key. Keep it out of git: local.properties, a Gradle property, or an env var.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val kakaoNativeAppKey = providers.gradleProperty("KAKAO_NATIVE_APP_KEY")
    .orElse(providers.environmentVariable("KAKAO_NATIVE_APP_KEY"))
    .orElse(localProperties.getProperty("KAKAO_NATIVE_APP_KEY", ""))
    .get()

fun buildConfigString(value: String) = "\"" + value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\""

android {
    namespace = "com.example.sposwitch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sposwitch"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", buildConfigString(kakaoNativeAppKey))
    }

    buildTypes {
        debug {
            buildConfigField("String", "BACKEND_BASE_URL", buildConfigString(debugBackendBaseUrl))
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "BACKEND_BASE_URL", buildConfigString(releaseBackendBaseUrl))
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
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kakao.map)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

apply(from = rootProject.file("scripts/local-android.gradle"))
