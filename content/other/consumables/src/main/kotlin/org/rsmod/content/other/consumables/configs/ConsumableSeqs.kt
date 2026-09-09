package org.rsmod.content.other.consumables.configs

import org.rsmod.api.type.refs.seq.SeqReferences

/**
 * The consume animations.
 *
 * Neither is in `BaseSeqs`; a module-local `SeqReferences` is the established way to reach a cache
 * seq upstream never named, as `PrayerSeqs` does.
 *
 * No sound is played. `.data/symbols/synth.sym` is a curated 187-entry list carrying no eat or
 * drink effect, and the ids are not recoverable from the cache either: `EdibleObjDump` decoded the
 * frame sounds of every one of the 12,521 seqs in this revision and *none* of them carries one, so
 * the animation cannot be asked what it should sound like. Rather than write a guessed id into a
 * symbol file and have it look decoded, eating is silent until the real id is read off the wire.
 */
internal object ConsumableSeqs : SeqReferences() {
    /** 829. The generic one-handed bite; three ticks, which is the eat delay itself. */
    val eat = find("human_eat")

    /** 1194. Raising a vessel to the mouth, used for jugs, cups and tankards. */
    val drink = find("human_drink_rum")
}
