import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()
val javaVersion = JavaVersion.VERSION_21

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.voxelpi.net/repository/maven-public/") }
    mavenLocal()
}

dependencies {
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.reflect)

    // Tests
    testImplementation(kotlin("stdlib"))
    testImplementation(libs.kotlin.reflect)
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    testImplementation(libs.slf4j.api)
    testImplementation(libs.log4j.slf4j.impl)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("io.lettuce.core.ExperimentalLettuceCoroutinesApi")
    }
}

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(javaVersion.toString().toInt())
    }

    dokka {
        basePublicationsDirectory.set(layout.buildDirectory.dir("docs"))
    }

    test {
        useJUnitPlatform()
    }

    ktlint {
        verbose.set(true)
        outputToConsole.set(true)
        coloredOutput.set(true)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
    }
}
