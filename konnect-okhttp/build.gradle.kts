plugins {
    alias(libs.plugins.konnect.plugin)
}

android {
    namespace = "com.kovcom.konnect.okhttp"
}

dependencies {
    implementation(project(":konnect-core"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.okhttp)
}
