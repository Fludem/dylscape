plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.specials)
    implementation(projects.content.skills.magic.magicCommons)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.specials)
    integrationImplementation(projects.content.skills.magic.magicCommons)
}
