package org.rsmod.content.custom.skillingtasks.shop

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.shops.operation.StandardShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.content.custom.skillingtasks.SkillingTaskState
import org.rsmod.content.custom.skillingtasks.configs.SkillingRewards
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.objtx.TransactionResult

/**
 * The Taskmaster's till.
 *
 * Modelled on Grace's marks-of-grace shop: prices come from [SkillingRewards], never from
 * `objType.cost`, and the shop is buy only. The difference is the currency: there is no obj to hand
 * over, so the points are checked and debited on [SkillingTaskState] around the stock transaction
 * instead of being deleted from the side inventory inside it.
 */
public class SkillingPointShopOperations
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val restockProcess: ShopRestockProcess,
    private val state: SkillingTaskState,
) : StandardShopOperations {
    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val obj = shop.inv[slot] ?: return
        val objType = objTypes[obj]
        val price = SkillingRewards.pointsByObj[obj.id]
        if (price == null) {
            player.mes("${objType.name}: the Taskmaster doesn't sell that.")
            return
        }
        player.mes("${objType.name}: currently costs $price ${points(price)}.")
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val shopInv = shop.inv
        val obj = shopInv[slot] ?: return
        val objType = objTypes[obj]

        val price = SkillingRewards.pointsByObj[obj.id]
        if (price == null) {
            player.mes("The Taskmaster doesn't sell that.")
            return
        }

        val inStock = min(shopInv.count(obj, objType), request)
        if (inStock == 0) {
            player.mes("That item is currently out of stock.")
            return
        }

        val affordable = state.points(player) / price
        val space =
            if (objType.isStackable) {
                Int.MAX_VALUE - sideInv.count(objType)
            } else {
                sideInv.freeSpace()
            }

        val count = minOf(inStock, affordable, space)
        if (count <= 0) {
            player.mes(if (affordable == 0) NOT_ENOUGH_POINTS else NOT_ENOUGH_INV_SPACE)
            return
        }

        val transaction =
            player.invTransaction(sideInv) {
                val inv = select(sideInv)
                val stock = select(shopInv)
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

        val stockObjAdd = transaction.results.getOrNull(1)
        when {
            stockObjAdd == TransactionResult.NotEnoughSpace -> player.mes(NOT_ENOUGH_INV_SPACE)
            count < inStock && affordable <= count -> player.mes(NOT_ENOUGH_POINTS)
            count < inStock -> player.mes(NOT_ENOUGH_INV_SPACE)
        }

        if (transaction.success) {
            // The stock moved, so the points must follow; `spendPoints` cannot refuse here because
            // `affordable` was computed off the same balance on the same tick.
            check(state.spendPoints(player, price * count)) { "Points changed mid-purchase." }
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
        player.objExamine(type, obj.count, marketPrice = 0)
    }

    private fun Inventory.resetDefaultStockItem(slot: Int, objType: ObjType) {
        val defaultStockIndices = type.stock?.indices ?: return
        if (slot in defaultStockIndices) {
            this[slot] = InvObj(objType, count = 0)
        }
    }

    private fun points(count: Int): String = if (count == 1) "skilling point" else "skilling points"

    public companion object {
        public const val NOT_ENOUGH_POINTS: String = "You don't have enough skilling points."
        public const val NOT_ENOUGH_INV_SPACE: String = "You don't have enough inventory space."
        public const val WONT_BUY_BACK: String = "The Taskmaster doesn't buy anything."
    }
}
