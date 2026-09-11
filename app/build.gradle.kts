import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

val privateSigningFile = providers.environmentVariable("KISHI_SIGNING_PROPERTIES").orNull?.let(::file)
val privateSigning = Properties().apply {
    if (privateSigningFile?.isFile == true) privateSigningFile.inputStream().use { load(it) }
}

android {
    namespace = "fr.kishiswitch.guildwars"
    compileSdk = 36
    buildToolsVersion = "36.1.0"
    defaultConfig {
        applicationId = "fr.kishiswitch.guildwars"
        minSdk = 36
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (privateSigning.isNotEmpty()) create("personal") {
            storeFile = file(privateSigning.getProperty("storeFile"))
            storePassword = privateSigning.getProperty("storePassword")
            keyAlias = privateSigning.getProperty("keyAlias")
            keyPassword = privateSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (privateSigning.isNotEmpty()) signingConfig = signingConfigs.getByName("personal")
        }
    }
    buildFeatures { aidl = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }
    lint { abortOnError = true }
}

dependencies {
    implementation(project(":engine"))
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
