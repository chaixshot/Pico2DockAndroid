plugins {
    alias(libs.plugins.android.application)
}

android {
    signingConfigs {
        create("pico2dock") {
            storeFile = file(".\\app\\src\\main\\res\\raw\\keystore.jks")
            storePassword = "forpick2dock"
            keyPassword = "forpick2dock"
            keyAlias = "H@mer"
        }
    }
    namespace = "com.hamer.pico2dock"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    androidResources {
        localeFilters += setOf(
            "cs", "da", "nl", "en", "en-rGB", "fi", "fr", "de", "el", "it", "ja", "ko", "ms",
            "nb", "pl", "pt-rPT", "pt-rBR", "ro", "ru", "es-rUS", "es", "sv", "th", "tr",
            "zh-rCN", "zh-rTW", "zh-rHK"
        )
    }

    defaultConfig {
        applicationId = "com.hamer.pico2dock"
        minSdk = 29
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 120
        versionName = "1.2.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("pico2dock")
    }

    buildTypes {
        release {
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.work.runtime)
    implementation(files("../libs/APKEditor-1.4.8.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.github.TutorialsAndroid:FilePicker:v9.0.1")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.github.patrickfav:uber-apk-signer:v1.3.0")
    implementation("com.github.srikanth-lingala:zip4j:v2.11.4")
}