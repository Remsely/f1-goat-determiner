plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
}

dependencies {
    implementation(projects.domain)

    implementation(libs.kotlin.logging)
    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    testImplementation(libs.spring.boot.starter.test)
}
