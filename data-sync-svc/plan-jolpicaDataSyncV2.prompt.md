# Plan v2: Полная реализация синхронизации данных Jolpica → PostgreSQL (Spring Boot 4)

Проект `data-sync-svc` имеет готовую доменную модель (9 entity types), Flyway-миграции (V001–V011), JPA/JDBC-репозитории
с upsert-логикой и инфраструктуру sync jobs/checkpoints. Модули `jolpica`, `scheduled` и `use-case` — пустые. Нужно:
реализовать HTTP-клиент Jolpica API, бизнес-логику оркестрации sync-джоб и scheduling, с checkpoint-based
отказоустойчивостью.

**Статус**: Шаги 1 и 2 завершены. Доменные интерфейсы вычищены, DAO-классы реализованы с тестами. Миграции исправлены
(enum `SyncEntityType` ↔ CHECK constraint выровнены). `SyncStatus.SKIPPED` добавлен.

## Критические нюансы Spring Boot 4 (Spring Framework 7)

Проект использует **Spring Boot 4.0.2**, **Kotlin 2.3.10**, **Java 21**. Ключевые отличия от Spring Boot 3.x, которые
**обязательно** учитывать:

### Переехавшие пакеты (уже учтено в коде)

| Класс/аннотация              | Старый пакет (Boot 3)                                 | Новый пакет (Boot 4)                                   |
|------------------------------|-------------------------------------------------------|--------------------------------------------------------|
| `@DataJpaTest`               | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `@AutoConfigureTestDatabase` | `org.springframework.boot.test.autoconfigure.jdbc`    | `org.springframework.boot.jdbc.test.autoconfigure`     |
| `FlywayAutoConfiguration`    | `org.springframework.boot.autoconfigure.flyway`       | `org.springframework.boot.flyway.autoconfigure`        |
| `@ImportAutoConfiguration`   | `org.springframework.boot.autoconfigure`              | `org.springframework.boot.autoconfigure`               |

### Правила, которые нужно соблюдать

1. **`@Transactional`** — использовать **только** `org.springframework.transaction.annotation.Transactional`.
   `jakarta.transaction.Transactional` может не подхватываться `TransactionInterceptor` Spring'а в Boot 4.
2. **`@ConfigurationProperties`** — constructor binding работает автоматически для data-классов. Не нужен
   `@ConstructorBinding`. Для автоматической регистрации добавить `@ConfigurationPropertiesScan` на
   `DataSyncApplication` (или явно `@EnableConfigurationProperties`).
3. **`@HttpExchange`** — декларативные HTTP Interface clients — **нативная замена Feign** в Spring Boot 4.
   Не требует Spring Cloud. Используется `RestClient` под капотом через `RestClientAdapter`.
4. **Virtual Threads** — Java 21 + Spring Boot 4 поддерживают `spring.threads.virtual.enabled=true`.
   Для blocking I/O (HTTP + JDBC) это даёт выигрыш без рефакторинга на реактив.
5. **Jackson Kotlin Module** — автоматически регистрируется Spring Boot при наличии Kotlin. DTO data-классы
   десериализуются без дополнительной настройки.
6. **`@Suppress` запрещён** — не использовать `@Suppress` / `@SuppressWarnings`. Если detekt/compiler выдаёт
   предупреждение — исправить причину, а не подавлять.

## Технологические решения

- **HTTP-клиент**: **`@HttpExchange` (HTTP Interface Clients)** — декларативные интерфейсы с аннотациями
  `@GetExchange`, `@PathVariable`, `@RequestParam`. Прокси создаётся через
  `HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(...)`.
  Под капотом — `RestClient` (блокирующий).
  Обоснование: bottleneck — rate limit Jolpica API (~4 req/sec), а не I/O. Checkpoint-based resume требует
  sequential execution. `@Transactional` + JPA работают на ThreadLocal — реактивный стек создал бы трение
  без выигрыша. `@HttpExchange` — нативная альтернатива Feign без зависимости от Spring Cloud.
- **Тестовые ассерты**: Kotest assertions (`shouldBe`, `shouldContainExactly`, `shouldThrow` и т.д.) — во всех модулях.
  Зависимость `io.kotest:kotest-assertions-core` уже в `testing.base.convention`.
