plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.skillMulti)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.interfaces.skillMulti)
}
