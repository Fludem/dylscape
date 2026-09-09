package org.rsmod.content.skills.agility.configs

import org.rsmod.api.type.refs.obj.ObjReferences

/**
 * The Mark of grace and the graceful set it buys.
 *
 * The mark's internal name really is just `grace` (obj 11849) - not `mark_of_grace`, which is the
 * *display* name. Nothing in the repo referenced either before this module.
 *
 * The graceful pieces themselves already work: `params.graceful_restore_rate` is set on them by
 * `ObjEdits.editGracefulSets()` and consumed by `PlayerRunUpdateProcessor`. Until now there was no
 * way to obtain the set.
 */
public object AgilityObjs : ObjReferences() {
    val mark_of_grace = find("grace")

    val graceful_hood = find("graceful_hood")
    val graceful_cape = find("graceful_cape")
    val graceful_top = find("graceful_top")
    val graceful_legs = find("graceful_legs")
    val graceful_gloves = find("graceful_gloves")
    val graceful_boots = find("graceful_boots")
}
