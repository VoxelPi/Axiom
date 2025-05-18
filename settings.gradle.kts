pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

rootProject.name = "axiom"
include("axiom-core")
include("axiom-arch:axiom-arch-mcpc8")
include("axiom-arch:axiom-arch-mcpc16")
include("axiom-asm")
include("axiom-cli")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
