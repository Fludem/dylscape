package org.rsmod.content.skills.agility.configs

import org.rsmod.api.type.refs.loc.LocReferences

/**
 * Every course obstacle, exactly as the rev 233 cache names them.
 *
 * The nine rooftops use the `rooftops_<course>_<obstacle>` convention; the ground courses are a
 * grab-bag of `obstical_*` (sic), `*_log_balance1` and one-off names. **Every** obstacle carries
 * its interaction on `op1` - verified by dumping `LocTypeDecoder.decodeAll` straight from
 * `.data/cache/game` (see `tools/agility/`). The op *text* varies wildly ("Climb", "Cross",
 * "Balance", "Jump-up", "Teeth-grip", "Swing-across", "Vault", "Grab", "Walk-on"...) but the slot
 * never does, which is why [org.rsmod.content.skills.agility.scripts.AgilityCourseScript] can bind
 * the whole game with a single `onOpLoc1`.
 *
 * Locs deliberately absent: `rooftops_canifis_tightrope(_end)`, `rooftops_ardy_leapdown`,
 * `rooftops_kharid_tree`, `rooftops_falador_tightrope_fordiagonal` and
 * `rooftops_rellekka_wallclimb_noop` are named in the cache but have **no map placement anywhere in
 * the world** - they are unused variants, not obstacles we are skipping. Canifis genuinely has no
 * tightrope in this revision.
 */
public object AgilityLocs : LocReferences() {
    // Draynor Village.
    val draynor_wallclimb = find("rooftops_draynor_wallclimb")
    val draynor_tightrope_1 = find("rooftops_draynor_tightrope_1")
    val draynor_tightrope_2 = find("rooftops_draynor_tightrope_2")
    val draynor_wallcrossing = find("rooftops_draynor_wallcrossing")
    val draynor_wallscramble = find("rooftops_draynor_wallscramble")
    val draynor_leapdown = find("rooftops_draynor_leapdown")
    val draynor_crate = find("rooftops_draynor_crate")

    // Al Kharid.
    val kharid_wallclimb = find("rooftops_kharid_wallclimb")
    val kharid_tightrope_1 = find("rooftops_kharid_tightrope_1")
    val kharid_rope_swing = find("rooftops_kharid_rope_swing")
    val kharid_slide_side = find("rooftops_kharid_slide_side")
    val kharid_bamboo_tree_top = find("rooftops_kharid_bamboo_tree_top")
    val kharid_wallclimb_2 = find("rooftops_kharid_wallclimb_2")
    val kharid_tightrope_4 = find("rooftops_kharid_tightrope_4")
    val kharid_leapdown = find("rooftops_kharid_leapdown")

    // Varrock.
    val varrock_wallclimb = find("rooftops_varrock_wallclimb")
    val varrock_clothesline = find("rooftops_varrock_clothesline")
    val varrock_leaptoruins = find("rooftops_varrock_leaptoruins")
    val varrock_wallswing = find("rooftops_varrock_wallswing")
    val varrock_wallscramble = find("rooftops_varrock_wallscramble")
    val varrock_leaptobalcony = find("rooftops_varrock_leaptobalcony")
    val varrock_leapdown = find("rooftops_varrock_leapdown")
    val varrock_stepuproof = find("rooftops_varrock_stepuproof")
    val varrock_finish = find("rooftops_varrock_finish")

    // Canifis.
    val canifis_start_tree = find("rooftops_canifis_start_tree")
    val canifis_jump = find("rooftops_canifis_jump")
    val canifis_jump_2 = find("rooftops_canifis_jump_2")
    val canifis_jump_3 = find("rooftops_canifis_jump_3")
    val canifis_jump_4 = find("rooftops_canifis_jump_4")
    val canifis_jump_5 = find("rooftops_canifis_jump_5")
    val canifis_polevault = find("rooftops_canifis_polevault")
    val canifis_leapdown = find("rooftops_canifis_leapdown")

    // Falador.
    val falador_wallclimb = find("rooftops_falador_wallclimb")
    val falador_tightrope_1 = find("rooftops_falador_tightrope_1")
    val falador_handholds_start = find("rooftops_falador_handholds_start")
    val falador_gap_1 = find("rooftops_falador_gap_1")
    val falador_gap_2 = find("rooftops_falador_gap_2")
    val falador_tightrope_2 = find("rooftops_falador_tightrope_2")
    val falador_tightrope_3 = find("rooftops_falador_tightrope_3")
    val falador_gap_3 = find("rooftops_falador_gap_3")
    val falador_ledge_1 = find("rooftops_falador_ledge_1")
    val falador_ledge_2 = find("rooftops_falador_ledge_2")
    val falador_ledge_3a = find("rooftops_falador_ledge_3a")
    val falador_ledge_3b = find("rooftops_falador_ledge_3b")
    val falador_ledge_4 = find("rooftops_falador_ledge_4")
    val falador_edge = find("rooftops_falador_edge")

