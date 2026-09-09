plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(libs.fastutil)
    // The gating test asserts on the registry combat reads, which lives in combat-commons.
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(libs.fastutil)
}
