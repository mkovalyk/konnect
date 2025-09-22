import com.vanniktech.maven.publish.AndroidMultiVariantLibrary
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.kovcom.konnect.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.lifecycle.process)
}

mavenPublishing {

    publishToMavenCentral()
    coordinates("com.kovcom", "konnect", "0.1.4")
    configure(
        AndroidMultiVariantLibrary(
            sourcesJar = true,
            publishJavadocJar = false,
            includedBuildTypeValues = setOf("release"),
        )
    )

    pom {
        name.set("Konnect")
        description.set("Library to manage network connections in a much more efficient way.")
        inceptionYear.set("2025")
        url.set("https://github.com/mkovalyk/konnect/tree/main/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("mkovalyk")
                name.set("Mykhailo Kovalyk")
                url.set("https://github.com/mkovalyk/")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/dynodict/dynodict-android.git")
            developerConnection.set("scm:git:ssh://github.com/dynodict/dynodict-android.git")
            url.set("https://github.com/mkovalyk/konnect/tree/main")
        }
    }

    signAllPublications()
}

signing {
    sign(publishing.publications)
}
