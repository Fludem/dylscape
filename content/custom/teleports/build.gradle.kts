plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies { implementation(projects.api.pluginCommons) }

testing.suites {
    named<JvmTestSuite>("integration") {
        dependencies {
            // `menu` suspends on a pause-button resume, so the tests need the real
            // `ResumePauseButtonHandler` to pick a row. Same reason as `toll-gate`.
            implementation(projects.api.net)
            implementation(libs.rsprot.api)
            implementation(libs.rsprot.shared)
        }
    }
}
