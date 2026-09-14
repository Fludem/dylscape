plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // Vorkath alternates ranged and magic, and the shared npc driver only fires the one projectile
    // an npc declares in its params, so the two attacks are rolled here off the formulae directly.
    implementation(projects.api.combat.combatFormulas)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            implementation(projects.api.player)
            // `runGameTest` starts only the scripts it is handed; a hit needs the player hit
            // script to land, and the accuracy formulae need the weapon styles initialised.
            implementation(projects.api.combat.combatScripts)
            implementation(projects.api.combat.combatWeapon)
            implementation(projects.api.hitPlugin)
            implementation(projects.api.combat.combatCommons)
            implementation(projects.api.combat.combatFormulas)
            implementation(projects.api.combatMaxhit)
            // `locRegistry` on the advanced test scope: the only source of real map locs.
            implementation(projects.api.registry)
            // `NpcDeathScript`: the kill and back-to-sleep tests need the real respawn path.
            implementation(projects.api.deathPlugin)
            implementation(projects.api.death)
            implementation(projects.api.npc)
            // Proves the generated table still claims the awake form.
            implementation(projects.content.custom.dropTables)
        }
    }
}
