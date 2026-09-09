package org.rsmod.content.custom.dagannothkings

/**
 * The three kings, and the one thing that differs between them: how they attack.
 *
 * Everything else about the fight falls out of this. The style picks the attack animation, the hunt
 * mode (melee engages through `OpPlayer2`, the other two through `ApPlayer2`), the attack range,
 * and the projectile. It is also the inverse of each king's own weakness -- the king who shoots you
 * is the one you have to walk up to -- which is what makes the room a gear-switching puzzle rather
 * than three health bars.
 */
public enum class DagannothKing(public val displayName: String, public val style: DagannothStyle) {
    Supreme("Dagannoth Supreme", DagannothStyle.Ranged),
    Prime("Dagannoth Prime", DagannothStyle.Magic),
    Rex("Dagannoth Rex", DagannothStyle.Melee);

    public companion object {
        public val all: List<DagannothKing> = entries
    }
}

public enum class DagannothStyle {
    Melee,
    Ranged,
    Magic;

    /** Whether this king holds its distance, and so needs an attack range and a projectile. */
    public val attacksAtRange: Boolean
        get() = this != Melee
}
