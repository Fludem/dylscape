plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // The reward caskets are drop-tables toml, loaded by its loader and rolled by its roller.
    implementation(projects.content.custom.dropTables)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            implementation(projects.content.custom.dropTables)
            implementation(projects.api.player)
            // The engine's own inventory-on-loc script, so a key can be used on the chest
            // through the real dispatch rather than a hand-published event.
            implementation(projects.api.invPlugin)
        }
    }
}
