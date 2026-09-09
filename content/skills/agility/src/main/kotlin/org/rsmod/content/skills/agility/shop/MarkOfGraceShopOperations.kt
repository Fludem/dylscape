package org.rsmod.content.skills.agility.shop

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.shops.operation.StandardShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.content.skills.agility.configs.AgilityObjs
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.objtx.TransactionResult

/**
 * Grace's till.
 *
 * The standard shop operations price everything off `objType.cost` and slide that price with how
 * much stock is left, which is right for a coin shop and wrong here: Grace's prices are fixed, and
 * marks of grace have a `cost` in the cache that has nothing to do with what she charges. So this
 * prices off [GracefulPrices] instead and never touches `StandardGpCostCalculations`.
 *
 * **Buy only.** The live game refunds a piece at 80% of its price; that is deliberately not
 * implemented here, so the sell paths refuse rather than quietly doing something wrong with a
 * player's set.
 */
public class MarkOfGraceShopOperations
@Inject
constructor(private val objTypes: ObjTypeList, private val restockProcess: ShopRestockProcess) :
    StandardShopOperations {
    private val currencyObj: UnpackedObjType by lazy { objTypes[AgilityObjs.mark_of_grace] }

    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val obj = shop.inv[slot] ?: return
        val objType = objTypes[obj]
        val price = GracefulPrices[obj.id]
        if (price == null) {
            player.mes("${objType.name}: Grace doesn't sell that.")
            return
        }
        player.mes("${objType.name}: currently costs $price ${marks(price)}.")
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val shopInv = shop.inv
        val obj = shopInv[slot] ?: return
        val objType = objTypes[obj]

        val price = GracefulPrices[obj.id]
        if (price == null) {
            player.mes("Grace doesn't sell that.")
            return
        }

        val inStock = min(shopInv.count(obj, objType), request)
        if (inStock == 0) {
            player.mes("That item is currently out of stock.")
            return
        }

        val heldMarks = sideInv.count(currencyObj)
        val affordable = heldMarks / price
        val space =
            if (objType.isStackable) {
                Int.MAX_VALUE - sideInv.count(objType)
            } else {
                sideInv.freeSpace()
            }

        val count = minOf(inStock, affordable, space)
        if (count <= 0) {
            player.mes(if (affordable == 0) NOT_ENOUGH_MARKS else NOT_ENOUGH_INV_SPACE)
            return
        }

        val transaction =
            player.invTransaction(sideInv) {
                val inv = select(sideInv)
                val stock = select(shopInv)
                delete {
                    this.from = inv
                    this.obj = currencyObj.id
                    this.strictCount = price * count
                }
                delete {
                    this.from = stock
                    this.obj = obj.id
                    this.strictCount = count
                }
                insert {
                    this.into = inv
                    this.obj = obj.id
                    this.strictCount = count
                }
            }

        val currencyDel = transaction.results[0]
        val stockObjAdd = transaction.results.getOrNull(2)
        when {
            currencyDel == TransactionResult.ObjNotFound -> player.mes(NOT_ENOUGH_MARKS)
            currencyDel == TransactionResult.NotEnoughObjCount -> player.mes(NOT_ENOUGH_MARKS)
            stockObjAdd == TransactionResult.NotEnoughSpace -> player.mes(NOT_ENOUGH_INV_SPACE)
            count < inStock && affordable <= count -> player.mes(NOT_ENOUGH_MARKS)
            count < inStock -> player.mes(NOT_ENOUGH_INV_SPACE)
        }

        if (transaction.success) {
            restockProcess += shopInv
        }

        // A sold-out line is kept in place at a count of zero rather than removed, so the shop
        // interface does not reshuffle under the player mid-purchase.
        if (shopInv[slot] == null) {
            shopInv.resetDefaultStockItem(slot, objType)
        }
    }

    override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
        player.mes(WONT_BUY_BACK)
    }

    override fun invSell(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        player.mes(WONT_BUY_BACK)
    }

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        val obj = inv[slot] ?: return
        val type = objTypes[obj]
        // Marks have no market price, and quoting one in coins would be nonsense here.
        player.objExamine(type, obj.count, marketPrice = 0)
    }

    private fun Inventory.resetDefaultStockItem(slot: Int, objType: ObjType) {
        val defaultStockIndices = type.stock?.indices ?: return
        if (slot in defaultStockIndices) {
            this[slot] = InvObj(objType, count = 0)
        }
    }

    private fun marks(count: Int): String = if (count == 1) "mark of grace" else "marks of grace"

    public companion object {
        public const val NOT_ENOUGH_MARKS: String = "You don't have enough marks of grace."
        public const val NOT_ENOUGH_INV_SPACE: String = "You don't have enough inventory space."
        public const val WONT_BUY_BACK: String = "Grace doesn't buy anything."
    }
}
