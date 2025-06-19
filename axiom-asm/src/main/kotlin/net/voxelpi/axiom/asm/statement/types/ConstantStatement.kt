package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.StringLike
import net.voxelpi.axiom.asm.type.ValueLike

public sealed interface ConstantStatement {

    @StatementType("constant")
    public data class IntegerConstant(
        val value: ValueLike,
    ) : ConstantStatement, ProgramElementStatement

    @StatementType("constant")
    public data class StringConstant(
        val value: StringLike,
    ) : ConstantStatement
}