- **Мокирование**: MockK для unit-тестов.
- **Интеграционные тесты**: Testcontainers (PostgreSQL) + WireMock.

## ~~Шаг 1. Ревизия доменных интерфейсов~~ ✅ Завершён

## ~~Шаг 2. Реализовать DAO-классы в модуле `db`~~ ✅ Завершён

## Шаг 3. Реализовать HTTP-клиент Jolpica API в модуле `jolpica`

**Что сделать:** Создать типизированный клиент для Jolpica F1 REST API (`https://api.jolpi.ca/ergast/f1/`)
с использованием **`@HttpExchange` HTTP Interface Clients** (Spring Boot 4 нативная альтернатива Feign).

### 3.1 Зависимости

- В `libs.versions.toml` добавить:
  ```toml
  spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
  resilience4j-ratelimiter = { module = "io.github.resilience4j:resilience4j-ratelimiter", version.ref = "resilience4j" }
  resilience4j-retry = { module = "io.github.resilience4j:resilience4j-retry", version.ref = "resilience4j" }
  wiremock-standalone = { module = "org.wiremock:wiremock-standalone", version.ref = "wiremock" }
  ```
- В `jolpica/build.gradle.kts`:
  ```kotlin
  plugins {
      id("spring.lib.convention")
      id("testing.base.convention")
  }
  dependencies {
      implementation(projects.domain)
      implementation(libs.spring.boot.starter.web)
      implementation(libs.resilience4j.ratelimiter)
      implementation(libs.resilience4j.retry)
      testImplementation(libs.wiremock.standalone)
      testImplementation(libs.spring.boot.starter.test)
  }
  ```

### 3.2 Структура пакета

`jolpica/src/main/kotlin/dev/remsely/f1goatdeterminer/datasync/jolpica/`:

#### `config/JolpicaClientProperties.kt`

```kotlin
@ConfigurationProperties(prefix = "jolpica")
data class JolpicaClientProperties(
    val baseUrl: String = "https://api.jolpi.ca/ergast/f1",
    val connectTimeout: Duration = Duration.ofSeconds(10),
    val readTimeout: Duration = Duration.ofSeconds(30),
    val rateLimit: Int = 4,           // requests per second
    val retryMaxAttempts: Int = 3,
    val retryWaitDuration: Duration = Duration.ofSeconds(2),
)
```

**Spring Boot 4 нюанс**: `@ConfigurationProperties` data class — constructor binding работает автоматически.
Добавить `@ConfigurationPropertiesScan` на `DataSyncApplication`.

#### `config/JolpicaClientConfig.kt`

```kotlin
@Configuration
class JolpicaClientConfig {

    @Bean
    fun jolpicaRestClient(
        properties: JolpicaClientProperties,
        rateLimitInterceptor: RateLimitInterceptor,
    ): RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestInterceptor(rateLimitInterceptor)
        .build()

    @Bean
    fun jolpicaApi(restClient: RestClient): JolpicaApi {
        val adapter = RestClientAdapter.create(restClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(JolpicaApi::class.java)
    }
}
```

**Ключевое**: `HttpServiceProxyFactory` + `RestClientAdapter` — Spring Boot 4 способ создания декларативных
HTTP-клиентов. Результат — прокси-объект, реализующий интерфейс `JolpicaApi`.

#### `api/JolpicaApi.kt` — Декларативный HTTP Interface

```kotlin
@HttpExchange
interface JolpicaApi {
    @GetExchange("/circuits.json")
    fun fetchCircuits(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse<CircuitTable>

    @GetExchange("/constructors.json")
    fun fetchConstructors(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse<ConstructorTable>

    @GetExchange("/drivers.json")
    fun fetchDrivers(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse<DriverTable>

    @GetExchange("/status.json")
    fun fetchStatuses(
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse<StatusTable>

    @GetExchange("/{season}.json")
    fun fetchRaces(
        @PathVariable season: Int,
        @RequestParam("limit") limit: Int,
        @RequestParam("offset") offset: Int,
    ): JolpicaResponse<RaceTable>

    @GetExchange("/{season}/{round}/results.json")
    fun fetchResults(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse<RaceTable>

    @GetExchange("/{season}/{round}/qualifying.json")
    fun fetchQualifying(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse<RaceTable>

    @GetExchange("/{season}/{round}/driverStandings.json")
    fun fetchDriverStandings(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse<StandingsTable>

    @GetExchange("/{season}/{round}/constructorStandings.json")
    fun fetchConstructorStandings(
        @PathVariable season: Int,
        @PathVariable round: Int,
    ): JolpicaResponse<StandingsTable>
}
```

