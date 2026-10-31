import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Host server pengembangan untuk build debug, dari local.properties (tidak di-commit):
//   debug.apiHost=192.168.1.15:3000   -> HP fisik di WiFi yang sama dengan PC
// Tanpa isian ini dipakai emulator (10.0.2.2 = localhost komputer).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val debugApiHost: String = localProperties.getProperty("debug.apiHost")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "10.0.2.2:3000"

android {
    namespace = "com.alhikmah.tpq"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.alhikmah.tpq"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // Emulator Android: 10.0.2.2 = localhost komputer; HP fisik: atur debug.apiHost di local.properties.
            // HTTP hanya diizinkan di build debug (src/debug/res/xml/network_security_config.xml).
            buildConfigField("String", "API_BASE_URL", "\"http://$debugApiHost/api/v1\"")
        }
        release {
            isMinifyEnabled = false
            // Satu-satunya tempat alamat server produksi. Wajib HTTPS (network_security_config di src/main melarang cleartext).
            buildConfigField("String", "API_BASE_URL", "\"https://tpq-alhikmah.roziqrizal.com/api/v1\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Tes memakai JUnit Jupiter dan kotlin.test, sama dengan Rizqflow.
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(project(":domain"))
    testImplementation(libs.org.json) // org.json asli; android.jar di tes unit hanya stub
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
