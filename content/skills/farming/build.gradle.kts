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
}
