plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.castano"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.castano"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // ← habilita desugaring

    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Core y ciclo de vida
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Jetpack Compose UI
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.4")

    // Preferencias (para guardar usuario/proyecto)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Swipe Refresh (para recargar pantallas como Reconteo o Productos)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    // ZXing para generación de QR o manejo de códigos
    implementation("com.google.zxing:core:3.5.2")
    implementation("androidx.compose.foundation:foundation:1.5.0")
    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")



    implementation("androidx.exifinterface:exifinterface:1.3.7")
    coreLibraryDesugaring(libs.desugar.jdk)     // ← dependencia

    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("com.github.tony19:logback-android:3.0.0")



}
