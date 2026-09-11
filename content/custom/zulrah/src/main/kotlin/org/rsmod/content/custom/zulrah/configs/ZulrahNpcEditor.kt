package org.rsmod.content.custom.zulrah.configs

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.content.custom.zulrah.ZulrahForm

/**
 * What the cache does not already say about Zulrah and its snakelings.
 *
 * `ZulrahDump` shows the three forms already carry 500 hitpoints, defence/ranged/magic 300, attack
 * rate 3, their `Attack` op, size 5, and a per-form `defence_magic` of -45 (green), 0 (red) and 300
 * (blue). None of that is repeated. What is missing:
 * 1. **Movement and AI.** Every form ships `Wander` with a wander range of 5. Zulrah never moves on
 *    its own - the fight script `telejump`s it between spots - so it gets `nomove`, no wandering
 *    and no default mode. It has no hunt mode either: the fight script decides every attack.
 * 2. **Ranged defence.** Unset, so it defaults to 0 for all three and the green form, whose whole
 *    point is that it resists ranged, would be shot freely.
 * 3. **Defend and death animations.** `NpcDeath` reads `death_anim` off the npc's *base* type,
 *    which is the green form it spawns as, so it has to be on every form rather than just one.
 * 4. **Immunity.** Zulrah and its snakelings cannot be poisoned or envenomed.
 *
 * The snakelings get the hunt modes that make them go for the player the moment they hatch, and
 * their own animations. Their attacks are rolled in `ZulrahScript`, because the magic one would
 * otherwise go through `NvPCombat`'s magic driver, which reads no max hit for it.
 */
internal object ZulrahNpcEditor : NpcEditor() {
    init {
        for ((form, type) in zulrah_npcs.forms) {
            edit(type) {
                moveRestrict = nomove
                wanderRange = 0
                defaultMode = none
                maxRange = ZULRAH_MAX_RANGE

                param[params.defend_anim] = zulrah_seqs.defend
                param[params.death_anim] = zulrah_seqs.death

                val rangedDefence = rangedDefence(form)
                param[params.defence_light] = rangedDefence
                param[params.defence_standard] = rangedDefence
                param[params.defence_heavy] = rangedDefence

                param[params.poison_immunity] = 1
                param[params.venom_immunity] = constants.npc_venom_full_immunity
            }
        }

        edit(zulrah_npcs.snakeling_melee) {
            huntMode = huntmodes.aggressive_melee
            huntRange = SNAKELING_HUNT_RANGE
            snakeling()
        }

        edit(zulrah_npcs.snakeling_magic) {
            huntMode = huntmodes.aggressive_ranged
            huntRange = SNAKELING_HUNT_RANGE
            attackRange = SNAKELING_MAGIC_RANGE
            snakeling()
        }
    }

    private fun org.rsmod.api.type.script.dsl.NpcPluginBuilder.snakeling() {
        // They hatch where the egg lands and chase from there; there is nowhere to wander back to.
        wanderRange = 0
        maxRange = ZULRAH_MAX_RANGE
        param[params.attack_anim] = zulrah_seqs.snakeling_attack
        param[params.defend_anim] = zulrah_seqs.snakeling_defend
        param[params.death_anim] = zulrah_seqs.snakeling_death
        param[params.poison_immunity] = 1
        param[params.venom_immunity] = constants.npc_venom_full_immunity
    }

    /**
     * The wiki's ranged defence per form: the green form is the one that shrugs off arrows and the
     * blue one the one that dies to them, which is the whole reason to switch gear mid-fight.
     */
    private fun rangedDefence(form: ZulrahForm): Int =
        when (form) {
            ZulrahForm.Serpentine -> 50
            ZulrahForm.Magma -> 0
            ZulrahForm.Tanzanite -> 0
        }

    /** Wide enough that nothing in the shrine is ever "too far" for the retreat check. */
    private const val ZULRAH_MAX_RANGE = 32

    private const val SNAKELING_HUNT_RANGE = 16
    private const val SNAKELING_MAGIC_RANGE = 7
}
