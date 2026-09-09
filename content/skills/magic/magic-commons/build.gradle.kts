plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    implementation(projects.api.spellsAutocast)
    implementation(projects.api.spellsRunes)
    implementation(libs.guice)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.spells)
}
