package org.rsmod.content.skills.thieving.configs

import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

/**
 * The `pickpocket_coin_pouch_*` family, objs 22521-22538 plus the two later additions.
 *
 * These are vanilla objects, not something this server invents: every one is stackable, carries
 * `Open-all` and `Open` on its **inventory** ops (iop 1 and 2, not the ground ops), and the cache
 * enricher already gives them the destroy note "You may simply open the pouch instead to continue
 * pickpocketing". That is exactly the mechanic this skill is built around — pouches stack, so the
 * inventory never fills and a pickpocketing session runs until the player stops it.
 *
 * All twenty are declared and tagged even though only nine currently have a target on the ladder.
 * Tagging the whole family costs nothing and means a future tier works the moment its npc is added.
 */
object ThievingObjs : ObjReferences() {
    val pouch_citizen = find("pickpocket_coin_pouch_citizen")
    val pouch_farmer = find("pickpocket_coin_pouch_farmer")
    val pouch_ham = find("pickpocket_coin_pouch_ham")
    val pouch_warrior = find("pickpocket_coin_pouch_warrior")
    val pouch_rogue = find("pickpocket_coin_pouch_rogue")
    val pouch_cavegoblin = find("pickpocket_coin_pouch_cavegoblin")
    val pouch_guard = find("pickpocket_coin_pouch_guard")
    val pouch_fremennik = find("pickpocket_coin_pouch_fremennik")
    val pouch_bandit2 = find("pickpocket_coin_pouch_bandit2")
    val pouch_desertbandit = find("pickpocket_coin_pouch_desertbandit")
    val pouch_knight = find("pickpocket_coin_pouch_knight")
    val pouch_bandit = find("pickpocket_coin_pouch_bandit")
    val pouch_watchman = find("pickpocket_coin_pouch_watchman")
    val pouch_menaphite = find("pickpocket_coin_pouch_menaphite")
    val pouch_paladin = find("pickpocket_coin_pouch_paladin")
    val pouch_gnome = find("pickpocket_coin_pouch_gnome")
    val pouch_hero = find("pickpocket_coin_pouch_hero")
    val pouch_elf = find("pickpocket_coin_pouch_elf")
    val pouch_vyre = find("pickpocket_coin_pouch_vyre")
    val pouch_varlamore = find("pickpocket_coin_pouch_varlamore_wealthy")

    val coins = find("coins")

    /**
     * Pickpocket loot beyond the purse. Names are the cache's own, which is why the runes read
     * `airrune` rather than `air_rune` and the wine is a `jug_wine`.
     *
     * Unlike the pouches these are mostly **not** stackable, and that is the point of the
     * inventory-space rule in `Pickpocketing.hasRoomFor`: a rung that can pay one of these needs a
     * free slot, so its session ends when the bag fills rather than dropping the roll on the floor.
     */
    val airrune = find("airrune")
    val arrow_shaft = find("arrow_shaft")
    val bloodrune = find("bloodrune")
    val chaosrune = find("chaosrune")
    val deathrune = find("deathrune")
    val diamond = find("diamond")
    val earthrune = find("earthrune")
    val fire_orb = find("fire_orb")
    val gold_ore = find("gold_ore")
    val iron_dagger_p = find("iron_dagger_p")
    val jug_wine = find("jug_wine")
    val king_worm = find("king_worm")
    val lockpick = find("lockpick")
    val swamp_toad = find("swamp_toad")

    /** Stall loot. Every one of these has a cert link, which is what lets stalls pay out noted. */
    val cake = find("cake")
    val bread = find("bread")
    val chocolate_slice = find("chocolate_slice")
    val cup_of_tea = find("cup_of_tea")
    val silk = find("silk")
    val grey_wolf_fur = find("grey_wolf_fur")
    val silver_ore = find("silver_ore")
    val uncut_sapphire = find("uncut_sapphire")
    val uncut_emerald = find("uncut_emerald")
    val uncut_ruby = find("uncut_ruby")
    val uncut_diamond = find("uncut_diamond")
    val spice = find("spicespot")

    val allPouches: List<ObjType> =
        listOf(
            pouch_citizen,
            pouch_farmer,
            pouch_ham,
            pouch_warrior,
            pouch_rogue,
            pouch_cavegoblin,
            pouch_guard,
            pouch_fremennik,
            pouch_bandit2,
            pouch_desertbandit,
            pouch_knight,
            pouch_bandit,
            pouch_watchman,
            pouch_menaphite,
            pouch_paladin,
            pouch_gnome,
            pouch_hero,
            pouch_elf,
            pouch_vyre,
            pouch_varlamore,
        )
}

/**
 * Puts every pouch into [ThievingContent.coin_pouch] so one pair of `onOpHeld` handlers covers the
 * whole family.
 *
 * Safe to do, unlike the npc side: the pouches carry no content group of their own, and the
 * enricher only sets their destroy note and untradable flag, both of which survive the merge
 * because this edit leaves every other field null.
 */
internal object ThievingPouchEditor : ObjEditor() {
    init {
        for (pouch in ThievingObjs.allPouches) {
            edit(pouch) { contentGroup = ThievingContent.coin_pouch }
        }
    }
}
