package org.rsmod.content.custom.cluechest

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.content.custom.droptables.DropTable
import org.rsmod.content.custom.droptables.DropTableRoller
import org.rsmod.content.custom.droptables.RolledDrop
import org.rsmod.content.custom.droptables.data.DropTableResourceLoader

/**
 * Rolls a reward casket.
 *
 * The tables are loaded on first use rather than at boot, and a table the loader rejected is simply
 * missing: the chest then refuses to open and keeps the key. `ClueCasketTest` pins [errors] empty,
 * which is where a bad import is meant to be caught.
 */
@Singleton
class ClueCasket
@Inject
constructor(
    private val random: GameRandom,
    private val roller: DropTableRoller,
    private val loader: DropTableResourceLoader,
) {
    private val loaded by lazy { loader.loadSources(ClueCasket::class.java, CASKETS_FILE) }

    val errors: List<String>
        get() = loaded.errors

    fun table(tier: ClueTier): DropTable? = loaded.tables[tier.casket]

    /** Every drop from one casket of [tier], in roll order; null if its table did not load. */
    fun roll(tier: ClueTier): List<RolledDrop>? {
        val table = table(tier) ?: return null
        val rolls = random.of(tier.rolls.first, tier.rolls.last)
        return List(rolls) { roller.roll(table) }.flatten()
    }

    private companion object {
        const val CASKETS_FILE = "caskets.toml"
    }
}
