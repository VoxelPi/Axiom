package net.voxelpi.axiom.asm.statement.argument

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.source.SourceLink

public interface Argument {

    public val source: SourceLink

    public interface ValueLike : Argument {

        public class Unparsed(
            override val source: SourceLink,
            public val value: String,
        ) : ValueLike
    }

    public interface RegisterLike : ValueLike {

        public class Unparsed(
            override val source: SourceLink,
            public val value: String,
        ) : RegisterLike
    }

    public interface AnchorLike : Argument

    public interface ScopeLike : Argument {

        public class NamedScopeReference(
            override val source: SourceLink,
            public val name: String,
        ) : ScopeLike
    }

    public interface UnitLike : Argument {

        public class NamedUnitReference(
            override val source: SourceLink,
            public val name: String,
        ) : UnitLike

        public class Unit(
            override val source: SourceLink,
            public val unit: CompilationUnit,
        ) : UnitLike
    }

    public class Text(
        override val source: SourceLink,
        public val value: String,
    ) : Argument

    public class Variable(
        override val source: SourceLink,
        public val name: String,
    ) : RegisterLike

    public class Integer(
        override val source: SourceLink,
        public val value: Long,
    ) : ValueLike
}
