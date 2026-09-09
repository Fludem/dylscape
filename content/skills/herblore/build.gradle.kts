plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.skillMulti)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.interfaces.skillMulti)
    // Integration-only, and only so `PotionCoverageTest` can assert that the `food` table and the
    // `potion` table partition the cache's drinkables. An obj carries exactly one content group, so
    // an overlap would silently break eating or drinking for whichever editor lost the race - and
    // the ale kegs are named like potions, so the overlap is a real risk rather than a theoretical
    // one. No main-source code reaches across.
    integrationImplementation(projects.content.other.consumables)
}
