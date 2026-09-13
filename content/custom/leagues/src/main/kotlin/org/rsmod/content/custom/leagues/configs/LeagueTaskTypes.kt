package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.api.type.refs.struct.StructReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.tasks.LeagueTask
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.enums.HashedEnumType
import org.rsmod.game.type.struct.HashedStructType
import org.rsmod.game.type.struct.StructType
import org.rsmod.game.type.varp.VarpType

typealias league_task_params = LeagueTaskParams

typealias league_task_structs = LeagueTaskStructs

typealias league_task_varps = LeagueTaskVarps

typealias league_task_objs = LeagueTaskObjs

/**
 * The vanilla params the `league_tasks` screen reads from each task struct, named in
 * `.data/symbols/.local/param.sym`. Decoded from `[proc,league_tasks_draw_list]` (3204) and
 * `[proc,league_task_is_completed]` (3216) by `LeagueTasksDump`.
 */
object LeagueTaskParams : ParamReferences() {
    /** On the league struct: the INT -> STRUCT enum the list walks, keys `0 until size`. */
    val tasks_enum = find<EnumType<Int, Int>>("league_tasks_enum")

    /**
     * The completion index. The client reads bit `index % 32` of varp `league_task_completed_<index
     * / 32>`, so this is the whole of a task's saved state.
     */
    val index = find<Int>("league_task_index")
    val name = find<String>("league_task_name")
    val description = find<String>("league_task_description")

    /** `LeagueTaskType.cacheValue`; the list's "Type:" line and its type filter. */
    val type = find<Int>("league_task_type")

    /** `LeagueTaskArea.cacheValue`; the "Area:" line and the area filter. */
    val area = find<Int>("league_task_area")

    /** `GatherKind.skill` / the skill's index in enum 2729, or 0 for none. */
    val skill = find<Int>("league_task_skill")

    /** `LeagueTaskTier.cacheValue`; enum 2671 maps it onto the points the row shows. */
    val tier = find<Int>("league_task_tier")

    /** On each relic tier struct: the League Points that open the tier. */
    val tier_points = find<Int>("league_tier_points")
}

/**
 * Our task list enum, by explicit id. Builders and editors encode their values the moment they are
 * instantiated, before any reference to a not-yet-packed type could resolve, so both the enum and
 * the task structs are addressed by the ids the symbol files pin (`LeagueTaskTableTest` checks the
 * files agree).
 */
object LeagueTaskListEnum {
    const val NAME: String = "league_task_list"

    /** `.data/symbols/.local/enum.sym`. */
    const val ID: Int = 50100

    val type: EnumType<Int, Int> =
        HashedEnumType(
            Int::class,
            Int::class,
            startHash = null,
            internalName = NAME,
            internalId = ID,
        )
}

/** The struct a task is packed as: `LeagueTask.STRUCT_ID_BASE + id`, per `.local/struct.sym`. */
val LeagueTask.structType: StructType
    get() =
        HashedStructType(
            startHash = null,
            internalName = structName,
            internalId = LeagueTask.STRUCT_ID_BASE + id,
        )

object LeagueTaskStructs : StructReferences() {
    /** `enum 2670[5]`: the Raging Echoes league struct the client draws for `league_type` 5. */
    val league = find("league_raging_echoes")

    /** The eight relic tier structs, in tier order. */
    val tiers: List<StructType> = List(Relic.TIER_COUNT) { find("league_tier_${it + 1}") }
}

object LeagueTaskVarps : VarpReferences() {
    /** The 62 vanilla completion bitsets, all `Perm`; see [LeagueTaskParams.index]. */
    val completed: List<VarpType> = List(COMPLETION_VARPS) { find("league_task_completed_$it") }

    const val COMPLETION_VARPS: Int = 62
}

/** Every obj a task names, as a product to count or a piece of gear to equip. */
object LeagueTaskObjs : ObjReferences() {
    // Ores and bars.
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val coal = find("coal")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")
    val bronze_bar = find("bronze_bar")
    val steel_bar = find("steel_bar")
    val mithril_bar = find("mithril_bar")
    val runite_bar = find("runite_bar")

    // Logs.
    val oak_logs = find("oak_logs")
    val willow_logs = find("willow_logs")
    val maple_logs = find("maple_logs")
    val yew_logs = find("yew_logs")
    val magic_logs = find("magic_logs")

    // Fish, raw and cooked.
    val raw_trout = find("raw_trout")
    val raw_salmon = find("raw_salmon")
    val raw_lobster = find("raw_lobster")
    val raw_swordfish = find("raw_swordfish")
    val raw_shark = find("raw_shark")
    val shrimp = find("shrimp")
    val lobster = find("lobster")
    val shark = find("shark")

    // Smithing, crafting and fletching products.
    val bronze_dagger = find("bronze_dagger")
    val steel_platebody = find("steel_platebody")
    val sapphire = find("sapphire")
    val shortbow = find("shortbow")
    val longbow = find("longbow")
    val oak_shortbow = find("oak_shortbow")
    val oak_longbow = find("oak_longbow")
    val willow_shortbow = find("willow_shortbow")
    val willow_longbow = find("willow_longbow")
    val maple_shortbow = find("maple_shortbow")
    val maple_longbow = find("maple_longbow")
    val yew_shortbow = find("yew_shortbow")
    val yew_longbow = find("yew_longbow")
    val magic_shortbow = find("magic_shortbow")
    val magic_longbow = find("magic_longbow")
    val unstrung_yew_longbow = find("unstrung_yew_longbow")
    val unstrung_magic_longbow = find("unstrung_magic_longbow")

    // Bones.
    val bones = find("bones")
    val big_bones = find("big_bones")
    val dragon_bones = find("dragon_bones")

    // Gear.
    val leather_armour = find("leather_armour")
    val iron_scimitar = find("iron_scimitar")
    val amulet_of_power = find("amulet_of_power")
    val mithril_platebody = find("mithril_platebody")
    val rune_scimitar = find("rune_scimitar")
    val green_dhide_body = find("dragonhide_body")
    val mystic_hat = find("mystic_hat")
    val amulet_of_glory = find("amulet_of_glory")
    val rune_platebody = find("rune_platebody")
    val dragon_scimitar = find("dragon_scimitar")
    val black_dhide_body = find("black_dragonhide_body")
    val granite_maul = find("granite_maul")
    val dragon_dagger = find("dragon_dagger")
    val rune_crossbow = find("xbows_crossbow_runite")
    val dragon_platebody = find("dragon_platebody")
    val abyssal_whip = find("abyssal_whip")
    val toxic_blowpipe = find("toxic_blowpipe")
    val serpentine_helm = find("serpentine_helm")
    val fire_cape = find("tzhaar_cape_fire")
    val ahrims_staff = find("barrows_ahrim_weapon")
    val dharoks_greataxe = find("barrows_dharok_weapon")
    val guthans_warspear = find("barrows_guthan_weapon")
    val karils_crossbow = find("barrows_karil_weapon")
    val torags_hammers = find("barrows_torag_weapon")
    val veracs_flail = find("barrows_verac_weapon")
}