**Преимущества `@HttpExchange` над Feign:**

- Нативная поддержка Spring — не нужен Spring Cloud.
- Работает с `RestClient` (blocking) и `WebClient` (reactive) — один интерфейс, разные транспорты.
- Type-safe, декларативный, тестируемый.
- Полная интеграция с Spring Boot actuator, micrometer, error handling.

#### `dto/` — Kotlin data-классы

Файлы: `JolpicaResponse.kt`, `CircuitDto.kt`, `ConstructorDto.kt`, `DriverDto.kt`, `RaceDto.kt`,
`ResultDto.kt`, `QualifyingDto.kt`, `StandingsDto.kt`, `StatusDto.kt`.

Каждый DTO — data class с `@JsonProperty` для маппинга Jolpica JSON-полей.

Обёрточная структура:

```kotlin
data class JolpicaResponse<T>(
    @JsonProperty("MRData") val mrData: MRData<T>,
)

data class MRData<T>(
    val xmlns: String?,
    val series: String?,
    val url: String?,
    val limit: Int,
    val offset: Int,
    val total: Int,
    // Одно из полей будет не null, в зависимости от T:
    @JsonProperty("CircuitTable") val circuitTable: T?,
    // ... другие таблицы
)
```

Альтернативный подход — generic с `@JsonTypeInfo`, но проще — конкретные response-классы для каждого endpoint.

#### `mapper/` — Extension-функции DTO → Domain

```kotlin
fun CircuitDto.toDomain(): Circuit = Circuit(
    id = circuitId.hashCodeDeterministic(),  // или lookup
    ref = circuitId,
    name = circuitName,
    locality = location.locality,
    country = location.country,
)
```

**ID mapping стратегия — вариант B (автогенерация + lookup-кеш)**:

- Убрать ручные PK из миграций V002–V004 (circuits, constructors, drivers), перейти на
  `GENERATED BY DEFAULT AS IDENTITY`.
- При маппинге DTO → Domain: сначала проверить in-memory кеш `Map<String, Int>` (ref → id). Если нет — вставить в БД,
  получить сгенерированный ID, закешировать.
- **Изменение миграций**: т.к. БД пока не используется — можно безопасно изменить существующие V002–V004.

#### `client/JolpicaApiClient.kt`

```kotlin
@Component
class JolpicaApiClient(
    private val api: JolpicaApi,
    private val retry: Retry,
) {
    fun fetchAllCircuits(): Sequence<CircuitDto> = paginatedFetch { offset, limit ->
        Retry.decorateSupplier(retry) { api.fetchCircuits(limit, offset) }.get()
    }

    private fun <T> paginatedFetch(
        pageSize: Int = 100,
        fetcher: (offset: Int, limit: Int) -> JolpicaResponse<T>,
    ): Sequence<T> = sequence {
        var offset = 0
        do {
            val response = fetcher(offset, pageSize)
            val data = response.mrData
            // yield items from data...
            offset += pageSize
        } while (offset < data.total)
    }
}
```

#### `interceptor/RateLimitInterceptor.kt`

```kotlin
@Component
class RateLimitInterceptor(
    properties: JolpicaClientProperties,
) : ClientHttpRequestInterceptor {
    private val rateLimiter = RateLimiter.of(
        "jolpica", RateLimiterConfig.custom()
            .limitForPeriod(properties.rateLimit)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofSeconds(10))
            .build()
    )

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        RateLimiter.waitForPermission(rateLimiter)
        return execution.execute(request, body)
    }
}
```

