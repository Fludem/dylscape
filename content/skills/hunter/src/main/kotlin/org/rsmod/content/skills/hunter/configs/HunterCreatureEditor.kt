package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.content.skills.hunter.TrapKind
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.hunt.HuntModeType
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

/**
 * Everything the engine needs to know about a huntable creature, plus the movement settings that
 * keep it in its hunting ground.
 *
 * **On wandering.** `wanderRange` is an RSMod-only cache opcode that the vanilla OSRS cache never
 * carries, so out of the box every one of these creatures inherits `NpcTypeBuilder`'s defaults —
 * `wanderRange = 5`, `maxRange = 7`, `defaultMode = Wander` — and drifts off its patch. Hunter
 * creatures *should* roam; that is half the skill. So these are set deliberately per family rather
 * than pinned to zero the way a shopkeeper would be: birds and kebbits get a wide patch,
 * salamanders a tight one because they sit near water.
 *
 * **The `maxRange` rule.** `NpcInteractionProcessor` cancels an npc's interaction once it is
 * further than `maxRange + attackRange` from its *spawn* tile, so a creature that has wandered a
 * few tiles and then targets a trap several more tiles away will silently give up halfway. Every
 * family below therefore satisfies `maxRange > wanderRange + huntRange`. `HunterConfigTest` asserts
 * it, because getting this wrong produces creatures that approach a trap and then turn around,
 * which looks like a broken trap rather than a broken range.
 *
 * Levels and XP are the live OSRS values. The catch weights are tuned to land in the right
 * catches-per-hour band rather than transcribed, in the same spirit as `MiningRates`: what they get
 * right is the ordering and the falloff as creature tier rises.
 */
internal object HunterCreatureEditor : NpcEditor() {
    init {
        // -- Bird snare: four biome birds, the low end of the skill. --------------------------
        bird(
            HunterNpcs.crimson_swift,
            1,
            34.0,
            HunterLocs.snare_trapping_jungle,
            HunterLocs.snare_full_jungle,
        )
        bird(
            HunterNpcs.golden_warbler,
            5,
            47.0,
            HunterLocs.snare_trapping_desert,
            HunterLocs.snare_full_desert,
        )
        bird(
            HunterNpcs.copper_longtail,
            9,
            61.0,
            HunterLocs.snare_trapping_woodland,
            HunterLocs.snare_full_woodland,
        )
        bird(
            HunterNpcs.cerulean_twitch,
            11,
            64.2,
            HunterLocs.snare_trapping_polar,
            HunterLocs.snare_full_polar,
        )

        // -- Box trap ------------------------------------------------------------------------
        boxed(
            HunterNpcs.ferret,
            27,
            115.0,
            HunterEnums.box_dirs_ferret,
            HunterLocs.box_full_ferret,
            null,
        )
        boxed(
            HunterNpcs.chinchompa,
            53,
            198.0,
            HunterEnums.box_dirs_chinchompa,
            HunterLocs.box_full_chin,
            HunterObjs.chinchompa,
        )
        boxed(
            HunterNpcs.carnivorous_chinchompa,
            63,
            265.0,
            HunterEnums.box_dirs_chinchompa_big,
            HunterLocs.box_full_chin_big,
            HunterObjs.red_chinchompa,
        )
        boxed(
            HunterNpcs.black_chinchompa,
            73,
            315.0,
            HunterEnums.box_dirs_chinchompa_black,
            HunterLocs.box_full_chin_black,
            HunterObjs.red_chinchompa,
        )

        // -- Net trap ------------------------------------------------------------------------
        // The salamander itself is the product; the obj shares the npc's name in the cache.
        netted(
            HunterNpcs.swamp_lizard,
            29,
            152.0,
            HunterLocs.net_catching_green,
            HunterLocs.net_full_green,
        )
        netted(
            HunterNpcs.orange_salamander,
            47,
            224.0,
            HunterLocs.net_catching_orange,
            HunterLocs.net_full_orange,
        )
        netted(
            HunterNpcs.red_salamander,
            59,
            272.0,
            HunterLocs.net_catching_red,
            HunterLocs.net_full_red,
        )
        netted(
            HunterNpcs.black_salamander,
            67,
            319.0,
            HunterLocs.net_catching_black,
            HunterLocs.net_full_black,
        )

        // -- Deadfall ------------------------------------------------------------------------
        kebbit(
            HunterNpcs.wild_kebbit,
            23,
            128.0,
            HunterLocs.deadfall_trapping_claw,
            HunterLocs.deadfall_full_claw,
            HunterObjs.kebbit_claws,
        )
        kebbit(
            HunterNpcs.barb_tailed_kebbit,
            33,
            166.5,
            HunterLocs.deadfall_trapping_barbed,
            HunterLocs.deadfall_full_barbed,
            HunterObjs.long_kebbit_spike,
        )
        kebbit(
            HunterNpcs.prickly_kebbit,
            37,
            204.0,
            HunterLocs.deadfall_trapping_spike,
            HunterLocs.deadfall_full_spike,
            HunterObjs.kebbit_spike,
        )
        kebbit(
            HunterNpcs.sabre_toothed_kebbit,
            51,
            200.0,
            HunterLocs.deadfall_trapping_sabre,
            HunterLocs.deadfall_full_sabre,
            HunterObjs.kebbit_teeth,
        )
    }

