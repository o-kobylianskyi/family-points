plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.olehkobylianskyi.familypoints.android"
    // CI runners currently lack the Android 37 preview platform.
    // Build against 36 only in CI; local IDE and release builds still target API 37.
    val ciSdk = providers.gradleProperty("familyPointsCiAndroidSdk").orElse("")
        .get().toIntOrNull()
    compileSdk {
        version = release(ciSdk ?: 37)
    }

    defaultConfig {
        applicationId = "com.olehkobylianskyi.familypoints.android"
        minSdk = 26
        targetSdk = ciSdk ?: 37
        // Installable updates require a monotonically increasing versionCode.
        versionCode = providers.gradleProperty("familyPointsVersionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("familyPointsVersionName").orElse("1.0").get()
        // Public Web OAuth client ID (audience validated by Spring GoogleTokenVerifier).
        // Set via -PfamilyPointsGoogleWebClientId=... or a local gradle.properties.
        val googleWebClientId = providers.gradleProperty("familyPointsGoogleWebClientId")
            .orElse("").get().trim()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Never commit the release key or its passwords. Provide env vars at build time.
    val releaseStoreFile = providers.environmentVariable("FP_ANDROID_KEYSTORE_FILE").orElse("").get()
    val releaseStorePassword = providers.environmentVariable("FP_ANDROID_KEYSTORE_PASSWORD").orElse("").get()
    val releaseKeyAlias = providers.environmentVariable("FP_ANDROID_KEY_ALIAS").orElse("").get()
    val releaseKeyPassword = providers.environmentVariable("FP_ANDROID_KEY_PASSWORD").orElse("").get()
    val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
        .all { it.isNotBlank() }
    if (hasReleaseSigning) {
        signingConfigs {
            create("familyPointsRelease") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Override only for an explicit BETA test build:
            // ./gradlew :app:assembleDebug -PfamilyPointsDebugApiUrl=https://beta.example.com/
            val debugApiUrl = providers.gradleProperty("familyPointsDebugApiUrl")
                .orElse("https://api-dev.family-point.com:8443/")
                .get()
                .trimEnd('/') + "/"
            require(debugApiUrl.startsWith("https://")) {
                "Debug API URL must use HTTPS"
            }
            buildConfigField(
                "String", "API_BASE_URL",
                "\"$debugApiUrl\""
            )
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("familyPointsRelease")
            val releaseApiUrl = providers.gradleProperty("familyPointsReleaseApiUrl")
                .orElse("https://family-point.com/")
                .get()
                .trimEnd('/') + "/"
            require(releaseApiUrl.startsWith("https://")) {
                "Release API must use HTTPS"
            }
            buildConfigField(
                "String", "API_BASE_URL",
                "\"$releaseApiUrl\""
            )
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}