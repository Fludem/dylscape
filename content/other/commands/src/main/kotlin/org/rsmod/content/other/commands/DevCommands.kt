package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.type.symbols.name.NameMapping
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.game.type.stat.UnpackedStatType
import org.rsmod.objtx.TransactionResult
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Admin commands that exist purely to make manual testing quicker. These are deliberately kept
 * separate from [AdminCommands], which mirrors the commands available in the original game.
 */
class DevCommands
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val playerList: PlayerList,
    private val statTypes: StatTypeList,
    private val objTypes: ObjTypeList,
    private val invTypes: InvTypeList,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val heldEquip: HeldEquipOp,
    private val wornUnequip: WornUnequipOp,
    private val names: NameMapping,
) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onCommand("setlvl", "Set a single stat level", ::setLvl) {
            invalidArgs = "Use as ::setlvl statDebugNameOrId level (ex: mining 50)"
        }
        onCommand("addxp", "Grant xp in a single stat", ::addXp) {
            invalidArgs = "Use as ::addxp statDebugNameOrId xp (ex: mining 13034)"
        }
        onCommand("heal", "Restore all stats and run energy", ::heal)
        onCommand("energy", "Set run energy percent", ::energy) {
            invalidArgs = "Use as ::energy percent (ex: 100)"
        }
        onCommand("equip", "Spawn obj directly into worn", ::equip) {
            invalidArgs = "Use as ::equip objDebugNameOrId count (ex: rune_scimitar)"
        }
        onCommand("unequip", "Move all worn objs back into inv", ::unequip)
        onCommand("bankadd", "Spawn obj into bank", ::bankAdd) {
            invalidArgs = "Use as ::bankadd objDebugNameOrId count (ex: coins 1000000)"
        }
        onCommand("bankclear", "Remove all objs from bank", ::bankClear)
        onCommand("objadd", "Spawn obj on the ground", ::objAdd) {
            invalidArgs = "Use as ::objadd duration objDebugNameOrId count (ex: 100 coins 50)"
        }
        onCommand("objdel", "Remove ground objs under you", ::objDel)
        onCommand("npcdel", "Remove npcs under you", ::npcDel)
        onCommand("teleto", "Teleport to a player", ::teleTo) {
            invalidArgs = "Use as ::teleto username (ex: dylan)"
        }
        onCommand("telehere", "Teleport a player to you", ::teleHere) {
            invalidArgs = "Use as ::telehere username (ex: dylan)"
        }
        onCommand("find", "Search debug names by category", ::find) {
            invalidArgs = "Use as ::find category text (ex: obj rune_scim)"
        }
        onCommand("noclip", "Toggle click-to-teleport through walls", ::noclip)
    }

    private fun noclip(cheat: Cheat) =
        with(cheat) {
            player.noClip = !player.noClip
            if (player.noClip) {
                player.mes("Noclip ON - click any tile to teleport there. Use ::mypos to read it.")
            } else {
                player.mes("Noclip OFF.")
            }
        }

    private fun setLvl(cheat: Cheat) =
        with(cheat) {
            val (typeName, levelArg) = args.asTypeNameAndNumber(defaultNumber = 1)
            val stat = player.resolveStat(typeName) ?: return
            val level = levelArg.toInt().coerceIn(stat.minLevel, MAX_STAT_LEVEL)
            player.setStatLevel(stat, level)
            player.mes("Set '${stat.internalName}' to level $level.")
        }

    private fun addXp(cheat: Cheat) =
        with(cheat) {
            val (typeName, xpArg) = args.asTypeNameAndNumber(defaultNumber = 0)
            val stat = player.resolveStat(typeName) ?: return
            val xp = xpArg.toDouble()
            val added = player.statAdvance(stat, xp, rate = 1.0, globalRate = 1.0)
            player.mes(
                "Granted ${added.formatAmount} xp in '${stat.internalName}' " +
                    "(now level ${player.statBase(stat)})."
            )
        }

    private fun heal(cheat: Cheat) =
        with(cheat) {
            player.statRestoreAll(statTypes.values)
            player.runEnergy = constants.run_max_energy
            UpdateRun.energy(player, player.runEnergy)
            player.mes("Restored all stats and run energy.")
        }

    private fun energy(cheat: Cheat) =
        with(cheat) {
            val percent = args.getOrNull(0)?.toInt()?.coerceIn(0, 100) ?: 100
            player.runEnergy = (constants.run_max_energy * percent) / 100
            UpdateRun.energy(player, player.runEnergy)
            player.mes("Set run energy to $percent%.")
        }

    private fun equip(cheat: Cheat) =
        with(cheat) {
            val (typeName, countArg) = args.asTypeNameAndNumber(defaultNumber = 1)
            val type = player.resolveObj(typeName) ?: return
            val slot = player.inv.indexOfFirst { it == null }
            if (slot == -1) {
                player.mes("You need a free inventory slot to spawn equipment.")
                return
            }
            val count = countArg.toSaneCount()
            val spawned = player.invAdd(player.inv, type, count, slot = slot, strict = false)
            if (spawned.err is TransactionResult.RestrictedDummyitem) {
                player.mes("You can't spawn this item!")
                return
            }
            val spawnedCount = spawned.completed()
            when (val result = heldEquip.equip(player, slot, player.inv)) {
                is HeldEquipResult.Success -> {
                    player.mes("Equipped `${type.debugName}` x ${spawnedCount.formatAmount}")
                }
                is HeldEquipResult.Fail -> {
                    result.messages.forEach(player::mes)
                    player.mes("Could not equip `${type.debugName}`. It is in your inventory.")
                }
            }
        }

    private fun unequip(cheat: Cheat) =
        with(cheat) {
            var unequipped = 0
            for (wornSlot in player.worn.indices) {
                if (player.worn[wornSlot] == null || Wearpos[wornSlot] == null) {
                    continue
                }
                val result = wornUnequip.unequip(player, wornSlot, player.worn, player.inv)
                if (result is WornUnequipResult.Fail) {
                    result.message?.let(player::mes)
                    break
                }
                unequipped++
            }
            player.mes("Unequipped $unequipped obj(s).")
        }

    private fun bankAdd(cheat: Cheat) =
        with(cheat) {
            val (typeName, countArg) = args.asTypeNameAndNumber(defaultNumber = 1)
            val type = player.resolveObj(typeName) ?: return
            val count = countArg.toSaneCount()
            val bank = player.bank()
            val spawned = player.invAdd(bank, type, count, strict = false)
            if (spawned.err is TransactionResult.RestrictedDummyitem) {
                player.mes("You can't spawn this item!")
                return
            }
            player.mes("Spawned bank obj `${type.debugName}` x ${spawned.completed().formatAmount}")
        }

    private fun bankClear(cheat: Cheat) = with(cheat) { player.invClear(player.bank()) }

    private fun objAdd(cheat: Cheat) =
        with(cheat) {
            val duration = args[0].toInt()
            val (typeName, countArg) = args.drop(1).asTypeNameAndNumber(defaultNumber = 1)
            val type = player.resolveObj(typeName) ?: return
            val count = countArg.toSaneCount()
            val obj = objRepo.add(type, player.coords, duration, receiver = player, count = count)
            player.mes(
                "Spawned ground obj `${type.debugName}` x ${count.formatAmount} " +
                    "(duration: $duration cycles)"
            )
            logger.debug { "Spawned obj: obj=$obj, type=$type" }
        }

    private fun objDel(cheat: Cheat) =
        with(cheat) {
            val objs = objRepo.findAll(player.coords).toList()
            if (objs.isEmpty()) {
                player.mes("No ground obj found on ${player.coords}")
                return
            }
            for (obj in objs) {
                objRepo.del(obj, duration = Int.MAX_VALUE)
            }
            player.mes("Deleted ${objs.size} ground obj(s) on ${player.coords}")
        }

    private fun npcDel(cheat: Cheat) =
        with(cheat) {
            val npcs = npcRepo.findAll(player.coords).toList()
            if (npcs.isEmpty()) {
                player.mes("No npc found on ${player.coords}")
                return
            }
            for (npc in npcs) {
                npcRepo.del(npc, duration = Int.MAX_VALUE)
            }
            player.mes("Deleted ${npcs.size} npc(s) on ${player.coords}")
        }

    private fun teleTo(cheat: Cheat) =
        with(cheat) {
            val target = resolvePlayer(player, args.asTypeName()) ?: return
            val coords = target.coords
            val launched =
                protectedAccess.launch(player) {
                    player.mes("Teleported to ${target.displayName} ($coords).")
                    telejump(coords)
                }
            if (!launched) {
                player.mes(constants.dm_busy)
            }
        }

    private fun teleHere(cheat: Cheat) =
        with(cheat) {
            val target = resolvePlayer(player, args.asTypeName()) ?: return
            val admin = player
            val coords = admin.coords
            val launched =
                protectedAccess.launch(target) {
                    admin.mes("Teleported ${target.displayName} to $coords.")
                    target.mes("You have been teleported by ${admin.displayName}.")
                    telejump(coords)
                }
            if (!launched) {
                admin.mes("${target.displayName} is busy right now.")
            }
        }

    private fun find(cheat: Cheat) =
        with(cheat) {
            val category = args[0].lowercase()
            val mapping = categoryNames(category)
            if (mapping == null) {
                player.mes("Unknown category: '$category'")
                player.mes("Valid categories: ${CATEGORIES.joinToString(", ")}")
                return
            }
            val query = args.drop(1).asTypeName().lowercase().replace("-", "_")
            if (query.isEmpty()) {
                player.mes("Use as ::find $category text")
                return
            }
            val matches = mapping.keys.filter { query in it.lowercase() }.sorted()
            if (matches.isEmpty()) {
                val closest = findClosestNameMatch(query, mapping.keys)
                player.mes("No $category matches for '$query'.")
                closest?.let { player.mes("Did you mean: '$it' (${mapping[it]})?") }
                return
            }
            player.mes("Found ${matches.size} $category match(es) for '$query':")
            for (match in matches.take(MAX_FIND_RESULTS)) {
                player.mes("  $match (${mapping[match]})")
            }
            if (matches.size > MAX_FIND_RESULTS) {
                player.mes("  ...and ${matches.size - MAX_FIND_RESULTS} more.")
            }
        }

    private fun categoryNames(category: String): Map<String, Int>? =
        when (category) {
            "obj",
            "item" -> names.objs
            "npc" -> names.npcs
            "loc" -> names.locs
            "seq",
            "anim" -> names.seqs
            "spot" -> names.spotanims
            "stat",
            "skill" -> names.stats
            "inv" -> names.invs
            "varp" -> names.varps
            "varbit" -> names.varbits
            "interface" -> names.interfaces
            "component" -> names.components
            else -> null
        }

    private fun Player.bank(): Inventory = invMap.getOrPut(invTypes[invs.bank])

    private fun Player.resolveObj(typeName: String): UnpackedObjType? {
        val resolvedName = resolveTypeName(typeName, names.objs)
        val typeId = resolveArgTypeId(resolvedName, names.objs)
        if (typeId == null) {
            mes("There is no obj mapped to name: '$resolvedName'")
            return null
        }
        val type = objTypes[typeId]
        if (type == null) {
            mes("That obj does not exist: $typeId")
        }
        return type
    }

    private fun Player.resolveStat(typeName: String): UnpackedStatType? {
        val resolvedName = resolveTypeName(typeName, names.stats)
        val typeId = resolveArgTypeId(resolvedName, names.stats)
        if (typeId == null) {
            mes("There is no stat mapped to name: '$resolvedName'")
            return null
        }
        val type = statTypes[typeId]
        if (type == null) {
            mes("That stat does not exist: $typeId")
        }
        return type
    }

    private fun resolvePlayer(player: Player, name: String): Player? {
        val normalized = name.replace('_', ' ').trim()
        if (normalized.isEmpty()) {
            player.mes("No username given.")
            return null
        }
        val target =
            playerList.firstOrNull { it.displayName.equals(normalized, ignoreCase = true) }
                ?: playerList.firstOrNull { it.username.equals(normalized, ignoreCase = true) }
        if (target == null) {
            player.mes("No player found with name: '$normalized'")
        }
        return target
    }

    private val UnpackedObjType.debugName: String
        get() = internalName ?: name

    private fun String.toSaneCount(): Int = toLong().coerceIn(1, Int.MAX_VALUE.toLong()).toInt()

    private companion object {
        private const val MAX_STAT_LEVEL = 99
        private const val MAX_FIND_RESULTS = 20

        private val CATEGORIES =
            listOf(
                "obj",
                "npc",
                "loc",
                "seq",
                "spot",
                "stat",
                "inv",
                "varp",
                "varbit",
                "interface",
                "component",
            )
    }
}
