plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // Zulrah rolls its own attacks from a phase script rather than going through `NvPCombat`, so
    // the ranged and magic formulae are called directly.
    implementation(projects.api.combat.combatFormulas)
    // Venom, which nothing else in the game inflicted before this module.
    implementation(projects.api.toxins)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            implementation(projects.api.player)
            // `runGameTest` starts only the scripts it is handed, and the fight's hits need the
            // player hit script to land at all.
            implementation(projects.api.combat.combatScripts)
            implementation(projects.api.combat.combatWeapon)
            implementation(projects.api.hitPlugin)
            implementation(projects.api.combat.combatCommons)
            implementation(projects.api.combat.combatFormulas)
            // `locRegistry` on the advanced test scope: the only source of real map locs.
            implementation(projects.api.registry)
            implementation(projects.api.toxins)
            // `NpcDeathScript`: the kill test needs a real death sequence to publish `Killed`.
            implementation(projects.api.deathPlugin)
            implementation(projects.api.death)
            implementation(projects.api.npc)
            // Proves the generated table still claims all three forms.
            implementation(projects.content.custom.dropTables)
        }
    }
}
