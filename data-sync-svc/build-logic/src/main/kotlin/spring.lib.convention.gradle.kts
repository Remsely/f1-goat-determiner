import extensions.implementation
import extensions.libs
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("kotlin.lib.convention")
}

apply(plugin = libs.plugins.kotlin.spring.get().pluginId)

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
}
