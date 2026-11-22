plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.syedhunain.hisaabi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.syedhunain.hisaabi"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.3"
    }

    // Release (upload) key comes from GitHub Actions secrets; it is never stored in the repo.
    val releaseKeystore = System.getenv("HISAABI_KEYSTORE")

    signingConfigs {
        // Fixed test key so the SHA-1 registered in Google Cloud stays the same on every build.
        // Only for test builds — the Play Store release uses a separate private key.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("HISAABI_KEYSTORE_PASSWORD")
                keyAlias = "hisaabi"
                keyPassword = System.getenv("HISAABI_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Google's official TEST ad units: safe to click, never earn money.
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_NATIVE", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_REWARDED", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "AD_APP_OPEN", "\"ca-app-pub-3940256099942544/9257395921\"")
        }
        release {
            // Real Hisaabi ad units (AdMob account SyedTechdevelopers).
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-8064134362670148/4698155188\"")
            buildConfigField("String", "AD_NATIVE", "\"ca-app-pub-8064134362670148/9674937660\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-8064134362670148/8235356774\"")
            buildConfigField("String", "AD_REWARDED", "\"ca-app-pub-8064134362670148/9548438440\"")
            buildConfigField("String", "AD_APP_OPEN", "\"ca-app-pub-8064134362670148/5488261162\"")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.maxHeapSize = "3g"
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Google sign-in (authorization for Drive app folder) and AdMob
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Screenshot tests (run on GitHub Actions)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
