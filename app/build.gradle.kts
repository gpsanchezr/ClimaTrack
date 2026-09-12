plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mantenimiento"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.mantenimiento"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // RecyclerView: se usa directamente (Adapters + ViewHolders) en Órdenes,
    // Equipos, Repuestos, Evidencias, Historial, Radar y Agenda. Antes no
    // estaba declarado y solo llegaba de forma transitiva a través de
    // 'material'; declararlo explícito evita que un futuro cambio en esa
    // librería rompa la compilación.
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Geolocalización (necesaria para obtener las coordenadas del cliente)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // OSMdroid para mapas libres y gratuitos
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("androidx.preference:preference-ktx:1.2.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}