    // Seers' Village.
    val seers_wallclimb = find("rooftops_seers_wallclimb")
    val seers_tightrope = find("rooftops_seers_tightrope")
    val seers_jump = find("rooftops_seers_jump")
    val seers_jump_1 = find("rooftops_seers_jump_1")
    val seers_jump_2 = find("rooftops_seers_jump_2")
    val seers_leapdown = find("rooftops_seers_leapdown")

    // Pollnivneach.
    val pollnivneach_basket = find("rooftops_pollnivneach_basket")
    val pollnivneach_marketstall = find("rooftops_pollnivneach_marketstall")
    val pollnivneach_hangingbanner = find("rooftops_pollnivneach_hangingbanner")
    val pollnivneach_gap = find("rooftops_pollnivneach_gap")
    val pollnivneach_tree = find("rooftops_pollnivneach_tree")
    val pollnivneach_wallclimb = find("rooftops_pollnivneach_wallclimb")
    val pollnivneach_monkeybars_start = find("rooftops_pollnivneach_monkeybars_start")
    val pollnivneach_treetop = find("rooftops_pollnivneach_treetop")
    val pollnivneach_line = find("rooftops_pollnivneach_line")

    // Rellekka.
    val rellekka_wallclimb = find("rooftops_rellekka_wallclimb")
    val rellekka_gap_1 = find("rooftops_rellekka_gap_1")
    val rellekka_tightrope_1 = find("rooftops_rellekka_tightrope_1")
    val rellekka_gap_2 = find("rooftops_rellekka_gap_2")
    val rellekka_gap_3 = find("rooftops_rellekka_gap_3")
    val rellekka_tightrope_3 = find("rooftops_rellekka_tightrope_3")
    val rellekka_dropoff = find("rooftops_rellekka_dropoff")

    // Ardougne.
    val ardy_wallclimb = find("rooftops_ardy_wallclimb")
    val ardy_jump = find("rooftops_ardy_jump")
    val ardy_plank = find("rooftops_ardy_plank")
    val ardy_jump_2 = find("rooftops_ardy_jump_2")
    val ardy_jump_3 = find("rooftops_ardy_jump_3")
    val ardy_wallcrossing = find("rooftops_ardy_wallcrossing")
    val ardy_jump_4 = find("rooftops_ardy_jump_4")

    // Gnome Stronghold. The pipes are two loc types, one per parallel pipe, each placed at both
    // ends; the tree down has two climbable trunks.
    val gnome_log = find("gnome_log_balance1")
    val gnome_net_1 = find("obstical_net2")
    val gnome_branch_up = find("climbing_branch")
    val gnome_rope = find("balancing_rope")
    val gnome_tree_down = find("climbing_tree")
    val gnome_tree_down_2 = find("climbing_tree2")
    val gnome_net_2 = find("obstical_net3")
    val gnome_pipe_1 = find("obstical_pipe3_1")
    val gnome_pipe_2 = find("obstical_pipe3_2")

    // Barbarian Outpost. The crumbling wall is one loc placed three times; the entrance pipe is
    // the level gate into the outpost and not part of the lap.
    val barbarian_pipe = find("agility_obstical_pipe_barbarian")
    val barbarian_ropeswing = find("obstical_ropeswing1")
    val barbarian_log = find("barbarian_log_balance1")
    val barbarian_net = find("agility_obstical_net_barbarian")
    val barbarian_ledge = find("balancing_ledge1")
    val barbarian_ladder_down = find("barbarian_laddertop_norim")
    val barbarian_wall = find("castlecrumbly1")

    // Wilderness. Since the 2024 rework the stepping stones are one loc and one click; the rocks
    // are one loc across three tiles. The gates are plain wall locs with no open variant.
    val wilderness_gate_outer = find("balancegate52a")
    val wilderness_gate_left = find("balancegate52b_left")
    val wilderness_gate_right = find("balancegate52b_right")
    val wilderness_pipe = find("obstical_pipe2")
    val wilderness_ropeswing = find("obstical_ropeswing2")
    val wilderness_stones = find("steppingstone1")
    val wilderness_log = find("wilderness_log_balance1")
    val wilderness_rocks = find("wildclimbingrock")
}
