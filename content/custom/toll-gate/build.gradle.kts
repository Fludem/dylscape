plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.generic.genericLocs)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            // The tests drive dialogues through the real pause-button handler, which is the only
            // way to click "continue" and pick a chat option from inside the harness.
            implementation(projects.api.net)
            implementation(libs.rsprot.api)
            implementation(libs.rsprot.shared)
        }
    }
}
