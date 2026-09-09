package org.rsmod.api.type.builders.comp

import org.rsmod.api.type.builders.HashTypeBuilder
import org.rsmod.game.type.comp.ComponentTypeBuilder
import org.rsmod.game.type.comp.UnpackedComponentType

/**
 * Authors interface components, which are packed into js5 archive 3 by
 * [org.rsmod.api.cache.types.comp.ComponentTypeEncoder].
 *
 * Interfaces themselves need no builder: `InterfaceTypeList.from` derives them by grouping decoded
 * components on their parent id, so writing components into the archive is enough to bring a whole
 * new interface into existence.
 *
 * Both the interface name and every component name must already exist in `.data/symbols`, since an
 * unresolved name is a hard boot failure. Server-authored ids belong in `.data/symbols/.local/`.
 *
 * ### Example usage
 *
 * ```
 * object MyPanelBuilder : ComponentBuilder() {
 *     init {
 *         build("my_panel:root") {
 *             type = 0
 *             width = 480
 *             height = 300
 *             // Fixed pixel size, centred in whatever container it is opened into.
 *             xMode = 1
 *             yMode = 1
 *         }
 *         child("my_panel:background", parent = 0) {
 *             type = 3
 *             colour1 = 0x1E1710
 *             fill = true
 *         }
 *     }
 * }
 * ```
 */
public abstract class ComponentBuilder :
    HashTypeBuilder<ComponentTypeBuilder, UnpackedComponentType>() {
    /**
     * Declares a root component - one with no parent.
     *
     * [ComponentTypeBuilder.v3] is set for you: rev 233 authors everything in the v3 format, and
     * the engine builder hard-requires it.
     */
    override fun build(internal: String, init: ComponentTypeBuilder.() -> Unit) {
        val builder = ComponentTypeBuilder(internal)
        builder.v3 = true
        builder.apply(init)
        // As with every other builder, `id = -1` is a placeholder; the real id comes from the
        // symbol lookup in `ComponentBuilderResolver`.
        cache += builder.build(id = -1)
    }

    /**
     * Declares a component parented to the one at child index [parent] *within the same interface*.
     *
     * Pass the plain child index. [ComponentBuilderResolver] rewrites it into the packed parent id
     * the cache format actually stores, once the interface id is known from the symbol table.
     */
    public fun child(internal: String, parent: Int, init: ComponentTypeBuilder.() -> Unit = {}) {
        require(parent in 0..0xFFFE) { "`parent` must be a child index, but was: $parent." }
        build(internal) {
            layer = parent
            init()
        }
    }
}
