package org.rsmod.content.skills.mining.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

private typealias anims = MiningSeqs

/**
 * Tags every pickaxe into the `mining_pickaxe` content group and gives it its swing animation and
 * Mining level requirement.
 *
 * `levelrequire` is what [org.rsmod.content.skills.mining.scripts.Mining] uses to pick the best
 * pickaxe a player can actually swing, so an untagged pickaxe is simply invisible to the skill.
 */
internal object MiningPickaxes : ObjEditor() {
    init {
        pickaxe(objs.bronze_pickaxe, anims.bronze, level = 1)
        pickaxe(MiningObjs.iron_pickaxe, anims.iron, level = 1)
        pickaxe(MiningObjs.steel_pickaxe, anims.steel, level = 6)
        pickaxe(MiningObjs.black_pickaxe, anims.black, level = 11)
        pickaxe(MiningObjs.mithril_pickaxe, anims.mithril, level = 21)
        pickaxe(MiningObjs.adamant_pickaxe, anims.adamant, level = 31)
        pickaxe(MiningObjs.rune_pickaxe, anims.rune, level = 41)
        pickaxe(MiningObjs.gilded_pickaxe, anims.gilded, level = 41)
        pickaxe(objs.dragon_pickaxe, anims.dragon, level = 61)
        pickaxe(objs.dragon_pickaxe_upgraded, anims.dragon_upgraded, level = 61)
        // The Trailblazer and Zalcano recolours are cosmetic re-skins of the dragon pickaxe and
        // have no mining animation of their own in the cache.
        pickaxe(objs.dragon_pickaxe_or_trailblazer, anims.dragon, level = 61)
        pickaxe(objs.dragon_pickaxe_or_zalcano, anims.dragon, level = 61)
        pickaxe(objs.third_age_pickaxe, anims.third_age, level = 61)
        pickaxe(objs.infernal_pickaxe, anims.infernal, level = 61)
        pickaxe(objs.infernal_pickaxe_or, anims.infernal, level = 61)
        pickaxe(objs.crystal_pickaxe, anims.crystal, level = 71)
    }

    private fun pickaxe(type: ObjType, anim: SeqType, level: Int) {
        edit(type) {
            contentGroup = MiningContent.mining_pickaxe
            param[params.skill_anim] = anim
            param[params.levelrequire] = level
        }
    }
}
