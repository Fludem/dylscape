package org.rsmod.content.skills.prayer.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.content.ContentGroupType
import org.rsmod.game.type.obj.ObjType

/**
 * Every buriable bone and scatterable pile of ashes in the cache.
 *
 * The `Bury` and `Scatter` ops are **already on these objs in the rev 233 cache** as inventory op1,
 * decoded rather than assumed, so this module only has to tag them into a content group and hang
 * the xp on them - no op editing is involved. That also means the reverse is true and dangerous: an
 * obj the cache marks buriable but this file forgets grants the player a dead menu entry that does
 * nothing at all. `PrayerConfigTest` asserts full coverage against the cache for exactly that
 * reason.
 *
 * Xp values are the real OSRS numbers. Four are less certain than the rest and are called out where
 * they appear: the bleached bones, the wyrmling bones, and the jogre paste variants, which are
 * quest items nobody trains on.
 */
internal object PrayerBoneObjs : ObjReferences() {
    // 4.5xp - the plain "Bones" family. Several are cosmetically distinct objs with the same name.
    val bones_burnt = find("bones_burnt")
    val newbie_bones = find("newbiebones")
    val tutorial_bones = find("tut2_bones")
    val soul_wars_bones = find("soul_wars_bones")
    val skeleton_bones = find("mm_skeleton_bones")
    val wolf_bones = find("wolf_bones")
    val bleached_bones = find("shade_bleached_bones")

    // 5xp - the Monkey Madness disguise bones.
    val monkey_bones = find("mm_normal_monkey_bones")
    val small_ninja_monkey_bones = find("mm_small_ninja_monkey_bones")
    val medium_ninja_monkey_bones = find("mm_medium_ninja_monkey_bones")
    val gorilla_bones = find("mm_normal_gorilla_monkey_bones")
    val bearded_gorilla_bones = find("mm_bearded_gorilla_monkey_bones")
    val small_zombie_monkey_bones = find("mm_small_zombie_monkey_bones")
    val large_zombie_monkey_bones = find("mm_large_zombie_monkey_bones")

    val bat_bones = find("bat_bones")
    val big_bones = find("big_bones")

    // 15xp - jogre bones and the Tai Bwo Wannai paste variants of them.
    val jogre_bones = find("tbwt_jogre_bones")
    val burnt_jogre_bones = find("tbwt_burnt_jogre_bones")
    val jogre_bones_raw_paste = find("tbwt_jogre_bones_in_raw_karambwanji_paste")
    val jogre_bones_cooked_paste = find("tbwt_jogre_bones_in_cooked_karambwanji_paste")
    val burnt_jogre_bones_raw_paste = find("tbwt_burnt_jogre_bones_in_raw_karambwanji_paste")
    val burnt_jogre_bones_cooked_paste = find("tbwt_burnt_jogre_bones_in_cooked_karambwanji_paste")
    val jogre_bones_marinated = find("tbwt_jogre_bones_marinated_in_karambwanji")
    val burnt_jogre_bones_marinated = find("tbwt_burnt_jogre_bones_marinated_in_karambwanji")

    val zogre_bones = find("zogre_bones")
    val shaikahan_bones = find("tbwt_beast_bones")
    val wyrmling_bones = find("babywyrm_bones")
    val babydragon_bones = find("babydragon_bones")
    val wyrm_bones = find("wyrm_bones")
    val dragon_bones = find("dragon_bones")
    val wyvern_bones = find("wyvern_bones")
    val drake_bones = find("drake_bones")
    val fayrg_bones = find("zogre_ancestral_bones_fayg")
    val lava_dragon_bones = find("lava_dragon_bones")
    val raurg_bones = find("zogre_ancestral_bones_raurg")
    val hydra_bones = find("hydra_bones")
    val dagannoth_bones = find("dagannoth_king_bones")
    val ourg_bones = find("zogre_ancestral_bones_ourg")
    val superior_dragon_bones = find("dragon_bones_superior")

    /**
     * The Barbarian Outpost archaeologist's Construction rewards. They are worth far more handed in
     * than buried, but the cache does put `Bury` on them, so they are tagged rather than left as a
     * menu entry that does nothing.
     */
    val long_bone = find("dorgesh_construction_bone")
    val curved_bone = find("dorgesh_construction_bone_curved")

    /** "Alan". The blessed-bone system carries a `blessed_alan_bones`, so this is a real bone. */
    val alan_bones = find("alan_bones")

    // Demonic ashes, which scatter rather than bury.
    val fiendish_ashes = find("fiendish_ashes")
    val vile_ashes = find("vile_ashes")
    val malicious_ashes = find("malicious_ashes")
    val abyssal_ashes = find("abyssal_ashes")
    val infernal_ashes = find("infernal_ashes")

