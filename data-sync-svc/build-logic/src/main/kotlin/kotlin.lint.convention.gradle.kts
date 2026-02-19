import extensions.detektPlugin
import extensions.libs
import dev.detekt.gradle.extensions.DetektExtension

apply(plugin = libs.plugins.detektV2.plugin.get().pluginId)

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false

    config.setFrom(files("$rootDir/config/detekt/detektV2.yml"))

    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
    )
}

dependencies {
    detektPlugin(libs.detektV2.formatting)
}
