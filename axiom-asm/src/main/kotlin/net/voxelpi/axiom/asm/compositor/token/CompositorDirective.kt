package net.voxelpi.axiom.asm.compositor.token

import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.SourcedValue

internal sealed interface CompositorDirective : CompositorToken {

    data class Definition(
        override val source: SourceReference,
        val id: NamespacedId,
    ) : CompositorDirective

    data class Label(
        override val source: SourceReference,
        val id: NamespacedId,
    ) : CompositorDirective

    data class Region(
        override val source: SourceReference,
    ) : CompositorDirective

    data class Insert(
        override val source: SourceReference,
        val parameters: List<List<CompositorToken>>,
    ) : CompositorDirective

    sealed interface Visibility : CompositorDirective {

        data class Global(
            override val source: SourceReference,
        ) : Visibility

        data class Public(
            override val source: SourceReference,
            val target: SourcedValue<NamespacedId>?,
        ) : Visibility

        data class Private(
            override val source: SourceReference,
        ) : Visibility
    }

    sealed interface Location : CompositorDirective {

        data class At(
            override val source: SourceReference,
            val address: CompositorDirectiveParameterValue<ULong>,
        ) : Location

        data class In(
            override val source: SourceReference,
            val regionId: SourcedValue<NamespacedId>,
        ) : Location
    }
}
