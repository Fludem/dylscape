package org.rsmod.content.custom.skillingtasks

import org.rsmod.api.config.refs.stats
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

/**
 * A counted skilling action, one per event the progress script listens on. [verb] is how the
 * Taskmaster phrases the job ("cut 150 yew logs"), [skillName] how the payout is described.
 */
enum class TaskKind(val stat: StatType, val verb: String, val skillName: String) {
    Chop(stats.woodcutting, "cut", "Woodcutting"),
    Mine(stats.mining, "mine", "Mining"),
    Fish(stats.fishing, "catch", "Fishing"),
    Cook(stats.cooking, "cook", "Cooking"),
    Smelt(stats.smithing, "smelt", "Smithing"),
    Smith(stats.smithing, "smith", "Smithing"),
    Fletch(stats.fletching, "fletch", "Fletching"),
    Craft(stats.crafting, "craft", "Crafting"),
    Potion(stats.herblore, "mix", "Herblore"),
}

/**
 * One job the Taskmaster can hand out.
 *
 * [key] is what the database stores, so rows can be reordered or renamed freely but a key, once
 * shipped, must keep meaning the same thing. [products] are the objs that count; a task with
 * several (an anvil tier) advances on any of them. [level] gates who can be given it and scales the
 * point payout; [xpEach] is the skill's own experience per item, which sizes the bonus.
 */
class SkillingTask(
    val key: String,
    val kind: TaskKind,
    val name: String,
    val products: Set<ObjType>,
    val level: Int,
    val xpEach: Double,
) {
    init {
        require(key.matches(KEY_PATTERN)) { "Task key must be a slug: '$key'" }
        require(level in 1..99) { "Task $key has a level outside 1..99: $level" }
        require(xpEach > 0.0) { "Task $key pays no experience" }
        require(products.isNotEmpty()) { "Task $key names no products" }
    }

    /** "cut 150 yew logs" */
    fun describe(amount: Int): String = "${kind.verb} $amount $name"

    fun counts(kind: TaskKind, product: ObjType): Boolean =
        kind == this.kind && products.any { it.id == product.id }

    override fun toString(): String = "SkillingTask($key)"

    private companion object {
        val KEY_PATTERN = Regex("[a-z0-9_]+")
    }
}
