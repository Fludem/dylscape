package org.rsmod.api.type.builders.comp

import jakarta.inject.Inject
import org.rsmod.api.type.builders.TypeBuilder
import org.rsmod.api.type.builders.resolver.TypeBuilderResolver
import org.rsmod.api.type.builders.resolver.TypeBuilderResult
import org.rsmod.api.type.builders.resolver.TypeBuilderResult.CachePackRequired
import org.rsmod.api.type.builders.resolver.TypeBuilderResult.FullSuccess
import org.rsmod.api.type.builders.resolver.TypeBuilderResult.NameNotFound
import org.rsmod.api.type.builders.resolver.err
import org.rsmod.api.type.builders.resolver.ok
import org.rsmod.api.type.builders.resolver.update
import org.rsmod.api.type.symbols.name.NameMapping
import org.rsmod.game.type.TypeResolver
import org.rsmod.game.type.comp.ComponentTypeBuilder
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.comp.UnpackedComponentType

public class ComponentBuilderResolver
@Inject
constructor(private val types: ComponentTypeList, private val nameMapping: NameMapping) :
    TypeBuilderResolver<ComponentTypeBuilder, UnpackedComponentType> {
    private val names: Map<String, Int>
        get() = nameMapping.components

    override fun resolve(
        builders: TypeBuilder<ComponentTypeBuilder, UnpackedComponentType>
    ): List<TypeBuilderResult> {
        val declared = mutableMapOf<String, Int>()
        return builders.cache.map { type ->
            val interfaceName = type.internalName.orEmpty().substringBefore(':')
            val position = declared.merge(interfaceName, 1, Int::plus)!! - 1
            type.resolve(position)
        }
    }

    private fun UnpackedComponentType.resolve(position: Int): TypeBuilderResult {
        val internalId = names[internalName] ?: return err(NameNotFound(internalName))

        checkDeclaredAtChildIndex(internalId, position)
        TypeResolver[this] = internalId

        val resolved = withPackedLayer(internalId)
        resolved.validateOps()

        val cacheType = types[internalId]
        return if (cacheType != resolved) {
            resolved.update(CachePackRequired)
        } else {
            resolved.ok(FullSuccess)
        }
    }

    /**
     * Rewrites [UnpackedComponentType.layer] from the plain child index authors write into the
     * packed parent id the cache format stores.
     *
     * The wire format only has room for the low 16 bits of `layer`, so `ComponentTypeEncoder`
     * truncates on the way out and `ComponentTypeDecoder` adds `combinedId and -65536` back on the
     * way in. A type authored without those high bits therefore decodes back as something it can
     * never equal, and the verifier would report `CacheUpdateRequired` on every boot: pack,
     * re-verify, still mismatch, and `packCache`'s second pass throws. Normalising here means an
     * author cannot get this wrong, and re-running on an already-packed value is a no-op.
     */
    private fun UnpackedComponentType.withPackedLayer(internalId: Int): UnpackedComponentType {
        if (layer == ComponentTypeBuilder.DEFAULT_LAYER) {
            return this
        }
        val childIndex = layer and 0xFFFF
        check(childIndex != 0xFFFF) {
            "`layer` is reserved for root components and cannot be authored: " +
                "$internalName (layer=$layer)."
        }
        val packedParent = (internalId and -0x10000) or childIndex
        return if (packedParent == layer) this else copy(layer = packedParent)
    }

    /**
     * Every component builder declares an interface's components in child-index order: parents are
     * passed to [ComponentBuilder.child] as that index. If `component.sym` numbers a component
     * differently from where it was declared, each `layer` would point at whatever sits at that
     * index instead of the intended parent, and the panel would pack scrambled without any error.
     *
     * The realistic way to get here is a `DesignedComponentBuilder` file whose layers were
     * reordered without saving through `tools/interface-designer`, which rewrites the symbol block
     * on save.
     */
    private fun UnpackedComponentType.checkDeclaredAtChildIndex(internalId: Int, position: Int) {
        val childIndex = internalId and 0xFFFF
        check(childIndex == position) {
            "`$internalName` is declared at position $position of its interface, but " +
                "component.sym gives it child index $childIndex. Declaration order is the child " +
                "index, so the symbol block is out of step with the builder (save the design " +
                "through tools/interface-designer, or fix the block by hand)."
        }
    }

    /**
     * A component that enables an op bit without supplying its text draws no menu entry at all, so
     * the button silently does nothing. Of roughly 23,000 vanilla components only three do this, so
     * treating it as an authoring error rather than a valid state costs nothing.
     *
     * [UnpackedComponentType.events] stores the v1 mask, where bits 1..10 are `Op1`..`Op10`.
     */
    private fun UnpackedComponentType.validateOps() {
        var remaining = (events shr 1) and OP_MASK
        var index = 0
        while (remaining != 0) {
            if (remaining and 1 != 0) {
                check(op.getOrNull(index)?.isNotEmpty() == true) {
                    "Component enables Op${index + 1} with no op text, so the client will draw no " +
                        "menu entry: $internalName (events=$events, op=${op.toList()})."
                }
            }
            remaining = remaining shr 1
            index++
        }
    }

    private companion object {
        /** Ten op bits, `Op1`..`Op10`. */
        private const val OP_MASK: Int = 0x3FF
    }
}
