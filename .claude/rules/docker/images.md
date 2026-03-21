# Docker Image Rules

## Pin Exact Versions

Always pin exact tool versions in Dockerfiles, matching the project configuration files.

**Check these sources before writing a Dockerfile:**

| Tool     | Config file                                | Example tag                     |
|----------|--------------------------------------------|---------------------------------|
| Gradle   | `gradle/wrapper/gradle-wrapper.properties` | `gradle:9.3.0-jdk21-alpine`     |
| Node/Bun | `package.json`, `bun.lock`                 | `oven/bun:1-alpine`             |
| JDK      | `build.gradle.kts` (toolchain)             | `eclipse-temurin:21-jre-alpine` |

**Never** use floating tags like `latest`, `jdk21`, or major-only versions (e.g., `gradle:9-jdk21-alpine`).

## Use Native Tool Images

Use the official image for the primary build tool, not a generic base + manual install.

**Good:**

```dockerfile
FROM oven/bun:1-alpine AS builder
```

**Bad:**

```dockerfile
FROM node:20-alpine AS builder
RUN npm install -g bun
```

**Good:**

```dockerfile
FROM gradle:9.3.0-jdk21-alpine AS builder
```

**Bad:**

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache bash
COPY gradlew ...
```

## Research Before Assuming

If unsure whether an official Docker image exists for a tool/version, **check Docker Hub first** (via web
search) before falling back to workarounds. Official images are almost always preferable.

## .dockerignore Placement

Docker reads `.dockerignore` from the **build context root**, not from the Dockerfile directory.
If `context: ./` (repo root), the `.dockerignore` must be at the repo root.

## Multi-stage Builds: Minimize Runtime Image

- **Builder stage**: use a full SDK/JDK image with build tools.
- **Runtime stage**: use the smallest possible image (`-alpine`, `-jre`).

## Spring Boot JAR Selection

Spring Boot generates both a boot JAR and a plain JAR by default. Before using `*.jar` wildcards in COPY:

1. Check if plain jar is disabled (`tasks.named<Jar>("jar") { enabled = false }`).
2. If not disabled, use a specific pattern or disable it.

## Gradle in Docker: Copy gradle.properties

Always copy `gradle.properties` into the builder stage — it contains JVM args, parallelism, and caching settings
that affect build performance and correctness.
