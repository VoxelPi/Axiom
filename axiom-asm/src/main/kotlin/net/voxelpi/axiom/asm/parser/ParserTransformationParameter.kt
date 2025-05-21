package net.voxelpi.axiom.asm.parser

import kotlin.reflect.KType

public data class ParserTransformationParameter<T>(val id: String, val type: KType, val value: T)
