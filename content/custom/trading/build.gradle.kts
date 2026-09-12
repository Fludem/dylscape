plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // `onOpPlayer4`: the "Trade with" op is a player op, which lives in the advanced script api.
    implementation(projects.api.scriptAdvanced)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            implementation(projects.api.player)
            implementation(projects.api.scriptAdvanced)
        }
    }
}
