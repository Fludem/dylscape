package org.rsmod.content.custom.vorkath.configs

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor

/**
 * What the cache does not already say about Vorkath.
 *
 * It says most of it, and `VorkathDump` is what proves it: the awake form carries 750 hitpoints,
 * attack 560, strength 308, defence 214, ranged 308, magic 150, an attack rate of 5, its `Attack`
 * op, size 7, its ready and walk seqs, and stab/slash/crush/magic defence bonuses of
 * 26/108/108/240. None of that is repeated here - a value the cache already holds is a second place
 * to be wrong, and every type edit is additive once packed.
 *
 * What is missing:
 * 1. **Movement and AI.** Both forms ship `Wander` with a range of 5. Vorkath never leaves its
 *    platform, so it gets `nomove`, no wandering and no default mode. The awake form hunts on the
 *    ranged mode, which engages through `ApPlayer2`, from far enough to cover the whole crater.
 * 2. **Ranged defence.** `defence_ranged` is unset, and `PvNRangedAccuracy` reads one of
 *    `defence_light`/`_standard`/`_heavy` by the player's ammunition, so out of the box it is
 *    trivially shot. Live is 26.
 * 3. **Attack style.** `npc_attack_type` decides how `NpcRetaliateScript` answers a hit: tagged
 *    ranged, a struck Vorkath goes to ap mode and fires from where it stands rather than trying to
 *    walk into melee range off a platform it cannot leave.
 * 4. **Animations.** No attack or death anim, so it would punch and die like an unarmed human.
 * 5. **Immunity.** Vorkath cannot be poisoned or envenomed.
 *
 * Deliberately **not** set: `proj_travel`/`proj_type`. The shared driver fires the one projectile
 * an npc declares, and Vorkath has two; `VorkathScript` binds the attack itself. Nor `respawnRate`:
 * the fighting form is added with a finite duration and never respawns; the script puts the sleeper
 * back itself.
 */
internal object VorkathNpcEditor : NpcEditor() {
    init {
        edit(vorkath_npcs.sleeping) {
            wanderRange = 0
            defaultMode = none
            moveRestrict = nomove
        }

        edit(vorkath_npcs.awake) {
            wanderRange = 0
            defaultMode = none
            moveRestrict = nomove
            // Disables the retreat branch in `NpcExtensions.retaliate`, as in the kings' editor.
            maxRange = 32

            huntMode = huntmodes.aggressive_ranged
            huntRange = ARENA_RANGE
            attackRange = ARENA_RANGE

            param[params.npc_attack_type] = categories.attacktype_ranged
            param[params.attack_anim] = vorkath_seqs.attack
            param[params.death_anim] = vorkath_seqs.death

            param[params.defence_light] = RANGED_DEFENCE
            param[params.defence_standard] = RANGED_DEFENCE
            param[params.defence_heavy] = RANGED_DEFENCE

            param[params.poison_immunity] = 1
            param[params.venom_immunity] = constants.npc_venom_full_immunity
        }

        // Neither Torfinn may wander: one stands on a pier and the other on a strip of shore.
        for (torfinn in listOf(vorkath_npcs.torfinn_rellekka, vorkath_npcs.torfinn_ungael)) {
            edit(torfinn) {
                wanderRange = 0
                defaultMode = none
            }
        }
    }

    /** The wiki's ranged defence bonus. */
    private const val RANGED_DEFENCE = 26

    /**
     * From the platform's centre the farthest floor tile in the crater is 12 away; 16 covers it
     * with margin and still stops well short of the shore outside.
     */
    private const val ARENA_RANGE = 16
}
