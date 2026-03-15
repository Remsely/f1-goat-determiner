import extensions.libs
import extensions.testImplementation
import extensions.testRuntimeOnly
import org.springframework.boot.gradle.plugin.SpringBootPlugin

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    testImplementation(platform(SpringBootPlugin.BOM_COORDINATES))
    testImplementation(platform(libs.kotest.bom))

    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}
