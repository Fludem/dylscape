package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * Structural templates for hand-authored interfaces.
 *
 * We can now pack our own components into js5 archive 3, but the client gives no diagnostics: a
 * component it parses but cannot draw renders blank or misplaced rather than erroring. Copying
 * field values off interfaces the client already renders correctly is far cheaper than guessing
 * them and repacking to find out.
 *
 * `messagebox_titled` (923) is the smallest complete panel in the cache - root, frame, content and
 * three text lines. `ironman_setup` (890) is the closest vanilla analogue of our own chooser: an
 * info pane, a button container and a row of mode buttons, and it is where the sprite ids for a
 * native-looking panel come from.
 *
 * Prints every field [org.rsmod.api.cache.types.comp.ComponentTypeEncoder] writes for the v3
 * format, in write order, so an authored component can be diffed field-for-field against a
 * known-good one.
 *
 * Gradle does not print test stdout - read `system-out` out of
 * `api/cache/build/test-results/integration/TEST-*.xml`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CustomInterfaceTemplateDump {
    @Test
    fun GameTestState.`dump v3 template interfaces`() = runBasicGameTest {
        for ((id, name) in TEMPLATES) {
            dumpInterface(id, name)
        }
    }

    /** Re-run after packing to confirm our own interface decodes back as authored. */
    @Test
    fun GameTestState.`dump authored interfaces`() = runBasicGameTest {
        val authored =
            cacheTypes.components.values.map { it.interfaceId }.filter { it >= LOCAL_ID_FLOOR }
        if (authored.isEmpty()) {
            println("=== no authored interfaces (id >= $LOCAL_ID_FLOOR) present in the cache")
            return@runBasicGameTest
        }
        for (id in authored.distinct().sorted()) {
            dumpInterface(id, name = "authored")
        }
    }

    private fun GameTestState.dumpInterface(id: Int, name: String) {
        val comps =
            cacheTypes.components.values.filter { it.interfaceId == id }.sortedBy { it.component }
        println("=== INTERFACE $id ($name) components=${comps.size}")
        for (c in comps) {
            println("  $id:${c.component} ${c.internalName}")
            println("    v3=${c.v3} type=${c.type} clientCode=${c.clientCode}")
            println("    x=${c.x} y=${c.y} w=${c.width} h=${c.height}")
            println(
                "    widthMode=${c.widthMode} heightMode=${c.heightMode} " +
                    "xMode=${c.xMode} yMode=${c.yMode}"
            )
            val parentChild = if (c.layer == -1) "root" else "${c.layer and 0xFFFF}"
            println("    layer=${c.layer} (parent=$parentChild) hide=${c.hide}")
            when (c.type) {
                0 ->
                    println(
                        "    scrollW=${c.scrollWidth} scrollH=${c.scrollHeight} " +
                            "noClickThrough=${c.noClickThrough}"
                    )
                3 -> println("    colour1=${c.colour1.asHex()} fill=${c.fill} trans1=${c.trans1}")
                4 ->
                    // Never pass authored copy through `format`: a description containing "20%."
                    // parses as a conversion and throws.
                    println(
                        "    font=${c.textFont} lineH=${c.textLineHeight} alignH=${c.textAlignH} " +
                            "alignV=${c.textAlignV} shadow=${c.textShadow} " +
                            "colour1=${c.colour1.asHex()} text=\"${c.text}\""
                    )
                5 ->
                    println(
                        "    graphic=${c.graphic} angle2d=${c.angle2d} tiling=${c.tiling} " +
                            "trans1=${c.trans1} outline=${c.outline} shadow=${c.graphicShadow} " +
                            "vFlip=${c.vFlip} hFlip=${c.hFlip}"
                    )
                6 ->
                    println(
                        "    model=${c.model} modelX=${c.modelX} modelY=${c.modelY} " +
                            "zoom=${c.modelZoom} anim=${c.modelAnim} orthog=${c.modelOrthog}"
                    )
                9 ->
                    println(
                        "    lineWid=${c.lineWid} colour1=${c.colour1.asHex()} " +
                            "dir=${c.lineDirection}"
                    )
            }
            val ops = c.op.withIndex().filter { it.value.isNotEmpty() }
            println(
                "    events=${c.events} opBase=\"${c.opBase}\" " +
                    "op=${ops.joinToString { "op${it.index + 1}='${it.value}'" }}"
            )
            val hooks =
                listOf(
                        "onLoad" to c.onLoad,
                        "onMouseOver" to c.onMouseOver,
                        "onMouseLeave" to c.onMouseLeave,
                        "onVarTransmit" to c.onVarTransmit,
                        "onInvTransmit" to c.onInvTransmit,
                        "onStatTransmit" to c.onStatTransmit,
                        "onTimer" to c.onTimer,
                        "onOp" to c.onOp,
                        "onClick" to c.onClick,
                        "onScrollWheel" to c.onScrollWheel,
                    )
                    .filter { it.second != null }
            if (hooks.isEmpty()) {
                println("    hooks=<none>")
            }
            for ((label, hook) in hooks) {
                println("    $label=${hook!!.contentToString()}")
            }
        }
    }

    private fun Int.asHex(): String = "0x%06X".format(this and 0xFFFFFF)

    private companion object {
        private val TEMPLATES = linkedMapOf(923 to "messagebox_titled", 890 to "ironman_setup")

        /** `.local` symbol ids start here; upstream `interface.sym` tops out at 924. */
        private const val LOCAL_ID_FLOOR = 1000
    }
}
