package org.rsmod.content.custom.cityshops

import org.rsmod.content.custom.cityshops.configs.shop_invs
import org.rsmod.content.custom.cityshops.configs.shop_npcs
import org.rsmod.content.custom.cityshops.towns.AsgarnianShops
import org.rsmod.content.custom.cityshops.towns.FaladorShops
import org.rsmod.content.custom.cityshops.towns.KandarinShops
import org.rsmod.content.custom.cityshops.towns.KaramjaShops
import org.rsmod.content.custom.cityshops.towns.PortSarimShops
import org.rsmod.content.custom.cityshops.towns.RimmingtonShops
import org.rsmod.content.custom.cityshops.towns.VarrockExtraShops
import org.rsmod.game.type.inv.InvType
import org.rsmod.game.type.npc.NpcType

/**
 * One shop, and every npc that opens it.
 *
 * @param greeting what the shopkeeper opens with on `Talk-to`. The player's half of the exchange is
 *   fixed, so this is the only line that needs to differ per shop.
 * @param tradeOp which op slot opens the shop. Almost every shopkeeper carries `Trade` on op3, but
 *   not all of them -- Aleck in Yanille has it on op5 -- and binding the wrong slot gives a
 *   shopkeeper you can walk up to and not trade with.
 */
data class ShopAssignment(
    val title: String,
    val inv: InvType,
    val greeting: String,
    val npcs: List<NpcType>,
    val tradeOp: Int = 3,
)

internal fun shop(
    title: String,
    inv: InvType,
    greeting: String,
    vararg npcs: NpcType,
    tradeOp: Int = 3,
): ShopAssignment = ShopAssignment(title, inv, greeting, npcs.toList(), tradeOp)

/**
 * Every shop we stock, outside of Lumbridge's own (which upstream already owns).
 *
 * Al Kharid and Varrock live here because they came first; everything since is grouped by region in
 * [org.rsmod.content.custom.cityshops.towns]. A shop is only reachable if
 * [org.rsmod.content.custom.cityshops.map.CityShopNpcSpawns] also places its npc — `ShopSpawnTest`
 * is what holds those two halves together.
 */
object ShopAssignments {
    private val alKharidAndVarrock: List<ShopAssignment> =
        listOf(
            // Al Kharid.
            shop(
                title = "Zeke's Superior Scimitars",
                inv = shop_invs.scimitarshop,
                greeting = "Would you like to buy a scimitar?",
                shop_npcs.zeke,
            ),
            shop(
                title = "Ranael's Super Skirt Store",
                inv = shop_invs.skirtshop,
                greeting = "Would you like to buy a skirt?",
                shop_npcs.ranael,
            ),
            shop(
                title = "Louie Legs' Leg Wear",
                inv = shop_invs.legsshop,
                greeting = "Would you like to buy some armoured legs?",
                shop_npcs.louie_legs,
            ),
            shop(
                title = "Dommik's Crafting Store",
                inv = shop_invs.craftingshop,
                greeting = "Do you want to buy any crafting equipment?",
                shop_npcs.dommik,
            ),
            shop(
                title = "Gem trader",
                inv = shop_invs.gemshop,
                greeting = "Do you want to buy any gems?",
                shop_npcs.gem_trader,
            ),

            // Varrock.
            shop(
                title = "Aubury's Rune Shop",
                inv = shop_invs.runeshop,
                greeting = "Do you want to buy some magical runes?",
                shop_npcs.aubury_2op,
                shop_npcs.aubury_3op,
            ),
            shop(
                title = "Zaff's Superior Staffs",
                inv = shop_invs.staffshop,
                greeting = "Would you like to buy a staff?",
                shop_npcs.zaff,
            ),
            shop(
                title = "Lowe's Archery Emporium",
                inv = shop_invs.archeryshop,
                greeting = "Would you like to buy a bow or some arrows?",
                shop_npcs.lowe,
            ),
            shop(
                title = "Horvik's Armour Shop",
                inv = shop_invs.armourshop,
                greeting = "Would you like to buy some armour?",
                shop_npcs.horvik,
            ),
            shop(
                title = "Thessalia's Fine Clothes",
                inv = shop_invs.clotheshop,
                greeting = "Would you like to buy some clothes?",
                shop_npcs.thessalia_normal,
                shop_npcs.thessalia_league,
            ),
            shop(
                title = "Varrock Swordshop",
                inv = shop_invs.swordshop,
                greeting = "Would you like to buy a sword?",
                shop_npcs.swordshop_keeper,
                shop_npcs.swordshop_assistant,
            ),

            // General stores. The cache numbers keepers and shops in lockstep, so
            // `generalshopkeeperN` and `generalassistantN` both open `generalshopN`.
            generalStore(
                shop_invs.generalshop2,
                shop_npcs.generalshopkeeper2,
                shop_npcs.generalassistant2,
            ),
            generalStore(
                shop_invs.generalshop3,
                shop_npcs.generalshopkeeper3,
                shop_npcs.generalassistant3,
            ),
            generalStore(
                shop_invs.generalshop4,
                shop_npcs.generalshopkeeper4,
                shop_npcs.generalassistant4,
            ),
            generalStore(
                shop_invs.generalshop5,
                shop_npcs.generalshopkeeper5,
                shop_npcs.generalassistant5,
            ),
            generalStore(
                shop_invs.generalshop6,
                shop_npcs.generalshopkeeper6,
                shop_npcs.generalassistant6,
            ),
            generalStore(
                shop_invs.generalshop7,
                shop_npcs.generalshopkeeper7,
                shop_npcs.generalassistant7,
            ),
        )

    val all: List<ShopAssignment> =
        alKharidAndVarrock +
            VarrockExtraShops.all +
            FaladorShops.all +
            PortSarimShops.all +
            RimmingtonShops.all +
            AsgarnianShops.all +
            KandarinShops.all +
            KaramjaShops.all

    private fun generalStore(inv: InvType, vararg npcs: NpcType): ShopAssignment =
        ShopAssignment(
            title = "General Store",
            inv = inv,
            greeting = "Can I help you at all?",
            npcs = npcs.toList(),
        )
}
