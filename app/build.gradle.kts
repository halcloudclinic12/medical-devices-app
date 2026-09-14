import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
//    Add the Google services Gradle plugin
    id("com.google.gms.google-services")

// Add the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics")

    /*id("com.android.application")
    id("org.jetbrains.kotlin.android")*/
//    id("com.google.dagger.hilt.android")
    /*id("dagger.hilt.android")
    id("kotlin-kapt")*/
}

var appName = "HAL Healthbox"

val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.test.healthbox_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.test.healthbox_app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true

//        setProperty("archivesBaseName", appName + "_mobile_" + "-v" + versionName)

        applicationVariants.all {
            outputs.all {
                val flavorPart = if (flavorName.isNullOrBlank()) "" else "-$flavorName"
                val buildTypePart = if (buildType.name.isNullOrBlank()) "" else "-${buildType.name}"

                val fileName = "${appName}_mobile_${flavorPart}${buildTypePart}-v${versionName}.apk"
                // this cast is sometimes needed depending on AGP version
                @Suppress("DEPRECATION")
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
            }
        }

//        buildConfigField("boolean", "ENABLE_LOGS", "false")

        // Shared BuildConfig fields
//        buildConfigField "int", "MAX_RETRIES", "3"
//        buildConfigField "boolean", "IS_CRASHLYTICS_ENABLED", "false"
    }

    flavorDimensions += "env"

    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_API", "\"https://dev-api.halcloudclinic.com/\"")
            buildConfigField("String", "ENVIRONMENT", "\"development\"")
        }

        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_API", "\"https://dev-api.halcloudclinic.com/\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {

        release {
            isMinifyEnabled = true
            isDebuggable = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

            buildConfigField("boolean", "ENABLE_LOGS", "false")

            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            isMinifyEnabled = false
            isDebuggable = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

            buildConfigField("boolean", "ENABLE_LOGS", "true")
        }
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true

        buildConfig = true
    }/*composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }*/
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    kapt.includeCompileClasspath = false
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
    implementation(libs.androidx.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    //Dagger - Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.fragment.ktx)

    // Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.runtime)
//    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //Load Images
    implementation(libs.picasso)
    implementation(libs.glide)

    //Material
    implementation(libs.material)

    //otp view
    implementation(libs.otpview)

    // Nav Drawer
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("com.google.android.material:material:1.13.0")

    //Gson
    implementation("com.google.code.gson:gson:2.12.1")

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    artifacts.add("default", file("Biosenselib.aar"))

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    artifacts.add("default", file("Prowess_SDK_v02.04.00-04.jar"))
    artifacts.add("default", file("libQBlueQPP.jar"))

    // Weighing Scale
    implementation(libs.ailinksdkrepositoryandroid)
    implementation(libs.ailinksdkparsinglibraryandroid)

    debugImplementation("com.github.chuckerteam.chucker:library:4.0.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.0.0")

    implementation("com.airbnb.android:lottie:6.1.0")

    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Add the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

//    implementation(name = "biosenselib-release".toString(), ext = "aar")

//    implementation("androidx.compose.compiler:compiler:1.5.8")

}
