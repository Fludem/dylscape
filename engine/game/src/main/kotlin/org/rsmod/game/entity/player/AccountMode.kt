package org.rsmod.game.entity.player

/**
 * The account type a player picked during first-login setup. Permanent once chosen, except that a
 * [HardcoreIronman] death demotes to [Ironman].
 *
 * [id] doubles as the value written to cache varbit 1777 `ironman`. That domain is fixed by the
 * button order of the cache's own `ironman_setup` interface (890): `none, im, uim, hcim, gim,
 * hcgim, ugim`. We offer only the first four; the group modes are deliberately unsupported.
 *
 * Note that writing the varbit does *not* give the ironman chat badge - `MessagePublicHandler`
 * derives that from the player's mod level, not from this.
 *
 * This lives in the engine rather than in content because the ground-obj pickup rule needs it, and
 * `api/obj-plugin` cannot depend on `content/`.
 */
public enum class AccountMode(public val id: Int) {
    Standard(0),
    Ironman(1),
    UltimateIronman(2),
    HardcoreIronman(3);

    public val isIronman: Boolean
        get() = this != Standard

    public companion object {
        /** Falls back to [Standard] so an unrecognised stored value can never lock an account. */
        public fun of(id: Int): AccountMode = entries.firstOrNull { it.id == id } ?: Standard
    }
}
