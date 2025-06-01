plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {

    buildFeatures {
        buildConfig = true
    }
    namespace = "com.nishant.cancerprediction"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nishant.cancerprediction"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEFAULT_WEB_CLIENT_ID", "\"${project.properties["DEFAULT_WEB_CLIENT_ID"]}\"")

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
}

dependencies {

    implementation("com.google.android.material:material:1.8.0")


    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.google.android.material:material:1.12.0")


    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")



    implementation("androidx.browser:browser:1.3.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20230227")

    implementation("com.google.android.play:integrity:1.3.0")

    implementation("org.tensorflow:tensorflow-lite:2.12.0")

    implementation("androidx.cardview:cardview:1.0.0")

    implementation("com.google.android.material:material:1.11.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")


    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth")

    implementation("com.hbb20:ccp:2.5.0")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}