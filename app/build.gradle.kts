import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing config is read from keystore.properties (kept out of git).
// Absent file = no release signing (local dev / CI without secrets still builds).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.notikeep"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.notikeep"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AppMetrica API key comes from gradle.properties / CI secrets
        // (notikeep.appmetrica.apiKey=...). Empty key = analytics stays local-only.
        val appMetricaKey = (project.findProperty("notikeep.appmetrica.apiKey") as? String).orEmpty()
        buildConfigField("String", "APPMETRICA_API_KEY", "\"$appMetricaKey\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Yandex demo banner — always fills, safe for development.
            buildConfigField("String", "ADS_BANNER_UNIT_ID", "\"demo-banner-yandex\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Use the real keystore when keystore.properties is present.
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Real РСЯ block id from gradle.properties / CI (notikeep.ads.bannerId=...).
            // Falls back to the demo banner until a real id is provided.
            val bannerId = (project.findProperty("notikeep.ads.bannerId") as? String)
                ?.takeIf { it.isNotBlank() } ?: "demo-banner-yandex"
            buildConfigField("String", "ADS_BANNER_UNIT_ID", "\"$bannerId\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging 3 (page-loaded per-app history and search)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // DataStore / WorkManager / Coroutines
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Coil for async app-icon loading
    implementation(libs.coil.compose)

    // AppMetrica analytics (consent-gated, anonymous UX events only)
    implementation(libs.appmetrica.analytics)

    // Yandex Mobile Ads (РСЯ)
    implementation(libs.yandex.mobileads)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
