plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // Akutha's counter stocks the tablet list `TeletabScript` binds `Break` on, so it cannot sell
    // one that does nothing.
    implementation(projects.content.skills.magic.magicSpellbooks)

    // `plugin-commons` is an `implementation` dependency, so nothing it pulls in reaches the
    // integration source set. `SkillcapeShopTest` prices a cape with the engine's own formula and
    // equips one through the engine's own op, so it needs both modules by name.
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.content.skills.magic.magicSpellbooks)
}
