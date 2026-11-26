plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.yazhi.saigai"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.yazhi.saigai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-m1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    }

    // MediaPipe ships OpenCV + JNI libraries for four ABIs (~160 MB of .so files).
    // Splitting per ABI keeps the installable APK near 50 MB instead of 200 MB.
    // arm64-v8a is the demo phone; x86_64 is the emulator used for verification.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")

    // M2 camera pipeline (design doc B1 §4/§11): CameraX preview with a lifecycle
    // binding. Camera frames stay on device; nothing here opens a socket.
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // M2 hand-landmark stage (design doc B1 §4). CPU delegate, bundled .task asset,
    // no network path: the manifest still declares no INTERNET permission.
    // 0.10.28 is the first version whose AAR ships an x86_64 JNI library; 0.10.14
    // only had arm64-v8a / armeabi-v7a / x86, which crashes with
    // UnsatisfiedLinkError on x86_64 emulators and test devices.
    // NOTE: MediaPipe hard-requires com.google.android.datatransport at runtime
    // (excluding it throws "Failed resolution of TransportRuntime" on init), even
    // though this app only uses a bundled model. The library is therefore present
    // but inert: the merged manifest strips INTERNET and ACCESS_NETWORK_STATE below,
    // so every socket call it could attempt fails with SecurityException. Verified
    // with `aapt2 dump badging` — see docs/guides/BUILD_AND_RUN.md.
    implementation("com.google.mediapipe:tasks-vision:0.10.28")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // JVM unit tests. org.json is a real implementation for the test classpath:
    // Android's android.jar org.json is a stub that throws "not mocked" off-device.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
