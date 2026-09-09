plugins {
    id("base-conventions")
    id("integration-test-suite")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.fastutil)
    implementation(libs.openrs2.buffer)
    implementation(libs.openrs2.cache)
    integrationImplementation(libs.openrs2.buffer)
    integrationImplementation(libs.openrs2.cache)
    // `locRegistry` on the advanced test scope: the only source of real map locs, which the
    // barrows placement dump needs.
    integrationImplementation(projects.api.registry)
    implementation(projects.api.repo)
    implementation(projects.api.type.typeSymbols)
    implementation(projects.engine.annotations)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.module)
    implementation(projects.engine.routefinder)
}
