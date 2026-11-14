plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "app.naerio"
    compileSdk = 34
    // AAOS 주행 제한(CarUxRestrictionsManager) 사용
    useLibrary("android.car")

    defaultConfig {
        applicationId = "app.naerio"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    // 서명 값은 저장소 밖(~/.gradle/gradle.properties)에서 읽는다. 저장소에는 비밀 값이 없다.
    signingConfigs {
        create("release") {
            val storePath = providers.gradleProperty("NAERIO_STORE_FILE").orNull
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = providers.gradleProperty("NAERIO_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("NAERIO_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("NAERIO_KEY_PASSWORD").get()
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Pleos Connect SDK
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

// 공모전 제출용 release APK: ./gradlew exportReleaseApk
// → apk/Naerio_Naerio_release.apk (팀명_앱이름_release.apk)
tasks.register<Copy>("exportReleaseApk") {
    dependsOn("assembleRelease")
    val team = providers.gradleProperty("teamName").orElse("Naerio")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(rootProject.layout.projectDirectory.dir("apk"))
    rename { "${team.get()}_Naerio_release.apk" }
}
