plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
}

dependencies {
    implementation(projects.useCase)
    implementation(projects.domain)

    implementation(libs.spring.boot.core)
    implementation(libs.spring.context)
    implementation(libs.kotlin.logging)
    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider.jdbc.template)

    testImplementation(testFixtures(projects.domain))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.postgresql)
    testImplementation(libs.awaitility)
}
