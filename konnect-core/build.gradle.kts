import com.vanniktech.maven.publish.AndroidMultiVariantLibrary

plugins {
    alias(libs.plugins.konnect.plugin)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.kovcom.konnect.core"
}

dependencies {
    implementation(libs.androidx.lifecycle.process)
}

mavenPublishing {

    publishToMavenCentral()
    coordinates("com.kovcom", "konnect-core", "0.1.4")
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
