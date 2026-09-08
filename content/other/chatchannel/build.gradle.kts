plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(libs.rsprot.api)
    implementation(projects.api.pluginCommons)
}

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            // The tests drive chat through the real `MessagePublic` handler, since the type byte it
            // branches on is the whole point of the feature.
            implementation(projects.api.net)
            implementation(libs.rsprot.api)
        }
    }
}
