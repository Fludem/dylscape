package org.rsmod.content.custom.cluechest.configs

import org.rsmod.api.type.builders.obj.ObjBuilder

/**
 * The clue keys vanilla never had: beginner, easy and hard. Ids live in
 * `.data/symbols/.local/obj.sym`; a new obj only exists after `./gradlew packCache`.
 *
 * Every field is copied from `trail_clue_medium_riddle001_key`. Vanilla draws every clue key -
 * medium and elite alike - as model 2372 with no recolour, so only the name tells the tiers apart,
 * and these follow suit.
 */
internal object ClueKeyBuilds : ObjBuilder() {
    init {
        key("trail_key_beginner", "Key (beginner)", members = false)
        key("trail_key_easy", "Key (easy)", members = true)
        key("trail_key_hard", "Key (hard)", members = true)
    }

    private fun key(internal: String, keyName: String, members: Boolean) {
        build(internal) {
            name = keyName
            desc = "A key to unlock a treasure chest."
            model = 2372
            zoom2d = 700
            xan2d = 328
            yan2d = 20
            xof2d = -3
            yof2d = 5
            cost = 20
            this.members = members
            tradeable = true
            category = 180
        }
    }
}