    private fun bird(type: NpcType, level: Int, xp: Double, trapping: LocType, full: LocType) {
        creature(type, TrapKind.SNARE, level, xp, HunterHunt.snare, wander = 4, max = 12) {
            param[HunterParams.loc_trapping] = trapping
            param[HunterParams.loc_full] = full
            param[params.skill_productitem] = HunterObjs.raw_bird_meat
            param[HunterParams.secondary_product] = HunterObjs.feather
            param[HunterParams.secondary_count] = 5
            param[HunterParams.trap_return] = HunterObjs.bird_snare
        }
    }

    private fun boxed(
        type: NpcType,
        level: Int,
        xp: Double,
        dirs: EnumType<Int, LocType>,
        full: LocType,
        product: ObjType?,
    ) {
        creature(type, TrapKind.BOX, level, xp, HunterHunt.boxtrap, wander = 5, max = 14) {
            param[HunterParams.loc_trapping_dirs] = dirs
            param[HunterParams.loc_full] = full
            // The ferret is released rather than kept, so it has no product item.
            if (product != null) {
                param[params.skill_productitem] = product
            }
            param[HunterParams.trap_return] = HunterObjs.box_trap
        }
    }

    private fun netted(type: NpcType, level: Int, xp: Double, trapping: LocType, full: LocType) {
        creature(type, TrapKind.NET, level, xp, HunterHunt.nettrap, wander = 3, max = 10) {
            param[HunterParams.loc_trapping] = trapping
            param[HunterParams.loc_full] = full
            // A net trap is built from scenery, so nothing is handed back but the rope and net.
            param[HunterParams.trap_return] = HunterObjs.rope
            param[HunterParams.secondary_product] = HunterObjs.small_fishing_net
            param[HunterParams.secondary_count] = 1
        }
    }

    private fun kebbit(
        type: NpcType,
        level: Int,
        xp: Double,
        trapping: LocType,
        full: LocType,
        product: ObjType,
    ) {
        creature(type, TrapKind.DEADFALL, level, xp, HunterHunt.deadfall, wander = 4, max = 12) {
            param[HunterParams.loc_trapping] = trapping
            param[HunterParams.loc_full] = full
            param[params.skill_productitem] = product
            // A deadfall is rebuilt from the boulder, so only the logs come back.
            param[HunterParams.trap_return] = HunterObjs.logs
        }
    }

    private fun creature(
        type: NpcType,
        kind: Int,
        level: Int,
        xp: Double,
        hunt: HuntModeType,
        wander: Int,
        max: Int,
        extra: org.rsmod.api.type.script.dsl.NpcPluginBuilder.() -> Unit,
    ) {
        edit(type) {
            contentGroup = HunterContent.hunter_trap_creature
            param[HunterParams.trap_kind] = kind
            param[params.levelrequire] = level
            param[params.skill_xp] = PlayerStatMap.toFineXP(xp).toInt()
            param[HunterParams.rate_low] = catchWeightLow(level)
            param[HunterParams.rate_high] = catchWeightHigh(level)
            param[HunterParams.creature_respawn] = 50

            huntMode = hunt
            huntRange = 5
            wanderRange = wander
            maxRange = max
            extra()
        }
    }

    /**
     * Catch weights on the standard 0..255 scale `statRandom` interpolates over: [catchWeightLow]
     * is the weight at Hunter 1 and [catchWeightHigh] the weight at 99. Both fall off with the
     * creature's own level requirement so higher-tier quarry stays slower to catch even at 99,
     * which is what keeps the ladder feeling like OSRS rather than flattening at the top.
     */
    private fun catchWeightLow(level: Int): Int = (80 - level / 2).coerceAtLeast(6)

    private fun catchWeightHigh(level: Int): Int = (250 - level).coerceAtLeast(120)
}
