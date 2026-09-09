plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.account)
    implementation(projects.api.db)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.registry)
    // Only so the multiloc regression test can register the engine's own inventory-on-loc
    // handler: the trigger it resolves is the whole point of the test.
    integrationImplementation(projects.api.invPlugin)
}
