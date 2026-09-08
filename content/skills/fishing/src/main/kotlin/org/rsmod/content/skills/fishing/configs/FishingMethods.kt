package org.rsmod.content.skills.fishing.configs

import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

/**
 * One fish a method can produce, with the level that unlocks it and the roll weights that decide
 * how often it is landed.
 *
 * [rateLow] is the weight at Fishing level 1 and [rateHigh] the weight at 99;
 * `ProtectedAccess.statRandom` interpolates between them by the player's level and rolls out of
 * 255. This is the same packing upstream's woodcutting uses for axes.
 */
data class FishingCatch(
    val fish: ObjType,
    val level: Int,
    val xp: Double,
    val rateLow: Int,
    val rateHigh: Int,
    /** Chat line on a successful catch. Held verbatim because OSRS varies "a" and "some". */
    val message: String,
)

/**
 * One way of fishing: the tool it needs, the bait it burns, its animation, and what it can land.
 *
 * [catches] is ordered **highest level requirement first**, because that is the order OSRS rolls
 * them in: each qualifying fish gets its own roll, and the first success wins. Sorting the other
 * way would make a level-99 player catch shrimps almost every time.
 */
data class FishingMethod(
    val tool: ObjType,
    val toolMessage: String,
    val bait: ObjType?,
    val baitMessage: String?,
    val anim: SeqType,
    val startMessage: String,
    val catches: List<FishingCatch>,
) {
    val lowestLevel: Int = catches.minOf { it.level }
}

/**
 * The spot archetypes, and the mapping from a spot npc's cache ops onto them.
 *
 * Fishing spots are npcs, and every one of them carries its methods on **op1 and op3** — op2 is
 * always null. There is no param or content group in the cache saying which method an op is; the op
 * text itself says it, and that is what the player reads too, so this module dispatches on the
 * **pair** of op texts rather than inventing a tagging scheme of its own.
 *
 * The pair matters because "Net" alone is ambiguous. Across all 92 standard spots in the rev 233
 * cache there are exactly seven op pairs, and they collapse to five archetypes:
 * ```
 *   Lure      | Bait      (27)  ->  lureBait
 *   Small Net | Bait      (22)  \
 *   Net       | Bait       (2)  /   netBait
 *   Cage      | Harpoon   (18)  ->  cageHarpoon
 *   Big Net   | Harpoon   (17)  \
 *   Net       | Harpoon    (5)  /   bigNetHarpoon
 *   Net       | hidden     (1)  ->  netOnly   (Tutorial Island)
 * ```
 *
 * So `Net` + `Bait` is a small net and `Net` + `Harpoon` is a big net — unambiguous once read as a
 * pair, and unresolvable if op1 is read alone. [FishingSpotConfigTest] pins that this holds for
 * every tagged spot, so a spot with an op combination we have never seen fails the build instead of
 * silently doing nothing when clicked.
 *
 * Levels and XP are the real OSRS values. The roll weights are **tuned to sit in the right
 * catch-per-hour band, not transcribed** — an individual pair may be a few points off live, but the
 * ordering between fish is faithful, which is what the grind actually feels like.
 */
object FishingSpots {
    private val smallNet =
        FishingMethod(
            tool = FishingObjs.net,
            toolMessage = "You need a small fishing net to catch these fish.",
            bait = null,
            baitMessage = null,
            anim = FishingSeqs.small_net,
            startMessage = "You cast out your net...",
            catches =
                listOf(
                    catch(FishingObjs.raw_anchovies, 15, 40.0, 16, 48, "You catch some anchovies."),
                    catch(FishingObjs.raw_shrimp, 1, 10.0, 34, 96, "You catch some shrimps."),
                ),
        )

    private val bigNet =
        FishingMethod(
            tool = FishingObjs.big_net,
            toolMessage = "You need a big fishing net to catch these fish.",
            bait = null,
            baitMessage = null,
            anim = FishingSeqs.big_net,
            startMessage = "You cast out your net...",
            catches =
                listOf(
                    catch(FishingObjs.raw_bass, 46, 100.0, 10, 40, "You catch a bass."),
                    catch(FishingObjs.raw_cod, 23, 45.0, 20, 60, "You catch a cod."),
                    catch(FishingObjs.raw_mackerel, 16, 20.0, 30, 90, "You catch a mackerel."),
                ),
        )

    private val bait =
        FishingMethod(
            tool = FishingObjs.fishing_rod,
            toolMessage = "You need a fishing rod to catch these fish.",
            bait = FishingObjs.fishing_bait,
            baitMessage = "You don't have any fishing bait left.",
            anim = FishingSeqs.rod,
            startMessage = "You cast out your line...",
            catches =
                listOf(
                    catch(FishingObjs.raw_herring, 10, 30.0, 24, 64, "You catch a herring."),
                    catch(FishingObjs.raw_sardine, 5, 20.0, 32, 80, "You catch a sardine."),
                ),
        )

