package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.aliases.ParamInt
import org.rsmod.api.config.aliases.ParamObj
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.content.skills.magic.commons.configs.magic_enums
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.isType
import org.rsmod.map.CoordGrid

/**
 * Every spell the cache lists, grouped by book, built from the same obj params upstream's
 * `MagicSpellRegistry` reads at boot.
 *
 * It is a second reader rather than a wrapper around the registry for two reasons. The registry
 * only answers "give me the spell for this obj" and cannot list a book, and it is populated by a
 * Guice provider in `MagicSpellModule` that the test harness never installs, so anything that
 * injected it would be untestable. The seven jewellery enchantments are not in any book enum at all
 * (they live in the spellbook's sub-list), which is why [spellFor] builds from params on demand.
 * The two upstream quirks are kept: Magic Dart validates its staff last, and Claws of Guthix lists
 * a display-only staff that is swapped for the real one.
 */
@Singleton
public class MagicSpellbooks
@Inject
constructor(private val objTypes: ObjTypeList, private val enumResolver: EnumTypeMapResolver) {
    private val byBook: Map<Spellbook, List<MagicSpell>> by lazy { load() }
    private val byObj = HashMap<Int, MagicSpell>()

    public val all: List<MagicSpell>
        get() = byBook.values.flatten()

    public fun byBook(book: Spellbook): List<MagicSpell> = byBook[book].orEmpty()

    /** Spells typed `Teleport` that carry a destination; the home and group teleports do not. */
    public fun teleports(): List<MagicSpell> =
        all.filter { it.type == MagicSpellType.Teleport && telecoord(it) != null }

    public fun telecoord(spell: MagicSpell): CoordGrid? =
        objTypes[spell.obj].paramMap?.getOrNull(params.spell_telecoord)

    /** The spell for [obj], whether or not a spellbook enum lists it. */
    public fun spellFor(obj: ObjType): MagicSpell =
        byObj.getOrPut(obj.id) { objTypes[obj].toMagicSpell() }

    private fun load(): Map<Spellbook, List<MagicSpell>> {
        val books = enumResolver[magic_enums.spellbooks].filterValuesNotNull()
        val result = HashMap<Spellbook, MutableList<MagicSpell>>()
        for ((bookId, bookEnum) in books) {
            val book = Spellbook[bookId] ?: continue
            val list = result.getOrPut(book) { ArrayList() }
            for (obj in enumResolver[bookEnum].filterValuesNotNull().values) {
                val spell = spellFor(obj)
                // The bounty-target teleport is listed in every book with no book of its own.
                if (spell.spellbook != book) continue
                list += spell
            }
        }
        return result
    }

    private fun org.rsmod.game.type.obj.UnpackedObjType.toMagicSpell(): MagicSpell {
        val castXp = paramMap?.getOrNull(params.spell_castxp)
        checkNotNull(castXp) { "Cast xp not defined for spell obj: $internalName ($id)" }
        val reqs = buildList {
            fun add(objParam: ParamObj, countParam: ParamInt) {
                val rune = paramMap?.getOrNull(objParam)?.usableRequirement() ?: return
                val worn = objTypes[rune].wearpos1.takeIf { it != -1 }
                this += MagicSpell.ObjRequirement(rune, param(countParam), worn)
            }
            add(params.spell_runetype_1, params.spell_runecount_1)
            add(params.spell_runetype_2, params.spell_runecount_2)
            add(params.spell_runetype_3, params.spell_runecount_3)
            add(params.spell_runetype_4, params.spell_runecount_4)
        }
        val staffLast = isType(objs.spell_magic_dart) && reqs.isNotEmpty()
        return MagicSpell(
            obj = this,
            name = param(params.spell_name),
            component = param(params.spell_button),
            spellbook = Spellbook[param(params.spell_spellbook)],
            type = MagicSpellType[param(params.spell_type)] ?: MagicSpellType.Other,
            maxHit = param(params.spell_maxhit),
            levelReq = param(params.spell_levelreq),
            castXp = castXp / 10.0,
            objReqs = if (staffLast) reqs.drop(1) + reqs.first() else reqs,
        )
    }

    private fun ObjType.usableRequirement(): ObjType =
        if (isType(objs.guthix_staff_rune)) objs.guthix_staff else this
}
