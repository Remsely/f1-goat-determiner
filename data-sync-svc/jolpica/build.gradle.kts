plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
}

dependencies {
    implementation(projects.domain)
    implementation(projects.useCase)

    implementation(libs.spring.web)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.core)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.logging)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.retry)

    testImplementation(libs.spring.boot.starter.test)
}
