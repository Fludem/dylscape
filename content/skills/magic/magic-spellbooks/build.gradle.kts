plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    implementation(projects.content.custom.teleports)
    implementation(projects.content.skills.magic.magicCommons)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.spells)
    integrationImplementation(projects.content.skills.magic.magicCommons)
}
