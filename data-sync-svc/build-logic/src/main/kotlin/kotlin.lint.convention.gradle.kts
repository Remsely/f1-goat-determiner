import extensions.detektPlugin
import extensions.libs
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

apply(plugin = libs.plugins.detekt.plugin.get().pluginId)

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false

    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))

    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
    )
}

dependencies {
    detektPlugin(libs.detekt.formatting)
}
