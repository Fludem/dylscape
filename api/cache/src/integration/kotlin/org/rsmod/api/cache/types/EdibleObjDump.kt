package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.seq.SeqFrameSound

/**
 * Decodes every obj the cache considers edible or drinkable.
 *
 * `HeldInteractions.hasOp` refuses to dispatch an op whose text is absent from the cache type, so
 * which objs can be eaten is a cache fact rather than a design choice - and the list is far too
 * long to enumerate by hand. This dump is what the consumables table is authored against.
 *
 * Noted items fall out for free: a `cert_*` obj carries no inv ops at all.
 */
class EdibleObjDump {
    @Test
    fun GameTestState.`dump edible objs`() = runBasicGameTest {
        val edible =
            cacheTypes.objs.values
                .filter { obj -> obj.iop.any { it in CONSUME_OPS } }
                .sortedBy { it.id }
        for (obj in edible) {
            println(
                "OBJ ${obj.id} sym=${obj.internalName} name='${obj.name}' " +
                    "iops=${obj.iop.toList()} stackable=${obj.stackable} " +
                    "certlink=${obj.certlink} cost=${obj.cost}"
            )
        }
        println("TOTAL_EDIBLE=${edible.size}")
    }

    @Test
    fun GameTestState.`dump consume op slots`() = runBasicGameTest {
        val bySlot = mutableMapOf<String, Int>()
        for (obj in cacheTypes.objs.values) {
            for ((index, op) in obj.iop.withIndex()) {
                if (op in CONSUME_OPS) {
                    bySlot.merge("$op@iop${index + 1}", 1, Int::plus)
                }
            }
        }
        for ((key, count) in bySlot.entries.sortedByDescending { it.value }) {
            println("SLOT $key -> $count")
        }
    }

    /**
     * Looks for the eat and drink sounds on the animations that would carry them.
     *
     * The answer, recorded here so nobody repeats the search: they are not there. Every one of the
     * 12,521 seqs in this revision decodes with an empty frame-sound array, `human_eat_withsound`
     * included, and `.data/symbols/synth.sym` is a curated 187-entry list naming neither sound. The
     * synth ids are therefore not recoverable from the cache, which is why eating is silent rather
     * than playing a guessed id.
     */
    @Test
    fun GameTestState.`dump consume anim sounds`() = runBasicGameTest {
        for (name in CONSUME_SEQS) {
            val seq = cacheTypes.seqs.values.firstOrNull { it.internalName == name }
            if (seq == null) {
                println("SEQ $name -> MISSING")
                continue
            }
            val sounds = seq.sounds.filter { it != SeqFrameSound.NULL }
            println(
                "SEQ ${seq.id} sym=$name ticks=${seq.tickDuration} " +
                    "sounds=${sounds.map { "synth=${it.type} loops=${it.loops}" }}"
            )
        }
    }

    private companion object {
        val CONSUME_OPS = setOf("Eat", "Drink")

        val CONSUME_SEQS =
            listOf(
                "human_eat",
                "human_eat_withsound",
                "human_eat_banana",
                "human_drink_rum",
                "human_drink_from_teacup_china",
                "human_drink_from_vial_cadava",
            )
    }
}
