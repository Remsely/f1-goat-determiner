plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "data-sync-svc"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("build-logic")

include(
    "app",
    "db",
    "domain",
    "jolpica",
    "scheduled",
    "use-case",
)
