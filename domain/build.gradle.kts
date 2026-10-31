// Kotlin/JVM murni: tidak boleh bergantung pada Android. Unit test berjalan cepat di JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

dependencies {
    // org.json sudah ada di Android (android.jar), jadi cukup compileOnly; di JVM untuk tes dipasang lewat testImplementation.
    compileOnly(libs.org.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.org.json)
    testImplementation(platform(libs.junit.bom))
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
