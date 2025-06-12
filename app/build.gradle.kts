import java.text.SimpleDateFormat
import java.util.Date
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.bigbirdbrother.recordaudio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bigbirdbrother.recordaudio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
        compose = true
    }

    // AGP 8.0+ 兼容的APK文件名修改配置
    applicationVariants.configureEach {
        val variant = this
        val appName = "Audio Transcriber" // 自定义应用名称
        val buildType = variant.buildType.name
        val versionName = variant.versionName
        val versionCode = variant.versionCode
        val date = SimpleDateFormat("yyyyMMdd").format(Date())

        outputs.configureEach {
            val output = this as com.android.build.gradle.api.ApkVariantOutput
            output.outputFileName = "${appName}_v${versionName}_${buildType}_${date}.apk"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 你要添加的依赖（手动添加，不走版本 catalog）
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation ("androidx.drawerlayout:drawerlayout:1.1.1")
//    implementation("androidx.preference:preference-ktx:1.2.1")
    kapt("androidx.room:room-compiler:2.6.1")
    // 添加JSON解析支持
    implementation("org.json:json:20231013")
}