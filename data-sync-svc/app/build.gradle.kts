plugins {
    id("spring.app.convention")
    id("testing.base.convention")
}

dependencies {
    implementation(projects.db)
    implementation(projects.domain)
//    implementation(projects.jolpica)
    implementation(projects.scheduled)
    implementation(projects.useCase)

    implementation(libs.spring.boot.starter)
    implementation(libs.jetbrains.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.test)
}