### 3.3 Тестирование

- **WireMock** для мокирования HTTP: `WireMockExtension` + JUnit 5.
- Тесты: корректный парсинг JSON, пагинация, retry при 5xx, rate limiting.
- Kotest assertions для ассертов.

## Шаг 4. Реализовать бизнес-логику в модуле `use-case`

**Что сделать:** Создать command-ы для оркестрации синхронизации.

### 4.1 Зависимости

- Изменить плагин в `use-case/build.gradle.kts`:
  ```kotlin
  plugins {
      id("spring.lib.convention")
      id("testing.base.convention")
  }
  dependencies {
      implementation(projects.domain)
  }
  ```

### 4.2 Структура пакета

`use-case/src/main/kotlin/dev/remsely/f1goatdeterminer/datasync/usecase/`:

- `sync/SyncOrchestrator.kt` — главный `@Service`, точка входа:
    - Создать `SyncJob` → создать `SyncCheckpoint` для каждого `SyncEntityType.syncOrdered`.
    - Последовательно вызывать `EntitySyncer` для каждого checkpoint.
    - При ошибке — обновить checkpoint (fail), перейти к следующему entity type.
    - В конце — пометить job как `FAILED` или `COMPLETED`.

- `sync/EntitySyncerRegistry.kt` — `@Component`, возвращает нужный `EntitySyncer` по `SyncEntityType`.

- `sync/entity/EntitySyncer.kt` — интерфейс:
  ```kotlin
  interface EntitySyncer {
      val entityType: SyncEntityType
      fun sync(checkpoint: SyncCheckpoint): SyncCheckpoint
  }
  ```

- `sync/entity/` — 9 реализаций: `StatusSyncer`, `CircuitSyncer`, `ConstructorSyncer`, `DriverSyncer`,
  `GrandPrixSyncer`, `RaceResultSyncer`, `QualifyingSyncer`, `DriverStandingSyncer`, `ConstructorStandingSyncer`.
    - **Справочники** (statuses, circuits, constructors, drivers): offset-based paging, upsert batch.
    - **Сезонные данные** (races, results, qualifying, standings): season+round iteration.

- `command/FullSyncCommand.kt` — `@Service`, создаёт `SyncJob(type=FULL)`, вызывает orchestrator.
- `command/IncrementalSyncCommand.kt` — `@Service`, определяет начальную точку из БД.
- `command/ResumeSyncCommand.kt` — `@Service`, находит `FAILED`/`PAUSED` job, возобновляет.

### 4.3 Транзакционность

**Spring Boot 4 нюанс**: `@Transactional` — использовать **только**
`org.springframework.transaction.annotation.Transactional`, **не** `jakarta.transaction.Transactional`.

Каждый upsert-batch оборачивается в `@Transactional`. Checkpoint обновляется **после** успешного коммита.

### 4.4 Тестирование

JUnit 5 + MockK + Kotest assertions. Unit-тесты для каждого Syncer и Orchestrator.

## Шаг 5. Реализовать scheduling в модуле `scheduled`

### 5.1 Зависимости

```kotlin
plugins {
    id("spring.lib.convention")
    id("testing.base.convention")
}
dependencies {
    implementation(projects.useCase)
    implementation(projects.domain)
}
```

### 5.2 Структура

- `SyncScheduler.kt` — `@Component` с `@Scheduled(cron = "...")`:
    - При первом запуске (БД пустая) — `FullSync`.
    - По расписанию — `IncrementalSync`.
    - При старте — проверить `FAILED`/`PAUSED` → `ResumeSync`.

- `SyncSchedulerProperties.kt` — `@ConfigurationProperties(prefix = "sync.schedule")` для cron-выражений.

### 5.3 Конфигурация приложения

- Добавить `@EnableScheduling` и `@ConfigurationPropertiesScan` в `DataSyncApplication`.
- Раскомментировать `implementation(projects.jolpica)` в `app/build.gradle.kts`.
- **Virtual Threads**: добавить `spring.threads.virtual.enabled=true` в `application.yaml`.

### 5.4 Distributed Lock

