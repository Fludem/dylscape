package org.rsmod.content.skills.mining.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.locParam
import org.rsmod.api.config.locXpParam
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.mining.configs.MiningContent
import org.rsmod.content.skills.mining.configs.MiningParams
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.enums.find
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Mining, built on the same skilling loop upstream uses for woodcutting.
 *
 * The shape is deliberately the same as `Woodcutting` so the two stay comparable, but mining is
 * simpler in one respect: an ore rock always depletes on a successful swing and comes back on a
 * fixed timer, so none of woodcutting's "tree stays up while people keep chopping" controller
 * machinery is needed here.
 *
 * Rocks carry `Mine` on **op1** (decoded from the cache, not assumed), so unlike trees there is no
 * second op to drive the repeat — the loop re-queues `opLoc1` against the same rock.
 *
 * TODO:
 * - Gem rocks, which roll a weighted gem table rather than one fixed product.
 * - Mining guild (+7 invisible levels) and the Varrock armour ore-doubling effect.
 */
class Mining
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val enumTypes: EnumTypeList,
    private val locRepo: LocRepository,
    private val playerRepo: PlayerRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(MiningContent.mining_rock) { mine(it.loc, it.type) }
        onOpLocU(MiningContent.mining_rock, MiningContent.mining_pickaxe) { mine(it.loc, it.type) }
    }

    private fun ProtectedAccess.mine(rock: BoundLocInfo, type: UnpackedLocType) {
        val pickaxe = findPickaxe(player, objTypes)
        if (pickaxe == null) {
            mes("You need a pickaxe to mine this rock.")
            mes("You do not have a pickaxe which you have the Mining level to use.")
            return
        }

        if (player.miningLvl < type.rockLevelReq) {
            mes("You need a Mining level of ${type.rockLevelReq} to mine this rock.")
            return
        }

        if (inv.isFull()) {
            val product = objTypes[type.rockOre]
            mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            soundSynth(synths.pillory_wrong)
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(objTypes[pickaxe].pickaxeMiningAnim)
        }

        var minedOre = false
        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
            spam("You swing your pickaxe at the rock.")
        } else if (actionDelay == mapClock) {
            val (low, high) = mineSuccessRates(type, pickaxe, enumTypes)
            minedOre = statRandom(stats.mining, low, high, invisibleLvls)
        }

        if (!minedOre) {
            opLoc1(rock)
            return
        }

        val product = objTypes[type.rockOre]
        val xp = type.rockXp * xpMods.get(player, stats.mining)
        spam("You manage to mine some ${product.name.lowercase()}.")
        statAdvance(stats.mining, xp)
        invAdd(inv, product)
        publish(MinedOre(player, rock, product))

        val respawnTime = type.rockRespawnTime
        locRepo.change(rock, type.rockSpent, respawnTime)
        resetAnim()
        sendLocalOverlayLoc(rock, type, respawnTime)
    }

    /**
     * Mirrors woodcutting's respawn overlay so a depleted rock shows its timer to everyone standing
     * at the same rock face, not just whoever landed the last swing.
     */
    private fun sendLocalOverlayLoc(rock: BoundLocInfo, type: UnpackedLocType, respawnTime: Int) {
        val players = playerRepo.findAll(ZoneKey.from(rock.coords), zoneRadius = 3)
        for (player in players) {
            ClientScripts.addOverlayTimerLoc(
                player = player,
                coords = rock.coords,
                loc = type,
                shape = rock.shape,
                timer = Constants.overlay_timer_woodcutting,
                ticks = respawnTime,
                colour = 16765184,
            )
        }
    }

    data class MinedOre(val player: Player, val rock: BoundLocInfo, val product: ObjType) :
        UnboundEvent

    companion object {
        val UnpackedObjType.pickaxeMiningReq: Int by objParam(params.levelrequire)
        val UnpackedObjType.pickaxeMiningAnim: SeqType by objParam(params.skill_anim)

        val UnpackedLocType.rockLevelReq: Int by locParam(params.levelrequire)
        val UnpackedLocType.rockOre: ObjType by locParam(params.skill_productitem)
        val UnpackedLocType.rockXp: Double by locXpParam(params.skill_xp)
        val UnpackedLocType.rockSpent: LocType by locParam(params.next_loc_stage)
        val UnpackedLocType.rockRespawnTime: Int by locParam(params.respawn_time)

        /**
         * Picks the best pickaxe the player can actually swing, checking worn before carried so a
         * wielded dragon pickaxe beats a bronze in the bag. Matches how woodcutting resolves axes.
         */
        fun findPickaxe(player: Player, objTypes: ObjTypeList): InvObj? {
            val worn = player.wornPickaxe(objTypes)
            val carried = player.carriedPickaxe(objTypes)
            if (worn != null && carried != null) {
                if (objTypes[worn].pickaxeMiningReq >= objTypes[carried].pickaxeMiningReq) {
                    return worn
                }
                return carried
            }
            return worn ?: carried
        }

        private fun Player.wornPickaxe(objTypes: ObjTypeList): InvObj? {
            val righthand = righthand ?: return null
            return righthand.takeIf { objTypes[it].isUsablePickaxe(miningLvl) }
        }

        private fun Player.carriedPickaxe(objTypes: ObjTypeList): InvObj? {
            return inv.filterNotNull { objTypes[it].isUsablePickaxe(miningLvl) }
                .maxByOrNull { objTypes[it].pickaxeMiningReq }
        }

        private fun UnpackedObjType.isUsablePickaxe(miningLevel: Int): Boolean =
            isContentType(MiningContent.mining_pickaxe) && miningLevel >= pickaxeMiningReq

        fun mineSuccessRates(
            rockType: UnpackedLocType,
            pickaxe: InvObj,
            enumTypes: EnumTypeList,
        ): Pair<Int, Int> {
            val pickaxes = rockType.param(MiningParams.success_rates)
            val rates = enumTypes[pickaxes].find(pickaxe)
            val low = rates shr 16
            val high = rates and 0xFFFF
            return low to high
        }
    }
}
