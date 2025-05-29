plugins {
    id("axiom.build")
    id("axiom.publish")
}

dependencies {
    compileOnlyApi(libs.kotlin.stdlib)
    compileOnlyApi(libs.kotlin.reflect)
    compileOnlyApi(libs.bundles.kotlinx.coroutines)

    api(projects.axiomCore)
}
