plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.olehkobylianskyi.familypoints.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.olehkobylianskyi.familypoints.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        // Public Web OAuth client ID (audience validated by Spring GoogleTokenVerifier).
        // Set via -PfamilyPointsGoogleWebClientId=... or a local gradle.properties.
        val googleWebClientId = providers.gradleProperty("familyPointsGoogleWebClientId")
            .orElse("").get().trim()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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