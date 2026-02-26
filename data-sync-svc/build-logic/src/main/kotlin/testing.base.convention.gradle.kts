import extensions.libs
import extensions.testImplementation
import extensions.testRuntimeOnly

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    testImplementation(platform(libs.kotest.bom))

    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}
