plugins {
    id("axiom.build")
    id("axiom.publish")
    alias(libs.plugins.blossom)
    alias(libs.plugins.indra.git)
}

dependencies {
    // Kotlin
    compileOnlyApi(libs.kotlin.stdlib)
    compileOnlyApi(libs.kotlin.reflect)
    compileOnlyApi(libs.bundles.kotlinx.coroutines)

    // Logging
    compileOnlyApi(libs.kotlin.logging.jvm)
}

sourceSets {
    main {
        blossom {
            kotlinSources {
                property("version", project.version.toString())
                property("git_commit", indraGit.commit()?.name ?: "<none>")
                property("git_branch", indraGit.branchName() ?: "<none>")
            }
        }
    }
}
