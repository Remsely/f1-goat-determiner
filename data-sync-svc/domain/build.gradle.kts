plugins {
    id("kotlin.lib.convention")
    id("testing.base.convention")
    `java-test-fixtures`
}

description = "Domain layer - pure Kotlin models and interfaces"

dependencies {
    testImplementation(libs.spring.boot.starter.test)
}
