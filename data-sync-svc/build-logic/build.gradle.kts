plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    pluginImplementation(libs.plugins.kotlin.jvm)
    pluginImplementation(libs.plugins.kotlin.spring)
    pluginImplementation(libs.plugins.spring.boot)
    pluginImplementation(libs.plugins.detekt.plugin)

    /* Workaround for version catalog working inside precompiled scripts
    Issue - https://github.com/gradle/gradle/issues/15383 */
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

fun DependencyHandler.pluginImplementation(pluginProvider: Provider<PluginDependency>) =
    add("implementation", pluginProvider.map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
    })
