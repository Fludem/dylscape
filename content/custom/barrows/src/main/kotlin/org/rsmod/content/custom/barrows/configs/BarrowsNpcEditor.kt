package org.rsmod.content.custom.barrows.configs

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.content.custom.barrows.BarrowsStyle
import org.rsmod.content.custom.barrows.Brother

/**
 * What the cache does not already say about the six brothers.
 *
 * It says more than you would expect. `api/cache-enricher` gives them only an examine string, but
 * the *cache type* already carries their combat levels (Ahrim 1/1/100 with 100 magic, Dharok
 * 100/100/100, and so on, all 100 hitpoints), their attack and defence bonuses, their melee or
 * ranged strength, and their `attackrate` - every one of which matches live. So none of that is
 * repeated here; re-declaring it would only create a second place to be wrong.
 *
 * Four things are genuinely missing, and this editor supplies them:
 * 1. **`attack_melee`.** `NvPMeleeAccuracy` reads exactly this param and nothing else for the npc's
 *    attack roll. The cache gives the brothers `attack_stab`/`_slash`/`_crush` but not this, so
 *    without it the four melee brothers roll against a bonus of zero and barely land a hit.
 * 2. **Ranged defence.** `defence_ranged` and the `defence_light`/`_standard`/`_heavy` trio are all
 *    absent, so a player shooting a brother is rolling against defaults.
 * 3. **`npc_attack_type`.** Decides which of stab/slash/crush the melee brothers attack with;
 *    without it `NvPCombatScript.resolveMeleeAttackType` falls through to crush for all of them.
 * 4. **Animations.** No brother has an `attack_anim`, and `params.attack_anim` defaults to
 *    `human_unarmedpunch` - so out of the box all six would stand there throwing punches while
 *    holding a greataxe. The seqs are in the cache and named for them.
 *
 * Plus the AI: every brother ships as `defaultMode = Wander` with no hunt mode, which means they
 * wander five tiles from the sarcophagus and ignore the player entirely.
 */
internal object BarrowsNpcEditor : NpcEditor() {
    init {
        for ((brother, type) in barrows_npcs.brothers) {
            edit(type) {
                // Two jobs, the same pair `KnightWavesNpcEditor` documents: it stops the brother
                // roaming away from his sarcophagus, and it disables the retreat branch in
                // `NpcExtensions.retaliate`, which only fires when `wanderRange > 0` and would
                // otherwise send a brother jogging home the moment the fight moved a few tiles.
                wanderRange = 0
                maxRange = 32
                defaultMode = none
                // Raised angry: the fight starts itself the moment the sarcophagus is searched.
                // `findNewMode` for these is `OpPlayer2`, which is what the combat scripts bind.
                huntMode =
                    when (brother.style) {
                        BarrowsStyle.Melee -> huntmodes.aggressive_melee
                        BarrowsStyle.Magic,
                        BarrowsStyle.Ranged -> huntmodes.aggressive_ranged
                    }
                huntRange = 10
                // Karil and Ahrim attack from where they stand; the rest close in.
                attackRange = if (brother.style == BarrowsStyle.Melee) 1 else 8

                param[params.retreat] = 0
                param[params.attack_melee] = MELEE_ATTACK_BONUS
                param[params.defence_ranged] = RANGED_DEFENCE
                param[params.defence_light] = RANGED_DEFENCE
                param[params.defence_standard] = RANGED_DEFENCE
                param[params.defence_heavy] = RANGED_DEFENCE

                param[params.attack_anim] = attackAnim(brother)
                defendAnim(brother)?.let { param[params.defend_anim] = it }

                param[params.npc_attack_type] =
                    when (brother) {
                        // A greataxe slashes; a warspear stabs; hammers and a flail crush.
                        Brother.Dharok -> categories.attacktype_slash
                        Brother.Guthan -> categories.attacktype_stab
                        else -> categories.attacktype_crush
                    }
            }
        }
    }

    private fun attackAnim(brother: Brother) =
        when (brother) {
            Brother.Ahrim -> barrows_seqs.ahrim_special
            Brother.Dharok -> barrows_seqs.dharok_attack
            Brother.Guthan -> barrows_seqs.guthan_attack
            Brother.Karil -> barrows_seqs.karil_attack
            Brother.Torag -> barrows_seqs.torag_attack
            Brother.Verac -> barrows_seqs.verac_attack
        }

    /** Only three of the six have a named defend seq; the rest keep the cache's default. */
    private fun defendAnim(brother: Brother) =
        when (brother) {
            Brother.Guthan -> barrows_seqs.guthan_defend
            Brother.Verac -> barrows_seqs.verac_defend
            else -> null
        }

    /**
     * The brothers' melee attack bonus.
     *
     * All six sit at combat 98-115 with 100 attack, which puts them in the same band as the other
     * mid-level bossing npcs the formulae were tuned against. One figure covers all of them because
     * the cache already differentiates them where it matters - through `attack_stab`/`_slash`/
     * `_crush`, melee strength and attack speed.
     */
    private const val MELEE_ATTACK_BONUS = 100

    /**
     * Ranged defence, matched to the brothers' existing `defence_stab`/`_slash`/`_crush` band so
     * that shooting one is neither trivially better nor worse than meleeing it. The cache leaves
     * all four ranged-defence params unset.
     */
    private const val RANGED_DEFENCE = 200
}
