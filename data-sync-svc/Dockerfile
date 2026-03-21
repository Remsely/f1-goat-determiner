FROM gradle:9.3.0-jdk21-alpine AS builder

WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY build-logic/ build-logic/
COPY app/ app/
COPY db/ db/
COPY domain/ domain/
COPY jolpica/ jolpica/
COPY scheduled/ scheduled/
COPY use-case/ use-case/
COPY config/ config/

RUN gradle :app:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/app/build/libs/app-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