    /**
     * Bones whose `Bury` is a quest step rather than prayer training, and which are therefore
     * deliberately left out of the content group: the Zadimus corpse (Shilo Village) and the five
     * goblin high priests reburied during Land of the Goblins. `PrayerConfigTest` knows about these
     * exclusions, so the coverage check stays exact rather than merely "mostly covered". If either
     * quest is implemented it will want its own `onOpHeld1` binding on these objs.
     */
    val zadimus_corpse = find("zqzadimusbones")
    val snothead = find("lotg_bone_highpriest1")
    val snailfeet = find("lotg_bone_highpriest2")
    val mosschin = find("lotg_bone_highpriest3")
    val redeyes = find("lotg_bone_highpriest4")
    val strongbones = find("lotg_bone_highpriest5")
}

private typealias bone = PrayerBoneObjs

internal object PrayerBonesEditor : ObjEditor() {
    init {
        buriable(objs.bones, xp = 4.5)
        buriable(bone.bones_burnt, xp = 4.5)
        buriable(bone.newbie_bones, xp = 4.5)
        buriable(bone.tutorial_bones, xp = 4.5)
        buriable(bone.soul_wars_bones, xp = 4.5)
        buriable(bone.skeleton_bones, xp = 4.5)
        buriable(bone.wolf_bones, xp = 4.5)
        // Uncertain: bleached bones are a Shades of Mort'ton item and are grouped with the plain
        // bones rather than scraped.
        buriable(bone.bleached_bones, xp = 4.5)

        buriable(bone.monkey_bones, xp = 5.0)
        buriable(bone.small_ninja_monkey_bones, xp = 5.0)
        buriable(bone.medium_ninja_monkey_bones, xp = 5.0)
        buriable(bone.gorilla_bones, xp = 5.0)
        buriable(bone.bearded_gorilla_bones, xp = 5.0)
        buriable(bone.small_zombie_monkey_bones, xp = 5.0)
        buriable(bone.large_zombie_monkey_bones, xp = 5.0)

        buriable(bone.bat_bones, xp = 5.3)
        buriable(bone.big_bones, xp = 15.0)

        buriable(bone.jogre_bones, xp = 15.0)
        // Uncertain: the burnt and karambwanji-paste jogre bones are quest-stage items. They take
        // the jogre value so that `Bury` is never a dead menu entry.
        buriable(bone.burnt_jogre_bones, xp = 15.0)
        buriable(bone.jogre_bones_raw_paste, xp = 15.0)
        buriable(bone.jogre_bones_cooked_paste, xp = 15.0)
        buriable(bone.burnt_jogre_bones_raw_paste, xp = 15.0)
        buriable(bone.burnt_jogre_bones_cooked_paste, xp = 15.0)
        buriable(bone.jogre_bones_marinated, xp = 15.0)
        buriable(bone.burnt_jogre_bones_marinated, xp = 15.0)

        buriable(bone.zogre_bones, xp = 22.5)
        buriable(bone.shaikahan_bones, xp = 25.0)
        // Uncertain: wyrmling bones sit between wolf and babydragon bones; the exact live value was
        // not confirmed.
        buriable(bone.wyrmling_bones, xp = 12.0)
        buriable(bone.babydragon_bones, xp = 30.0)
        buriable(bone.wyrm_bones, xp = 50.0)
        buriable(bone.dragon_bones, xp = 72.0)
        buriable(bone.wyvern_bones, xp = 72.0)
        buriable(bone.drake_bones, xp = 80.0)
        buriable(bone.fayrg_bones, xp = 84.0)
        buriable(bone.lava_dragon_bones, xp = 85.0)
        buriable(bone.raurg_bones, xp = 96.0)
        buriable(bone.hydra_bones, xp = 110.0)
        buriable(bone.dagannoth_bones, xp = 125.0)
        buriable(bone.ourg_bones, xp = 140.0)
        buriable(bone.superior_dragon_bones, xp = 150.0)

        // Uncertain: the two Construction bones and "Alan" are buriable per the cache but are not
        // bones anyone trains on, so their live values were not confirmed. Long and curved bones
        // take the big-bones tier; Alan takes the plain-bones one.
        buriable(bone.long_bone, xp = 15.0)
        buriable(bone.curved_bone, xp = 15.0)
        buriable(bone.alan_bones, xp = 4.5)

        scatterable(bone.fiendish_ashes, xp = 30.0)
        scatterable(bone.vile_ashes, xp = 75.0)
        scatterable(bone.malicious_ashes, xp = 105.0)
        scatterable(bone.abyssal_ashes, xp = 125.0)
        scatterable(bone.infernal_ashes, xp = 200.0)
    }

    private fun buriable(type: ObjType, xp: Double) = grantsXp(type, PrayerContent.prayer_bones, xp)

    private fun scatterable(type: ObjType, xp: Double) =
        grantsXp(type, PrayerContent.prayer_ashes, xp)

    private fun grantsXp(type: ObjType, group: ContentGroupType, xp: Double) =
        edit(type) {
            contentGroup = group
            param[params.skill_xp] = PlayerStatMap.toFineXP(xp).toInt()
        }
}
