plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.levelup)
    implementation(projects.content.skills.magic.magicCommons)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.pluginCommons)
}
