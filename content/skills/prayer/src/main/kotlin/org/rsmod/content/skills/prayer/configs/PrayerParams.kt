package org.rsmod.content.skills.prayer.configs

import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.refs.param.ParamReferences

object PrayerParams : ParamReferences() {
    /**
     * The share of an offered bone's xp the altar grants, as a percentage: `100` is the plain bury
     * value and `350` is the Chaos altar's 3.5x. Defaulting to `100` is what makes an ordinary
     * altar refuse offerings - the script treats "no better than burying" as "not an offering
     * altar" rather than needing a second content group.
     */
    val offer_xp_percent = find<Int>("prayer_offer_xp_percent")

    /**
     * Percent chance (`0`-`100`) that an offered bone is *not* consumed. This is the Chaos altar's
     * signature 50% save, which is what takes it from 3.5x to an effective 7x xp per bone.
     */
    val offer_keep_percent = find<Int>("prayer_offer_keep_percent")
}

internal object PrayerParamBuilder : ParamBuilder() {
    init {
        build<Int>("prayer_offer_xp_percent") { default = 100 }
        build<Int>("prayer_offer_keep_percent") { default = 0 }
    }
}
