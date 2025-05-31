pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

rootProject.name = "axiom"
include("axiom-core")
include("axiom-arch:axiom-arch-ax08")
include("axiom-arch:axiom-arch-dev08")
include("axiom-arch:axiom-arch-dev16")
include("axiom-arch:axiom-arch-dev32")
include("axiom-arch:axiom-arch-dev64")
include("axiom-arch:axiom-arch-mcpc08")
include("axiom-arch:axiom-arch-mcpc16")
include("axiom-asm")
include("axiom-cli")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
