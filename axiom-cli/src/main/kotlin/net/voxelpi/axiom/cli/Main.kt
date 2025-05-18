package net.voxelpi.axiom.cli

import io.github.oshai.kotlinlogging.KotlinLogging

fun main() {
    val logger = KotlinLogging.logger {}
    logger.info { "Hello World!" }
}
