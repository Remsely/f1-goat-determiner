plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
    alias(libs.plugins.kotlin.jpa)
}

description = "Database infrastructure - Flyway migrations and JPA repositories"

dependencies {
    implementation(projects.domain)

    api(libs.spring.boot.starter.data.jpa)
    api(libs.spring.boot.starter.data.jdbc)
    api(libs.spring.boot.starter.flyway)

    api(libs.bundles.database)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
}
