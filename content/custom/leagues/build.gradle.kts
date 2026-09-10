plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.content.interfaces.levelup)
    implementation(projects.content.skills.fishing)
    implementation(projects.content.skills.herblore)
    implementation(projects.content.skills.hunter)
    implementation(projects.content.skills.magic.magicCommons)
    implementation(projects.content.skills.mining)
    implementation(projects.content.skills.woodcutting)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.specials)
}
