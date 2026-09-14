package org.rsmod.content.custom.skillingtasks

import jakarta.inject.Inject
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Everything the suites pull out of the game injector. */
class SkillingTaskTestDeps
@Inject
constructor(
    val state: SkillingTaskState,
    val assigner: SkillingTaskAssigner,
    val collision: CollisionFlagMap,
)
