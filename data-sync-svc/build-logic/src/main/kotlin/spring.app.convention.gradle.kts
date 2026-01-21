import extensions.libs
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("spring.lib.convention")
}

apply(plugin = libs.plugins.spring.boot.get().pluginId)

tasks.withType<BootJar> {
    mainClass.set("dev.remsely.f1goatdeterminer.datasync.app.DataSyncApplication")
}

tasks.named<Jar>("jar") {
    enabled = false
}
