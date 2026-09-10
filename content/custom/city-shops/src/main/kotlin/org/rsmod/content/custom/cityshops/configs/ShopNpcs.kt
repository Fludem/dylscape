@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.cityshops.configs

import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.script.dsl.NpcPluginBuilder
import org.rsmod.game.type.npc.NpcType

internal typealias shop_npcs = ShopNpcs

/**
 * The shopkeepers we hand a stocked shop to. `Talk-to` is op1 on all of them; `Trade` is op3 on all
 * but Aleck, who carries it on op5. The slot is declared per shop in
 * [org.rsmod.content.custom.cityshops.ShopAssignment] and `ShopAssignmentsTest` checks each one
 * against the cache, because it is genuinely not uniform — Bob, for instance, carries `Repair` on
 * op4.
 *
 * [org.rsmod.content.areas.city.lumbridge.configs.LumbridgeNpcs] already owns Lumbridge's own
 * shopkeeper, assistant and Bob, so they are deliberately absent here.
 */
object ShopNpcs : NpcReferences() {
    // Al Kharid.
    val zeke = find("zeke")
    val ranael = find("ranael")
    val dommik = find("dommik")
    val louie_legs = find("louie_legs")
    val gem_trader = find("gem_trader")

    // Varrock. `aubury` and `thessalia` are nameless placeholder types with no ops at all; the
    // types that actually spawn are the variants below. Which Aubury spawns depends on Rune
    // Mysteries progress, so both are wired.
    val aubury_2op = find("aubury_2op")
    val aubury_3op = find("aubury_3op")
    val thessalia_normal = find("thessalia_normal")
    val thessalia_league = find("thessalia_league")
    val zaff = find("zaff")
    val horvik = find("horvik_the_armourer")
    val lowe = find("lowe")
    val swordshop_keeper = find("swordshop1")
    val swordshop_assistant = find("swordshop2")
    val peksa = find("peksa")

    // Champions' Guild, just south-west of Varrock.
    val valaine = find("valaine")
    val scavvo = find("scavvo")

    // Falador.
    val cassie = find("cassie")
    val flynn = find("flynn")
    val herquin = find("herquin")
    val wayne = find("wayne")

    // Port Sarim. The cache also carries a `sarim_`-prefixed set for four of these; the plain
    // types are the ones that stand in the shops.
    val wydin = find("wydin")
    val betty = find("betty")
    val gerrant = find("gerrant")
    val brian = find("brian")
    val grum = find("grum")

    // Rimmington.
    val rommik = find("rommik")

    // Dwarven Mine and Taverley.
    val nurmof = find("nurmof")
    val jatix = find("jatix")
    val gaius = find("gaius")

    // Catherby.
    val hickton = find("hickton")
    val harry = find("harry")
    val candle_maker = find("candle_maker")

    // Yanille. The cache never uses the name "Aleck" for the hunter shop owner.
    val aleck = find("hunting_shop_owner_yanille")
    val frenita = find("frenita")

    // Brimhaven.
    val davon = find("davon")

    // Edgeville. Primula is the Myths' Guild herbalist; the guild is not standable up on this
    // server and nothing spawns her there, so she is free to keep a counter in Edgeville instead.
    // Her cache ops are already the ones this module wants -- `Talk-to` on op1, `Trade` on op3 --
    // and her name is left alone, since renaming a cache type is a permanent, additive edit.
    val primula = find("myths_guild_herbalist")

    // Jack is the Myths' Guild cape seller, borrowed on the same terms as Primula above: the
    // guild is unreachable here, nothing spawns him there, and his cache ops are already
    // `Talk-to` on op1 and `Trade` on op3. He is the only npc in the cache whose whole job is
    // selling capes, which makes him the natural face for the skillcape counter.
    val jack = find("myths_guild_cape_seller")

    // General stores 2..7. Store 1 is Lumbridge's and store 8 is Zanaris', which stocks something
    // else entirely and is unreachable without Lost City — both are left alone.
    val generalshopkeeper2 = find("generalshopkeeper2")
    val generalshopkeeper3 = find("generalshopkeeper3")
    val generalshopkeeper4 = find("generalshopkeeper4")
    val generalshopkeeper5 = find("generalshopkeeper5")
    val generalshopkeeper6 = find("generalshopkeeper6")
    val generalshopkeeper7 = find("generalshopkeeper7")
    val generalassistant2 = find("generalassistant2")
    val generalassistant3 = find("generalassistant3")
    val generalassistant4 = find("generalassistant4")
    val generalassistant5 = find("generalassistant5")
    val generalassistant6 = find("generalassistant6")
    val generalassistant7 = find("generalassistant7")

    /** Stands behind a counter inside a building and must never leave it. */
    val indoors: List<NpcType> =
        listOf(
            zeke,
            ranael,
            dommik,
            louie_legs,
            aubury_2op,
            aubury_3op,
            thessalia_normal,
            thessalia_league,
            zaff,
            horvik,
            lowe,
            swordshop_keeper,
            swordshop_assistant,
            peksa,
            valaine,
            scavvo,
            cassie,
            flynn,
            herquin,
            wayne,
            wydin,
            betty,
            gerrant,
            brian,
            grum,
            rommik,
            jatix,
            gaius,
            hickton,
            harry,
            candle_maker,
            aleck,
            frenita,
            davon,
            primula,
            jack,
            generalshopkeeper2,
            generalshopkeeper3,
            generalshopkeeper4,
            generalshopkeeper5,
            generalshopkeeper6,
            generalshopkeeper7,
            generalassistant2,
            generalassistant3,
            generalassistant4,
            generalassistant5,
            generalassistant6,
            generalassistant7,
        )

