package extensions

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.the

val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

val Project.javaVersion: JavaVersion
    get() = JavaVersion.toVersion(libs.versions.java.get().toInt())

fun DependencyHandlerScope.implementation(dependencyNotation: Any) =
    add("implementation", dependencyNotation)
