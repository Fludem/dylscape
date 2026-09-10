package org.rsmod.content.skills.farming.scripts

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.farming.configs.FarmingObjs
import org.rsmod.content.skills.farming.configs.FarmingSeqs
import org.rsmod.content.skills.farming.data.CompostTier
import org.rsmod.content.skills.farming.data.FarmingCrop
import org.rsmod.content.skills.farming.data.FarmingCrops
import org.rsmod.content.skills.farming.data.FarmingPatch
import org.rsmod.content.skills.farming.data.FarmingRates
import org.rsmod.content.skills.farming.data.FarmingRegistry
import org.rsmod.content.skills.farming.data.HarvestModel
import org.rsmod.content.skills.farming.data.PatchKind
import org.rsmod.content.skills.farming.data.PatchState
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType

/**
 * Everything that actually changes a patch.
 *
 * Separate from [FarmingScript] on the same principle as `HunterTraps`: the script decides *when*
 * one of these runs, and this decides what happens. It also gives the integration tests a way in --
 * the harness can drive a loc op but has no "use this item on that loc", so planting, composting
 * and clearing are only reachable from a test through a seam like this one.
 */
@Singleton
public class FarmingPatchActions
@Inject
constructor(
    private val registry: FarmingRegistry,
    private val locTypes: LocTypeList,
    private val objTypes: ObjTypeList,
    private val random: GameRandom,
    private val xpMods: XpModifiers,
    private val perks: Perks,
) {
    private val compostTiers by lazy {
        mapOf(
            objTypes[FarmingObjs.bucket_compost].id to CompostTier.Compost,
            objTypes[FarmingObjs.bucket_supercompost].id to CompostTier.Supercompost,
            objTypes[FarmingObjs.bucket_ultracompost].id to CompostTier.Ultracompost,
        )
    }

    private val wateringCans by lazy {
        FarmingObjs.watering_cans.mapTo(HashSet()) { objTypes[it].id }
    }

    public fun state(access: ProtectedAccess, patch: FarmingPatch): PatchState =
        registry[access.player][patch]

    public fun cropOf(state: PatchState): FarmingCrop? =
        if (state.empty) null else FarmingCrops.forSeed(state.seedId)

    public suspend fun ProtectedAccess.rake(patch: FarmingPatch, state: PatchState, now: Long) {
        if (!state.empty) {
            mes("The patch is already planted.")
            return
        }
        if (state.displayState(patch.kind, null, now) == patch.kind.cleared) {
            mes("The ${patch.kind.label} is already clear of weeds.")
            return
        }
        if (invTotal(inv, FarmingObjs.rake) < 1) {
            mes("You need a rake to clear this patch.")
            return
        }
        while (!state.rake(patch.kind, System.currentTimeMillis())) {
            anim(FarmingSeqs.raking)
            statAdvance(stats.farming, RAKE_XP * xpMods.get(player, stats.farming))
            invAdd(inv, FarmingObjs.weeds, strict = false)
            sync()
            delay(3)
        }
        anim(FarmingSeqs.raking)
        statAdvance(stats.farming, RAKE_XP * xpMods.get(player, stats.farming))
        invAdd(inv, FarmingObjs.weeds, strict = false)
        sync()
        mes("The ${patch.kind.label} is now clear of weeds.")
    }

    public suspend fun ProtectedAccess.plant(
        patch: FarmingPatch,
        state: PatchState,
        seed: UnpackedObjType,
        now: Long,
    ) {
        val crop = FarmingCrops.forSeed(seed.id)
        if (crop == null) {
            mes("You can't plant that here.")
            return
        }
        if (crop.kind != patch.kind) {
            mes("${seed.name} needs a ${crop.kind.label}, not a ${patch.kind.label}.")
            return
        }
        if (!state.empty) {
            mes("The ${patch.kind.label} is already planted.")
            return
        }
        if (state.displayState(patch.kind, null, now) != patch.kind.cleared) {
            mes("The ${patch.kind.label} needs weeding first.")
            return
        }
        if (player.farmingLvl < crop.level) {
            mes("You need a Farming level of ${crop.level} to plant that.")
            return
        }
        val tool = plantingTool(crop)
        if (tool != null && invTotal(inv, tool) < 1) {
            mes("You need ${objTypes[tool].name.lowercase()} to plant that.")
            return
        }
        val seedSaved = perks.has(player, Perk.SeedSaver) && random.of(100) < SEED_SAVE_PERCENT
        if (!seedSaved && !invDel(inv, seed, count = 1).success) {
            return
        }
        anim(if (tool == FarmingObjs.dibber) FarmingSeqs.dibbing else FarmingSeqs.dig)
        // Half grown means planted half a growth cycle ago: every stage timer runs off `plantedAt`.
        val headStart =
            if (perks.has(player, Perk.HalfGrownCrops)) {
                crop.growthStages * FarmingRates.stageMillis(crop.cycleMinutes) / 2
            } else {
                0L
            }
        state.plant(seed.id, System.currentTimeMillis() - headStart)
        if (seedSaved) {
            spam("Your seed is not used up.")
        }
        statAdvance(stats.farming, crop.plantXp * xpMods.get(player, stats.farming))
        sync()
        delay(2)
        mes("You plant ${seed.name.lowercase()} in the ${patch.kind.label}.")
    }

    /**
     * Picking, harvesting and taking fruit, which the cache spells a dozen different ways
     * (`Pick-from`, `Pick-fruit`, `Pick-spine`, `Pick-dragonfruit`, `Harvest`) and which all mean
     * the same thing here.
     */
    public suspend fun ProtectedAccess.harvest(
        patch: FarmingPatch,
        state: PatchState,
        crop: FarmingCrop?,
        now: Long,
    ) {
        if (crop?.produce == null) {
            mes("There's nothing to take from this ${patch.kind.label}.")
            return
        }
        if (!state.fullyGrown(crop, now)) {
            mes("The crop isn't ready yet.")
            return
        }
        if (crop.checkState != -1 && !state.checked) {
            mes("You should check the health of this ${patch.kind.label} first.")
            return
        }
        if (inv.freeSpace() == 0) {
            mes("Your inventory is too full to hold any more.")
            return
        }
        anim(pickAnim(crop))
        delay(2)

        invAdd(inv, crop.produce)
        statAdvance(stats.farming, crop.harvestXp * xpMods.get(player, stats.farming))
        spam("You pick ${objTypes[crop.produce].name.lowercase()}.")

        when (crop.model) {
            HarvestModel.Lives -> {
                val saveChance =
                    if (perks.has(player, Perk.HarvestSaver)) {
                        HARVEST_SAVER_CHANCE
                    } else {
                        FarmingRates.harvestSaveChance(player.farmingLvl)
                    }
                val saved = random.randomDouble() < saveChance
                if (!saved) {
                    state.livesUsed++
                }
                if (state.livesLeft(crop) <= 0) {
                    state.clear(System.currentTimeMillis())
                    mes("You have harvested the last of this crop.")
                }
            }
            HarvestModel.Counted -> {
                state.takeProduce(crop, System.currentTimeMillis())
                if (
                    crop.regrowMinutes <= 0 &&
                        state.produceLeft(crop, System.currentTimeMillis()) == 0
                ) {
                    state.clear(System.currentTimeMillis())
                    mes("You have taken the last of this crop.")
                }
            }
            HarvestModel.Checked -> Unit
        }
        sync()
    }

    /**
     * The tree-and-bush payout: one big lump of xp for confirming the plant is healthy, which is
     * also what unlocks its produce.
     */
    public suspend fun ProtectedAccess.checkHealth(
        patch: FarmingPatch,
        state: PatchState,
        crop: FarmingCrop?,
        now: Long,
    ) {
        if (crop == null || !state.fullyGrown(crop, now) || state.checked) {
            mes("There is nothing to check here.")
            return
        }
        state.checked = true
        // Produce starts filling the moment the plant is signed off, not when it finished growing.
        state.regrewAt = System.currentTimeMillis()
        statAdvance(stats.farming, crop.checkXp * xpMods.get(player, stats.farming))
        sync()
        mes("You examine the ${patch.kind.label}: the ${cropLabel(crop)} is in perfect health.")
    }

    public suspend fun ProtectedAccess.chopDown(
        patch: FarmingPatch,
        state: PatchState,
        crop: FarmingCrop?,
        now: Long,
    ) {
        if (crop == null || !state.fullyGrown(crop, now)) {
            mes("There is nothing to chop down.")
            return
        }
        if (crop.stumpState == -1) {
            clear(patch, state, crop, now)
            return
        }
        anim(FarmingSeqs.dig)
        delay(2)
        state.felled = true
        sync()
        mes("You chop the ${cropLabel(crop)} down, leaving a stump.")
    }

    /**
     * Digging a patch out: the stump after a felling, a finished plant, or a crop being scrapped.
     */
    public suspend fun ProtectedAccess.clear(
        patch: FarmingPatch,
        state: PatchState,
        crop: FarmingCrop?,
        now: Long,
    ) {
        if (state.empty) {
            mes("There is nothing to clear from this ${patch.kind.label}.")
            return
        }
        if (invTotal(inv, FarmingObjs.spade) < 1) {
            mes("You need a spade to clear this patch.")
            return
        }
        anim(FarmingSeqs.dig)
        delay(2)
        state.clear(System.currentTimeMillis())
        sync()
        mes("You clear the ${patch.kind.label}.")
    }

    public suspend fun ProtectedAccess.applyCompost(
        patch: FarmingPatch,
        state: PatchState,
        bucket: UnpackedObjType,
        now: Long,
    ) {
        if (!state.empty) {
            mes("The ${patch.kind.label} has already been planted.")
            return
        }
        if (state.displayState(patch.kind, null, now) != patch.kind.cleared) {
            mes("The ${patch.kind.label} needs weeding first.")
            return
        }
        val tier = compostTiers.getValue(bucket.id)
        if (state.compost.ordinal >= tier.ordinal) {
            mes("This patch is already treated with something at least as good.")
            return
        }
        if (!invDel(inv, bucket, count = 1).success) {
            return
        }
        invAdd(inv, FarmingObjs.bucket_empty, strict = false)
        state.compost = tier
        delay(1)
        mes("You treat the ${patch.kind.label} with ${bucket.name.lowercase()}.")
    }

    /**
     * Watering is kept because players reach for it, but it does nothing: in the real game its only
     * job is holding disease off, and there is no disease here.
     */
    public suspend fun ProtectedAccess.water(
        patch: FarmingPatch,
        state: PatchState,
        crop: FarmingCrop?,
        now: Long,
    ) {
        anim(FarmingSeqs.watering)
        delay(1)
        if (state.empty) {
            mes("There is nothing planted here to water.")
        } else {
            mes("You water the ${patch.kind.label}. Crops here grow fine without it.")
        }
    }

    /**
     * Scooping soil out of a patch to fill plant pots, which is the first step of every sapling.
     * Filling all of them at once rather than one per click keeps a tree run from being twenty
     * separate right-clicks.
     */
    public suspend fun ProtectedAccess.fillPlantPots() {
        if (invTotal(inv, FarmingObjs.gardening_trowel) < 1) {
            mes("You need a gardening trowel to dig soil out.")
            return
        }
        val pots = invTotal(inv, FarmingObjs.plantpot_empty)
        if (pots <= 0) {
            return
        }
        anim(FarmingSeqs.fill_plantpot)
        delay(2)
        if (!invDel(inv, FarmingObjs.plantpot_empty, count = pots).success) {
            return
        }
        invAdd(inv, FarmingObjs.plantpot_compost, count = pots)
        mes("You fill the plant pot${if (pots == 1) "" else "s"} with soil.")
    }

    public fun ProtectedAccess.inspect(
        patch: FarmingPatch,
        state: PatchState,
        crop: FarmingCrop?,
        now: Long,
    ) {
        if (crop == null) {
            val weedy = state.displayState(patch.kind, null, now) != patch.kind.cleared
            val compost =
                if (state.compost == CompostTier.None) ""
                else " It is treated with ${state.compost.name.lowercase()}."
            mes(
                if (weedy) "This ${patch.kind.label} is full of weeds.$compost"
                else "This ${patch.kind.label} is empty and ready for a seed.$compost"
            )
            return
        }
        val grown = state.stagesGrown(crop, now)
        if (grown < crop.growthStages) {
            val remaining = crop.growthStages - grown
            val minutes =
                (remaining * FarmingRates.stageMillis(crop.cycleMinutes) / 60_000L).coerceAtLeast(1)
            mes(
                "The ${cropLabel(crop)} is at stage $grown of ${crop.growthStages}, " +
                    "roughly $minutes minute${if (minutes == 1L) "" else "s"} from ready."
            )
            return
        }
        if (crop.checkState != -1 && !state.checked) {
            mes("The ${cropLabel(crop)} is fully grown and ready to have its health checked.")
            return
        }
        when (crop.model) {
            HarvestModel.Lives ->
                mes(
                    "The ${cropLabel(crop)} is ready to harvest, with ${state.livesLeft(crop)} lives left."
                )
            HarvestModel.Counted -> {
                val left = state.produceLeft(crop, now)
                mes("The ${cropLabel(crop)} is carrying $left of ${crop.maxProduce}.")
            }
            HarvestModel.Checked -> mes("The ${cropLabel(crop)} is fully grown.")
        }
    }

    /* Helpers */

    /**
     * The op text on the face the player is currently being shown.
     *
     * A handler bound to the parent loc is handed the parent's own type, which carries no ops at
     * all, so the visible child has to be resolved from the state we just computed. That is the
     * same lookup the client did when it drew the menu.
     */
    public fun visibleOp(patch: FarmingPatch, state: Int, opIndex: Int): String? {
        val multiLoc = locTypes[patch.primary].multiLoc
        if (state !in multiLoc.indices) {
            return null
        }
        val childId = multiLoc[state].toInt() and 0xFFFF
        val child = locTypes[childId] ?: return null
        return child.op.getOrNull(opIndex)
    }

    private fun cropLabel(crop: FarmingCrop): String =
        objTypes[crop.seed].name.lowercase().removeSuffix(" seed").removeSuffix(" sapling")

    /** Seeds go in with a dibber; saplings and spores go in with a spade. */
    private fun plantingTool(crop: FarmingCrop) =
        when (crop.kind) {
            PatchKind.Tree,
            PatchKind.FruitTree,
            PatchKind.Hardwood,
            PatchKind.Calquat,
            PatchKind.SpiritTree,
            PatchKind.Celastrus,
            PatchKind.Redwood,
            PatchKind.Crystal -> FarmingObjs.spade
            PatchKind.Mushroom,
            PatchKind.Hespori,
            PatchKind.Anima -> null
            else -> FarmingObjs.dibber
        }

    private fun pickAnim(crop: FarmingCrop): SeqType =
        when (crop.kind) {
            PatchKind.FruitTree,
            PatchKind.Calquat,
            PatchKind.Celastrus,
            PatchKind.Grape -> FarmingSeqs.picking_high
            PatchKind.Bush,
            PatchKind.Cactus -> FarmingSeqs.picking_mid
            PatchKind.Mushroom -> FarmingSeqs.pick_mushroom
            else -> FarmingSeqs.picking_low
        }

    /** Pushes the patches around the player back to the client after anything changes one. */
    private fun ProtectedAccess.sync() {
        FarmingSyncScript.transmit(player, registry)
    }

    /** Whether an obj is a bucket of compost, and which tier it is worth. */
    public fun isCompost(obj: UnpackedObjType): Boolean = obj.id in compostTiers

    public fun isWateringCan(obj: UnpackedObjType): Boolean = obj.id in wateringCans

    private companion object {
        /** Every rake pulls one clump of weeds and pays the same trickle of xp. */
        const val RAKE_XP: Double = 4.0

        /** [Perk.SeedSaver]'s chance, out of 100, to keep the seed. */
        const val SEED_SAVE_PERCENT = 75

        /** [Perk.HarvestSaver]'s chance to keep a patch's life on each harvest. */
        const val HARVEST_SAVER_CHANCE = 0.80
    }
}
