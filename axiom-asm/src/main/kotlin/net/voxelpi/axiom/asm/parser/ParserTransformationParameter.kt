package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.StatementParameter

public data class ParserTransformationParameter<T>(val parameter: StatementParameter<T>, val valueProvider: ParserTransformation.ArgumentState.() -> T)