    /**
     * The same "Bait" option, but on a lure spot it lands pike rather than the sardine/herring of a
     * net spot — the reason methods hang off the archetype rather than off the op text alone.
     */
    private val pikeBait =
        FishingMethod(
            tool = FishingObjs.fishing_rod,
            toolMessage = "You need a fishing rod to catch these fish.",
            bait = FishingObjs.fishing_bait,
            baitMessage = "You don't have any fishing bait left.",
            anim = FishingSeqs.rod,
            startMessage = "You cast out your line...",
            catches = listOf(catch(FishingObjs.raw_pike, 25, 60.0, 30, 90, "You catch a pike.")),
        )

    private val lure =
        FishingMethod(
            tool = FishingObjs.fly_fishing_rod,
            toolMessage = "You need a fly fishing rod to catch these fish.",
            bait = FishingObjs.feather,
            baitMessage = "You don't have any feathers left.",
            anim = FishingSeqs.rod,
            startMessage = "You cast out your line...",
            catches =
                listOf(
                    catch(FishingObjs.raw_salmon, 30, 70.0, 20, 70, "You catch a salmon."),
                    catch(FishingObjs.raw_trout, 20, 50.0, 32, 96, "You catch a trout."),
                ),
        )

    private val cage =
        FishingMethod(
            tool = FishingObjs.lobster_pot,
            toolMessage = "You need a lobster pot to catch these fish.",
            bait = null,
            baitMessage = null,
            anim = FishingSeqs.cage,
            startMessage = "You attempt to catch a lobster...",
            catches =
                listOf(catch(FishingObjs.raw_lobster, 40, 90.0, 15, 50, "You catch a lobster.")),
        )

    private val harpoon =
        FishingMethod(
            tool = FishingObjs.harpoon,
            toolMessage = "You need a harpoon to catch these fish.",
            bait = null,
            baitMessage = null,
            anim = FishingSeqs.harpoon,
            startMessage = "You start harpooning fish...",
            catches =
                listOf(
                    catch(FishingObjs.raw_swordfish, 50, 100.0, 10, 35, "You catch a swordfish."),
                    catch(FishingObjs.raw_tuna, 35, 80.0, 20, 65, "You catch a tuna."),
                ),
        )

    private val sharkHarpoon =
        FishingMethod(
            tool = FishingObjs.harpoon,
            toolMessage = "You need a harpoon to catch these fish.",
            bait = null,
            baitMessage = null,
            anim = FishingSeqs.harpoon,
            startMessage = "You start harpooning fish...",
            catches = listOf(catch(FishingObjs.raw_shark, 76, 110.0, 5, 22, "You catch a shark.")),
        )

    /** Tutorial Island's spot yields the tutorial's own shrimps, which cannot leave the island. */
    private val newbieNet =
        FishingMethod(
            tool = FishingObjs.net,
            toolMessage = "You need a small fishing net to catch these fish.",
            bait = null,
            baitMessage = null,
            anim = FishingSeqs.small_net,
            startMessage = "You cast out your net...",
            catches =
                listOf(
                    catch(FishingObjs.newbie_raw_shrimp, 1, 10.0, 96, 96, "You catch some shrimps.")
                ),
        )

    /**
     * Every distinct method any spot offers. Exposed so the config test can walk them all without
     * having to go via a spot, which would silently skip a method no archetype happened to use.
     */
    val allMethods: List<FishingMethod> =
        listOf(smallNet, bigNet, bait, pikeBait, lure, cage, harpoon, sharkHarpoon, newbieNet)

    /** op1 and op3 of one kind of spot. A null op3 means that option is hidden on this spot. */
    private data class Archetype(val op1: FishingMethod, val op3: FishingMethod?)

    private val archetypes: Map<Pair<String, String>, Archetype> =
        mapOf(
            ("Small Net" to "Bait") to Archetype(smallNet, bait),
            ("Net" to "Bait") to Archetype(smallNet, bait),
            ("Lure" to "Bait") to Archetype(lure, pikeBait),
            ("Cage" to "Harpoon") to Archetype(cage, harpoon),
            ("Big Net" to "Harpoon") to Archetype(bigNet, sharkHarpoon),
            ("Net" to "Harpoon") to Archetype(bigNet, sharkHarpoon),
            ("Net" to "hidden") to Archetype(newbieNet, null),
        )

    /**
     * Resolves what clicking [op] on [spot] should do, or null if this spot advertises a
     * combination of options the module does not know — in which case the click is ignored rather
     * than guessed at.
     */
    fun methodFor(spot: UnpackedNpcType, op: Int): FishingMethod? {
        val archetype = archetypes[spot.op1Text to spot.op3Text] ?: return null
        return when (op) {
            1 -> archetype.op1
            3 -> archetype.op3
            else -> null
        }
    }

    /** Exposed for the config test, which walks every tagged spot looking for an unknown pair. */
    fun isKnownSpot(spot: UnpackedNpcType): Boolean =
        archetypes.containsKey(spot.op1Text to spot.op3Text)

    val UnpackedNpcType.op1Text: String
        get() = op.getOrNull(0) ?: ""

    val UnpackedNpcType.op3Text: String
        get() = op.getOrNull(2) ?: ""

    private fun catch(
        fish: ObjType,
        level: Int,
        xp: Double,
        rateLow: Int,
        rateHigh: Int,
        message: String,
    ) = FishingCatch(fish, level, xp, rateLow, rateHigh, message)
}