    /**
     * Trades out in the open, so `indoors` would be wrong — there is no building to confine them
     * to. They still get pinned; they simply get pinned without the move restriction.
     */
    val outdoors: List<NpcType> = listOf(gem_trader, nurmof)

    val all: List<NpcType> = indoors + outdoors
}

/**
 * Shopkeepers stand behind a counter and are useless if they wander out of the building.
 *
 * `indoors` alone is not enough: the cache never writes RSMod's `wanderRange` opcode, so every npc
 * falls back to a 5-tile wander box anchored on its spawn tile, which is more than enough to walk a
 * keeper behind a wall or out of the room a player was sent to. Pinning the range to 0 is what
 * actually keeps them on their post.
 *
 * Margins are npc params, and they are per-shop in OSRS — Betty sells runes at 100% and buys at
 * 60%, while a general store sells at 130% and buys at 40%. [ShopMargins] carries the ones that
 * differ from the engine default; anything absent from that table keeps 1300/400/30.
 */
internal object ShopNpcEditor : NpcEditor() {
    init {
        for (npc in shop_npcs.indoors) {
            edit(npc) {
                moveRestrict = indoors
                wanderRange = 0
                applyMargin(npc)
            }
        }
        for (npc in shop_npcs.outdoors) {
            edit(npc) {
                wanderRange = 0
                applyMargin(npc)
            }
        }
    }

    private fun NpcPluginBuilder.applyMargin(npc: NpcType) {
        val margin = ShopMargins.byNpc[npc] ?: return
        param[ShopParams.shop_sell_percentage] = margin.sell
        param[ShopParams.shop_buy_percentage] = margin.buy
        param[ShopParams.shop_change_percentage] = margin.change
    }
}

/**
 * What a shop charges, as OSRS sets it. All three are tenths of a percent, matching the engine's
 * "multiplied by 10 for decimal precision" convention: `sell = 1000` is "sells at 100% of value",
 * `buy = 600` is "buys at 60%", and `change` is how far the price drifts per item traded.
 */
internal data class ShopMargin(val sell: Int, val buy: Int, val change: Int)

internal object ShopMargins {
    private val specialist = ShopMargin(sell = 1000, buy = 600, change = 20)

    val byNpc: Map<NpcType, ShopMargin> =
        mapOf(
            shop_npcs.zeke to specialist,
            shop_npcs.ranael to specialist,
            shop_npcs.louie_legs to specialist,
            shop_npcs.dommik to ShopMargin(1000, 650, 20),
            shop_npcs.gem_trader to ShopMargin(1000, 700, 30),
            shop_npcs.aubury_2op to ShopMargin(1000, 600, 10),
            shop_npcs.aubury_3op to ShopMargin(1000, 600, 10),
            shop_npcs.zaff to specialist,
            shop_npcs.lowe to ShopMargin(1000, 500, 10),
            shop_npcs.horvik to specialist,
            shop_npcs.swordshop_keeper to specialist,
            shop_npcs.swordshop_assistant to specialist,
            shop_npcs.peksa to ShopMargin(1000, 600, 10),
            shop_npcs.valaine to ShopMargin(1300, 400, 30),
            shop_npcs.scavvo to ShopMargin(1000, 600, 33),
            shop_npcs.cassie to specialist,
            shop_npcs.flynn to ShopMargin(1000, 600, 10),
            shop_npcs.herquin to ShopMargin(1000, 700, 30),
            shop_npcs.wayne to ShopMargin(1000, 650, 10),
            shop_npcs.wydin to ShopMargin(1000, 700, 10),
            shop_npcs.betty to ShopMargin(1000, 600, 10),
            shop_npcs.gerrant to ShopMargin(1000, 700, 10),
            shop_npcs.brian to ShopMargin(1000, 550, 10),
            shop_npcs.grum to ShopMargin(1000, 700, 20),
            shop_npcs.rommik to ShopMargin(1000, 650, 20),
            shop_npcs.nurmof to specialist,
            shop_npcs.jatix to ShopMargin(1000, 700, 30),
            shop_npcs.gaius to specialist,
            shop_npcs.hickton to ShopMargin(1000, 500, 10),
            shop_npcs.harry to ShopMargin(1000, 700, 10),
            shop_npcs.aleck to ShopMargin(1200, 700, 20),
            shop_npcs.frenita to ShopMargin(1000, 550, 10),
            shop_npcs.davon to ShopMargin(1200, 900, 20),
            // Jatix's margin: Primula's is the same kind of shop, and without an entry here she
            // would silently fall back to the general-store default of 1300/400/30.
            shop_npcs.primula to ShopMargin(1000, 700, 30),
            // Jack's skillcape counter. `change = 0` is the point of this entry: every other shop
            // here drifts its price with stock, and a cape whose price wanders off 100,000gp the
            // moment somebody buys one is exactly what a fixed-price shop must not do.
            shop_npcs.jack to ShopMargin(sell = 1000, buy = 600, change = 0),
        )
}
