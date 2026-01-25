package extensions

import org.gradle.api.Project

fun Project.detektPlugin(dependencyNotation: Any) {
    dependencies.add("detektPlugins", dependencyNotation)
}
