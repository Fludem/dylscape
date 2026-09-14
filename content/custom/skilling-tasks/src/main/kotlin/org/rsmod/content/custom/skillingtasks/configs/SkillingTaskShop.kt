@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.skillingtasks.configs

import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType

internal typealias skilling_task_currencies = SkillingTaskCurrencies

internal typealias skilling_task_invs = SkillingTaskInvs

/**
 * Skilling points as a shop currency.
 *
 * A `CurrencyType` is a name-only server type resolved from `.data/symbols/.local/currency.sym`, so
 * it needs no `packCache`. Unlike marks of grace there is no obj behind it: the balance lives in
 * `SkillingTaskState` and `SkillingPointShopOperations` debits it directly.
 */
object SkillingTaskCurrencies : CurrencyReferences() {
    val skilling_points = find("skilling_points")
}

/**
 * The reward counter's stock inv. `partyroom_tempinv` is a cache-named inv vanilla never stocks and
 * nothing on this server uses, the same trade city-shops makes with `omnishop_inv_temp` to avoid
 * minting an inv id and packing before the shop exists.
 */
object SkillingTaskInvs : InvReferences() {
    val reward_shop = find("partyroom_tempinv")
}

object SkillingRewardObjs : ObjReferences() {
    val lumberjack_hat = find("ramble_lumberjack_hat")
    val lumberjack_top = find("ramble_lumberjack_top")
    val lumberjack_legs = find("ramble_lumberjack_legs")
    val lumberjack_boots = find("ramble_lumberjack_boots")
    val prospector_helmet = find("motherlode_reward_hat")
    val prospector_jacket = find("motherlode_reward_top")
    val prospector_legs = find("motherlode_reward_legs")
    val prospector_boots = find("motherlode_reward_boots")
    val angler_hat = find("trawler_reward_hat")
    val angler_top = find("trawler_reward_top")
    val angler_waders = find("trawler_reward_legs")
    val angler_boots = find("trawler_reward_boots")
    val smiths_tunic = find("smithing_uniform_torso")
    val smiths_trousers = find("smithing_uniform_legs")
    val smiths_boots = find("smithing_uniform_boots")
    val smiths_gloves = find("smithing_uniform_gloves")
    val chefs_hat = find("chefs_hat")
    val dragon_axe = find("dragon_axe")
    val dragon_pickaxe = find("dragon_pickaxe")
    val dragon_harpoon = find("dragon_harpoon")
}

/** What the Taskmaster sells and for how many points. Order here is the order on the shelf. */
object SkillingRewards {
    class Reward(val obj: ObjType, val points: Int)

    val all: List<Reward> =
        listOf(
            Reward(SkillingRewardObjs.chefs_hat, 5),
            Reward(SkillingRewardObjs.lumberjack_hat, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.lumberjack_top, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.lumberjack_legs, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.lumberjack_boots, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.prospector_helmet, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.prospector_jacket, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.prospector_legs, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.prospector_boots, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.angler_hat, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.angler_top, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.angler_waders, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.angler_boots, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.smiths_tunic, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.smiths_trousers, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.smiths_boots, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.smiths_gloves, OUTFIT_PIECE),
            Reward(SkillingRewardObjs.dragon_axe, DRAGON_TOOL),
            Reward(SkillingRewardObjs.dragon_pickaxe, DRAGON_TOOL),
            Reward(SkillingRewardObjs.dragon_harpoon, DRAGON_TOOL),
        )

    /** Obj id -> price. Lazy because `ObjType.id` throws before the reference loader has run. */
    val pointsByObj: Map<Int, Int> by lazy { all.associate { it.obj.id to it.points } }

    private const val OUTFIT_PIECE = 25
    private const val DRAGON_TOOL = 150
}

/**
 * Stocks the reward counter. Stock is an RSMod extension vanilla never writes, so without this the
 * shop opens empty. Deep stock with a one-tick restock means it never runs dry; the price comes
 * from [SkillingRewards], never from the shelf.
 */
internal object SkillingRewardShopInv : InvEditor() {
    init {
        edit(SkillingTaskInvs.reward_shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (reward in SkillingRewards.all) {
                stock += stock(reward.obj, count = 100, restockCycles = 1)
            }
        }
    }
}
