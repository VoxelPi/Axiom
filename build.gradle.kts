plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.dokka)
}

allprojects {
    group = "net.voxelpi.axiom"
    version = "0.4.0-SNAPSHOT"
}
