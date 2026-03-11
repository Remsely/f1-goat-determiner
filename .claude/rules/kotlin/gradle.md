# Gradle Conventions

## Version Catalog

All external dependencies (libraries and plugins) must be declared in `gradle/libs.versions.toml` and referenced via
`libs.*` accessors. Never hardcode dependency coordinates directly in `build.gradle.kts` files.

**Good:**

```kotlin
// build.gradle.kts
implementation(libs.spring.web)
alias(libs.plugins.kotlin.jpa)
```

**Bad:**

```kotlin
// build.gradle.kts
implementation("org.springframework:spring-web:6.2.7")
id("org.jetbrains.kotlin.plugin.jpa") version "2.3.10"
```

**Exception — built-in Gradle plugins:** Plugins shipped with Gradle itself (`java`, `java-library`,
`java-test-fixtures`, `kotlin-dsl`, `maven-publish`, etc.) have no external version and must be applied using backtick
syntax. They must NOT be added to the version catalog.

```kotlin
// Correct — built-in plugin, no version, backtick syntax
`java-test-fixtures`

// Wrong — built-in plugins cannot be versioned
alias(libs.plugins.java.test.fixtures)
```

## Starters in `app` Only

Spring Boot starters provide auto-configuration and must only be declared in the `app` module. Library modules use plain
Spring libraries instead.

| Module                                   | Use                                                     |
|------------------------------------------|---------------------------------------------------------|
| `app`                                    | `spring-boot-starter-*` (auto-configuration runs here)  |
| `db`, `jolpica`, `use-case`, `scheduled` | `spring-data-jpa`, `spring-web`, `spring-context`, etc. |

**Exception:** `testImplementation` scope allows starters where tests need auto-configuration (e.g.,
`spring-boot-starter-test`, `spring-boot-starter-flyway` for `@DataJpaTest`).

**Good:**

```kotlin
// app/build.gradle.kts
implementation(libs.spring.boot.starter.data.jpa)
implementation(libs.spring.boot.starter.flyway)

// db/build.gradle.kts
implementation(libs.spring.data.jpa)
implementation(libs.hibernate.core)
implementation(libs.flyway.core)
```

**Bad:**

```kotlin
// db/build.gradle.kts
api(libs.spring.boot.starter.data.jpa)  // starter in library module
```

## Virtual Threads and JVM Lifetime

When `spring.threads.virtual.enabled: true` is set, **all** threads (including `@Scheduled` threads) become virtual
threads, which are **daemon threads**. A Spring Boot app without an embedded web server will exit immediately after
the application context starts, because no non-daemon thread keeps the JVM alive.

**Rule:** A scheduled-only Spring Boot app **must** include `spring-boot-starter-web` in `app` to keep the JVM alive
via Tomcat's non-daemon thread. This also enables actuator health/liveness endpoints for container orchestration.

```kotlin
// app/build.gradle.kts — required even for scheduler-only apps with virtual threads
implementation(libs.spring.boot.starter.web)
```
