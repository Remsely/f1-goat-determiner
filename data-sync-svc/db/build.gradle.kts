import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
    alias(libs.plugins.kotlin.jpa)
    `java-test-fixtures`
}

description = "Database infrastructure - Flyway migrations and JPA repositories"

dependencies {
    implementation(projects.domain)

    implementation(libs.spring.data.jpa)
    implementation(libs.hibernate.core)
    implementation(libs.spring.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.bundles.database)

    testImplementation(testFixtures(projects.domain))
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway)

    testFixturesImplementation(projects.domain)
    testFixturesImplementation(platform(SpringBootPlugin.BOM_COORDINATES))
    testFixturesImplementation(libs.spring.context)
}
