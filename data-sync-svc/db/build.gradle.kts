plugins {
    id("spring.lib.convention")
    alias(libs.plugins.kotlin.jpa)
}

description = "Database infrastructure - Flyway migrations and JPA repositories"

dependencies {
    implementation(projects.domain)

    api(libs.spring.boot.starter.data.jpa)
    api(libs.spring.boot.starter.data.jdbc)

    api(libs.bundles.database)
}
