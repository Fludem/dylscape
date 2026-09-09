package org.rsmod.content.skills.herblore.configs

import org.rsmod.api.config.refs.stats

/**
 * The potions people drink in a fight: the boosts, the brews and the divine family.
 *
 * Every number is the live formula rather than an approximation. "Raises Attack by 10% + 3" is
 * exactly the `(constant, percent)` pair `statBoost` takes, so a row reads as the wiki writes it.
 */
internal object PotionsCombat : PotionFamily() {
    init {
        ladder("1attack", effects = listOf(boost(stats.attack, 3, 10)))
        ladder("2attack", effects = listOf(boost(stats.attack, 5, 15)))

        // The one irregular head in the whole cache: `strength4`, not `4dose1strength`. Every other
        // family of the forty-six is a complete `4dose..1dose` set.
        ladder("1strength", head = "strength4", effects = listOf(boost(stats.strength, 3, 10)))
        ladder("2strength", effects = listOf(boost(stats.strength, 5, 15)))

        // American here, British in `divinedefence` twenty lines down. The cache's spelling, twice,
        // and not ours to fix.
        ladder("1defense", effects = listOf(boost(stats.defence, 3, 10)))
        ladder("2defense", effects = listOf(boost(stats.defence, 5, 15)))

        // A combat potion raises Attack and Strength only; the super raises Defence too.
        ladder("combat", effects = listOf(boost(stats.attack, 3, 10), boost(stats.strength, 3, 10)))
        ladder(
            "2combat",
            effects =
                listOf(
                    boost(stats.attack, 5, 15),
                    boost(stats.strength, 5, 15),
                    boost(stats.defence, 5, 15),
                ),
        )

        ladder("rangerspotion", effects = listOf(boost(stats.ranged, 4, 10)))
        ladder("1magic", effects = listOf(boost(stats.magic, 4)))
        ladder("magicess", effects = listOf(boost(stats.magic, 3)))

        ladder("bastion", effects = listOf(boost(stats.ranged, 4, 10), boost(stats.defence, 5, 15)))
        ladder("battlemage", effects = listOf(boost(stats.magic, 4), boost(stats.defence, 5, 15)))

        // The brews. Each trades combat stats for something else, and the drains are why
        // `PotionEffect` carries a direction rather than a signed number.
        ladder(
            "potionofzamorak",
            effects =
                listOf(
                    boost(stats.attack, 2, 20),
                    boost(stats.strength, percent = 12),
                    drain(stats.defence, percent = 10),
                    restore(stats.prayer, percent = 10),
                ),
            // The 12% hitpoints cost is not modelled: `PotionEffect` cannot deal damage, and a
            // drain on hitpoints is not the same thing as a hit. Worth doing properly rather than
            // approximating, so it is left out and said so.
        )
        ladder(
            "potionofsaradomin",
            heal = 2,
            healPercent = 15,
            effects =
                listOf(
                    boost(stats.defence, 2, 20),
                    drain(stats.attack, percent = 10),
                    drain(stats.strength, percent = 10),
                    drain(stats.magic, percent = 10),
                    drain(stats.ranged, percent = 10),
                ),
        )
        ladder(
            "ancientbrew",
            effects =
                listOf(
                    boost(stats.magic, 2, 5),
                    restore(stats.prayer, 2, 10),
                    drain(stats.attack, percent = 10),
                    drain(stats.strength, percent = 10),
                    drain(stats.defence, percent = 10),
                ),
        )
        ladder(
            "forgottenbrew",
            effects =
                listOf(
                    boost(stats.magic, 3, 8),
                    restore(stats.prayer, 3, 12),
                    drain(stats.attack, percent = 10),
                    drain(stats.strength, percent = 10),
                    drain(stats.defence, percent = 10),
                ),
        )

        // The divine family. In live these re-apply their boost every twenty-five ticks for five
        // minutes at a cost of ten hitpoints a tick, which is a real subsystem: a repeating queue
        // plus the four `divine_potion_*` varps the cache already reserves. Until that exists they
        // give their base potion's boost once, which is the honest subset rather than a wrong
        // approximation of the whole.
        ladder("divineattack", effects = listOf(boost(stats.attack, 5, 15)))
        ladder("divinestrength", effects = listOf(boost(stats.strength, 5, 15)))
        ladder("divinedefence", effects = listOf(boost(stats.defence, 5, 15)))
        ladder("divinerange", effects = listOf(boost(stats.ranged, 4, 10)))
        ladder("divinemagic", effects = listOf(boost(stats.magic, 4)))
        ladder(
            "divinebastion",
            effects = listOf(boost(stats.ranged, 4, 10), boost(stats.defence, 5, 15)),
        )
        ladder(
            "divinebattlemage",
            effects = listOf(boost(stats.magic, 4), boost(stats.defence, 5, 15)),
        )
        ladder(
            "divinecombat",
            effects =
                listOf(
                    boost(stats.attack, 5, 15),
                    boost(stats.strength, 5, 15),
                    boost(stats.defence, 5, 15),
                ),
        )
    }
}
