import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    id("axiom.build")
    alias(libs.plugins.shadow)
    application
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.cli)
    implementation(libs.bundles.kotlinx.coroutines)

    // Project
    api(projects.axiomCore)
    api(projects.axiomAsm)
    api(projects.axiomArch.axiomArchAx08)
    api(projects.axiomArch.axiomArchDev64)
    api(projects.axiomArch.axiomArchMcpc8)
    api(projects.axiomArch.axiomArchMcpc16)

    // Logging
    implementation(libs.kotlin.logging.jvm)
    runtimeOnly(libs.slf4j.api)
    runtimeOnly(libs.log4j.slf4j.impl)
}

application {
    mainClass.set("net.voxelpi.axiom.cli.MainKt")
}

// See https://docs.gradle.org/8.12/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
interface Injected {
    @get:Inject val fs: FileSystemOperations
}

tasks {
    shadowJar {
        transform(Log4j2PluginsCacheFileTransformer::class.java)

        val injected = project.objects.newInstance<Injected>()
        doLast {
            injected.fs.copy {
                from(outputs.files.singleFile)
                into(rootProject.layout.buildDirectory.dir("libs"))

                rename { "axiom-cli-${version}.jar" }
            }
        }
    }

    named<JavaExec>("run") {
        standardInput = System.`in`
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "net.voxelpi.axiom.cli.MainKt",
                "Multi-Release" to true
            )
        }
    }
}
