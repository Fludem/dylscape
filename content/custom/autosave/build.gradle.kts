plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // `AccountManager`, the same save path a logout uses. Not in plugin-commons.
    implementation(projects.api.account)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies { implementation(projects.api.account) }
    }
}
