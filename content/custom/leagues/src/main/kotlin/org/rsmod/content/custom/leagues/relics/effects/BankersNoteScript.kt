package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Banker's Note relic: use any item on the note to note or un-note every copy carried, the way
 * a bank's note toggle would.
 *
 * Op1 Activate repeats that on the last item used. Op2 Note-Quantity and op3 Un-note-Quantity set
 * how many convert at once (all, by default). Op4 Toggle has nothing to toggle yet. The last item
 * and the quantities are held for the session.
 */
class BankersNoteScript @Inject constructor(private val objTypes: ObjTypeList) : PluginScript() {
    private val lastItem = HashMap<Player, Int>()
    private val noteQuantity = HashMap<Player, Int>()
    private val unnoteQuantity = HashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpHeldU(league_objs.bankers_note) { use(it.second) }
        onOpHeld1(league_objs.bankers_note) { activate() }
        onOpHeld2(league_objs.bankers_note) {
            setQuantity(noteQuantity, "How many would you like to note at once?")
        }
        onOpHeld3(league_objs.bankers_note) {
            setQuantity(unnoteQuantity, "How many would you like to un-note at once?")
        }
        onOpHeld4(league_objs.bankers_note) { mes("There is nothing to toggle on the note yet.") }
        onEvent<SessionStateEvent.Delete> {
            lastItem.remove(player)
            noteQuantity.remove(player)
            unnoteQuantity.remove(player)
        }
    }

    private fun ProtectedAccess.use(type: UnpackedObjType) {
        if (!player.hasRelic(Relic.BankersNote)) {
            mes(NOT_YOURS)
            return
        }
        val base = if (type.isCert) objTypes.uncert(type) else type
        lastItem[player] = base.id
        convert(type)
    }

    private fun ProtectedAccess.activate() {
        if (!player.hasRelic(Relic.BankersNote)) {
            mes(NOT_YOURS)
            return
        }
        val id = lastItem[player]
        if (id == null) {
            mes("Use an item on the note first, and Activate will repeat it.")
            return
        }
        val base = objTypes.types.getValue(id)
        val cert = if (base.canCert) objTypes.cert(base) else null
        when {
            invTotal(inv, base) > 0 -> convert(base)
            cert != null && invTotal(inv, cert) > 0 -> convert(cert)
            else -> mes("You are not carrying any ${base.name.lowercase()}.")
        }
    }

    private fun ProtectedAccess.convert(type: UnpackedObjType) {
        when {
            type.isCert -> unnote(type)
            type.canCert -> note(type)
            else -> mes("The note cannot hold that.")
        }
    }

    private fun ProtectedAccess.note(type: UnpackedObjType) {
        val count = minOf(invTotal(inv, type), noteQuantity[player] ?: Int.MAX_VALUE)
        if (count <= 0) {
            return
        }
        val cert = objTypes.cert(type)
        invDel(inv, type, count = count)
        invAdd(inv, cert, count = count)
    }

    private fun ProtectedAccess.unnote(type: UnpackedObjType) {
        val carried = invTotal(inv, type)
        val wanted = minOf(carried, unnoteQuantity[player] ?: Int.MAX_VALUE)
        // Un-noting the whole stack frees its slot for one more item.
        val room = inv.freeSpace() + if (wanted >= carried) 1 else 0
        val count = minOf(wanted, room)
        if (count <= 0) {
            mes("You don't have enough inventory space to un-note that.")
            return
        }
        val item = objTypes.uncert(type)
        invDel(inv, type, count = count)
        invAdd(inv, item, count = count)
    }

    private suspend fun ProtectedAccess.setQuantity(into: HashMap<Player, Int>, title: String) {
        val amount = countDialog(title)
        if (amount <= 0) {
            into.remove(player)
            mes("The note will convert everything you carry.")
            return
        }
        into[player] = amount
        mes("The note will convert up to $amount at once.")
    }

    private companion object {
        const val NOT_YOURS = "The note is blank to you."
    }
}
