plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            implementation(projects.api.player)
            // `runGameTest` starts only the scripts it is handed, so the combat suite has to name
            // the npc attack and retaliation scripts explicitly to have any handler at all.
            implementation(projects.api.combat.combatScripts)
            // `WeaponAttackStylesScript` initialises `AttackStyles`, whose `weaponStyles` map the
            // accuracy formulae read; without it the first roll throws on an uninitialised lateinit.
            implementation(projects.api.combat.combatWeapon)
            // `PlayerHitScript` is what actually applies a queued hit to the player. Without it
            // the driver rolls its damage, queues it, and nothing ever takes the hitpoints off.
            implementation(projects.api.hitPlugin)
            // `Npc.queueCombatRetaliate`, to drive the retaliation branch directly.
            implementation(projects.api.combat.combatCommons)
            // `NpcMeleeMaxHit` and friends: the config test re-derives each king's max hit rather
            // than trusting that no strength bonus crept into the editor.
            implementation(projects.api.combatMaxhit)
            // `locRegistry` on the advanced test scope: the only source of real map locs, which is
            // how the two ladder placements are checked rather than assumed.
            implementation(projects.api.registry)
            // The generated drop tables are what make this module's lack of a death handler
            // correct, so the test that proves they claim all three kings needs the loader.
            implementation(projects.content.custom.dropTables)
        }
    }
}
