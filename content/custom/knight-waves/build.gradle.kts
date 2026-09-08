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
            // The tests drive the squire's dialogue through the real pause-button handler, which is
            // the only way to click "continue" and pick a chat option from inside the harness.
            implementation(projects.api.net)
            implementation(libs.rsprot.api)
            implementation(libs.rsprot.shared)
            // `locRegistry` on the advanced test scope: the only source of real map locs.
            implementation(projects.api.registry)
            // Asserts the unlock actually lands, by asking the prayer book itself.
            implementation(projects.content.interfaces.prayerTab)
        }
    }
}
