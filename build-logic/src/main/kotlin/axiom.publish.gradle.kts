plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    `java-library`
    `maven-publish`
}

val javadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

kotlin {
    explicitApi()
}

publishing {
    repositories {
        maven {
            name = "VoxelPiRepo"
            val releasesRepoUrl = "https://repo.voxelpi.net/repository/maven-releases/"
            val snapshotsRepoUrl = "https://repo.voxelpi.net/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = findProperty("vpr.user") as String? ?: System.getenv("VOXELPI_REPO_USER")
                password = findProperty("vpr.key") as String? ?: System.getenv("VOXELPI_REPO_KEY")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["kotlin"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/voxelpi/axiom"

                licenses {
                    license {
                        name = "The MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "voxelpi"
                        name = "Peter Smek"
                        url = "https://voxelpi.net"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/voxelpi/axiom.git"
                    developerConnection = "scm:git:ssh://git@github.com/voxelpi/axiom.git"
                    url = "https://github.com/voxelpi/axiom"
                }

                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/voxelpi/axiom/issues"
                }

                ciManagement {
                    system = "GitHub Actions"
                    url = "https://github.com/voxelpi/axiom/actions"
                }
            }
        }
    }
}
