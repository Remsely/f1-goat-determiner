plugins {
    id("spring.app.convention")
    id("testing.base.convention")
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.jetbrains.kotlin.reflect)
    testImplementation(libs.spring.boot.starter.test)
}
