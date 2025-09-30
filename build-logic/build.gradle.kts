plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        create("androidLibrary") {
            id = libs.plugins.konnect.plugin.get().pluginId
            implementationClass = "com.kovcom.konnect.AndroidLibraryPlugin"
        }
    }
}