- ShedLock с `shedlock-spring` + `shedlock-provider-jdbc-template`.
- Flyway миграция V012 для таблицы `shedlock`.
- `@SchedulerLock` на scheduled-методах.

### 5.5 Тестирование

`@SpringBootTest` + Testcontainers + WireMock для end-to-end. Awaitility для асинхронных проверок.

## Шаг 6. Обновить CI pipeline — lint → build → test

**Что сделать:** Создать reusable workflow `verify-kotlin.yml` (lint → build → test).

- Заменить `lint-kotlin.yml` на `verify-kotlin.yml` в `pr-check.yml` и `main-ci.yml`.
- Gradle caching через `gradle/actions/setup-gradle@v3`.
- Testcontainers в CI: GitHub Actions runners имеют Docker.
- Публикация test reports: `actions/upload-artifact@v4`.
- Java 21 (LTS) в CI runners.

## Шаг 7. Обновить copilot-instructions

Добавить секции:

- **Data Sync Architecture**: `SyncOrchestrator` → `EntitySyncer` → `JolpicaApi` (@HttpExchange) → `*Persister`.
- **Module Dependency Graph**: `app` → `scheduled` → `use-case` → `domain` ← `jolpica`, `db`.
- **Spring Boot 4 Guidelines**: переехавшие пакеты, `@Transactional`, `@HttpExchange`, Virtual Threads.
- **Testing Strategy**: Testcontainers + WireMock + MockK + Kotest assertions.
- **Key Patterns**: upsert-all через JDBC batch, checkpoint-based pagination, Resilience4j.
- **CI Pipeline**: `verify-kotlin.yml` (lint → build → test).

## Зависимости для добавления в `libs.versions.toml`

```toml
[versions]
resilience4j = "2.3.0"    # Проверить актуальную версию
wiremock = "3.13.0"        # Проверить актуальную версию
shedlock = "6.8.0"         # Проверить актуальную версию
awaitility = "4.3.0"       # Проверить актуальную версию

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
resilience4j-ratelimiter = { module = "io.github.resilience4j:resilience4j-ratelimiter", version.ref = "resilience4j" }
resilience4j-retry = { module = "io.github.resilience4j:resilience4j-retry", version.ref = "resilience4j" }
wiremock-standalone = { module = "org.wiremock:wiremock-standalone", version.ref = "wiremock" }
shedlock-spring = { module = "net.javacrumbs.shedlock:shedlock-spring", version.ref = "shedlock" }
shedlock-provider-jdbc-template = { module = "net.javacrumbs.shedlock:shedlock-provider-jdbc-template", version.ref = "shedlock" }
awaitility = { module = "org.awaitility:awaitility-kotlin", version.ref = "awaitility" }
```

## Дополнительные соображения

1. **`@HttpExchange` vs Feign vs `RestClient`**: `@HttpExchange` — **однозначный выбор** для Spring Boot 4.
   Нативная поддержка, декларативный стиль (как Feign), без зависимости от Spring Cloud.
   Используется `RestClient` под капотом. Полная интеграция с Spring экосистемой.

2. **Virtual Threads (Java 21 + Spring Boot 4)**: `spring.threads.virtual.enabled=true` — рекомендуется
   включить. Для blocking I/O (HTTP-клиент + JDBC) даёт лёгкий выигрыш без рефакторинга на реактив.
   `@Scheduled` задачи также будут выполняться на виртуальных потоках.

3. **Jolpica ID mapping — вариант B (автогенерация + кеш)**: Jolpica API отдаёт slug-ref (`hamilton`, `monza`).
   Нужна стратегия генерации `Int` ID. Рекомендация — автогенерация в БД + in-memory lookup-кеш по ref.
   Т.к. БД пока не используется — безопасно изменить существующие миграции V002–V004 (убрать ручные PK,
   перейти на `GENERATED BY DEFAULT AS IDENTITY`).

4. **`@Suppress` запрещён**: Если detekt/compiler выдаёт warning — исправить причину. Пример: `SpreadOperator`
   в `main()` — можно использовать `runApplication<DataSyncApplication>(*args)` с альтернативной перегрузкой
   без varargs, или настроить detekt для исключения main-функции.
