package org.rsmod.content.custom.zulrah

/** The three forms, named as the wiki names them. The cache's names are in `ZulrahNpcs`. */
enum class ZulrahForm {
    /** Green. Ranged attacks, venom clouds and snakelings. Weak to magic. */
    Serpentine,
    /** Red. Stares at a tile and whips its tail down on it. */
    Magma,
    /** Blue. Mostly magic, sometimes ranged. Immune-ish to magic, weak to ranged. */
    Tanzanite,
}

/** Where Zulrah surfaces. The coordinates behind each are in `ZulrahShrine`. */
enum class ZulrahSpot {
    Middle,
    South,
    East,
    West,
}

/** One thing Zulrah does in a phase. Every action takes one attack cycle. */
enum class ZulrahAction {
    /** A ranged, magic or tail attack at the player, depending on the form. */
    Attack,
    /** A venom barrage that leaves a toxic cloud. */
    Cloud,
    /** An egg that hatches into a snakeling. */
    Snakeling,
}

data class ZulrahPhase(val form: ZulrahForm, val spot: ZulrahSpot, val actions: List<ZulrahAction>)

/**
 * The fight's one and only rotation.
 *
 * This is OSRS rotation 1 ("Crimson A") with its Jad phase taken out, and it never changes: every
 * kill runs the same ten phases in the same order and loops back to the first if Zulrah is still
 * alive at the end. Live picks between four rotations after the fourth phase; this server
 * deliberately does not.
 *
 * The magma phases list two attacks each. Live's magma form stares and whips twice.
 */
object ZulrahRotation {
    private val A = ZulrahAction.Attack
    private val C = ZulrahAction.Cloud
    private val S = ZulrahAction.Snakeling

    val phases: List<ZulrahPhase> =
        listOf(
            phase(ZulrahForm.Serpentine, ZulrahSpot.Middle, C, C, C, C),
            phase(ZulrahForm.Magma, ZulrahSpot.Middle, A, A),
            phase(ZulrahForm.Tanzanite, ZulrahSpot.Middle, A, A, A, A),
            phase(ZulrahForm.Serpentine, ZulrahSpot.South, A, A, A, A, A, S, S, C, C, S, S),
            phase(ZulrahForm.Magma, ZulrahSpot.Middle, A, A),
            phase(ZulrahForm.Tanzanite, ZulrahSpot.West, A, A, A, A, A),
            phase(ZulrahForm.Serpentine, ZulrahSpot.South, C, C, C, S, S, S, S),
            phase(ZulrahForm.Tanzanite, ZulrahSpot.South, A, A, A, A, A, S, C, S, C, S),
            phase(ZulrahForm.Magma, ZulrahSpot.Middle, A, A),
            phase(ZulrahForm.Serpentine, ZulrahSpot.Middle, A, A, A, A, A, C, C, C, C),
        )

    /** The phase after [index], wrapping round to the first. */
    fun next(index: Int): Int = (index + 1) % phases.size

    private fun phase(form: ZulrahForm, spot: ZulrahSpot, vararg actions: ZulrahAction) =
        ZulrahPhase(form, spot, actions.toList())
}
