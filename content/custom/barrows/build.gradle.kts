plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // Ahrim and Karil roll their own attacks. `NvPCombat` only ever builds a
    // `CombatAttack.NpcMelee`, so the ranged and magic formulae are called directly; they are
    // public and complete, it is only the dispatcher that is melee-only.
    implementation(projects.api.combat.combatFormulas)
    // The chest's consolation half reuses the weighted-table dsl and roller, whose builder asserts
    // that slot weights sum to their denominator. The potential-driven part - how many rolls, and
    // whether a roll reaches the equipment table - is barrows' own and lives in `BarrowsChest`.
    implementation(projects.content.custom.dropTables)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            // `locRegistry` on the advanced test scope: the only source of real map locs, which is
            // what the coordinate table is validated against.
            implementation(projects.api.registry)
            implementation(projects.api.combat.combatFormulas)
            implementation(projects.content.custom.dropTables)
        }
    }
}
