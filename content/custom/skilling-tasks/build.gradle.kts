plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.account)
    implementation(projects.api.db)
    implementation(projects.api.pluginCommons)
    // The skills whose events the progress script listens on.
    implementation(projects.content.skills.cooking)
    implementation(projects.content.skills.crafting)
    implementation(projects.content.skills.fishing)
    implementation(projects.content.skills.fletching)
    implementation(projects.content.skills.herblore)
    implementation(projects.content.skills.mining)
    implementation(projects.content.skills.smithing)
    implementation(projects.content.skills.woodcutting)

    // `plugin-commons` is an `implementation` dependency, so nothing it pulls in reaches the
    // integration source set; the suites name what they touch.
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.content.skills.cooking)
    integrationImplementation(projects.content.skills.fishing)
    integrationImplementation(projects.content.skills.mining)
    integrationImplementation(projects.content.skills.smithing)
    integrationImplementation(projects.content.skills.woodcutting)
}
