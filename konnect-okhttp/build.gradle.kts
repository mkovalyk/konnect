import com.kovcom.konnect.Developer
import com.kovcom.konnect.Konnect
import com.kovcom.konnect.License
import com.kovcom.konnect.Scm
import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    alias(libs.plugins.konnect.plugin)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.kovcom.konnect.okhttp"
    flavorDimensions += "version"
    productFlavors {
        create("project") {
            dimension = "version"
        }
        create("maven") {
            dimension = "version"
        }
    }
}

dependencies {
    "projectImplementation"(project(":konnect-core"))
    "mavenImplementation"(libs.konnect.core)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.okhttp)
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(Konnect.GROUP, Konnect.ARTIFACT_ID_OK_HTTP, Konnect.VERSION)
    configure(
        AndroidMultiVariantLibrary(
            sourcesJar = true,
            publishJavadocJar = false,
            includedBuildTypeValues = setOf("release"),
            includedFlavorDimensionsAndValues = mapOf("version" to setOf("maven"))
        )
    )

    pom {
        name.set("Konnect-okhttp")
        description.set("Library to manage network connections in a much more efficient way.")
        inceptionYear.set("2025")
        url.set(Konnect.URL)
        licenses {
            license {
                name.set(License.NAME)
                url.set(License.URL)
                distribution.set(License.DISTRIBUTION)
            }
        }
        developers {
            developer {
                id.set(Developer.ID)
                name.set(Developer.NAME)
                url.set(Developer.URL)
            }
        }
        scm {
            connection.set(Scm.CONNECTION)
            developerConnection.set(Scm.DEVELOPER_CONNECTION)
            url.set(Scm.URL)
        }
    }

    signAllPublications()
}

signing {
    sign(publishing.publications)
}