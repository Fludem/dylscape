plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.account)
    implementation(projects.api.db)
    implementation(projects.content.interfaces.skillMulti)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.registry)
    integrationImplementation(projects.content.interfaces.skillMulti)
}
