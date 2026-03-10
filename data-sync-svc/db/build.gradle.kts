plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
    alias(libs.plugins.kotlin.jpa)
    `java-test-fixtures`
}

description = "Database infrastructure - Flyway migrations and JPA repositories"

dependencies {
    implementation(projects.domain)

    api(libs.spring.boot.starter.data.jpa)
    api(libs.spring.boot.starter.data.jdbc)
    api(libs.spring.boot.starter.flyway)

    api(libs.bundles.database)

    testImplementation(testFixtures(projects.domain))
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)

    testFixturesImplementation(projects.domain)
    testFixturesImplementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}
