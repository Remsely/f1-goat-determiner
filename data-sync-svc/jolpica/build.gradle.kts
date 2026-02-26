plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
}

dependencies {
    implementation(projects.domain)

    api(libs.spring.boot.starter.web)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.retry)

    testImplementation(libs.spring.boot.starter.test)
}
