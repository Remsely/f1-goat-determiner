# Plan: Полная реализация синхронизации данных Jolpica → PostgreSQL

Проект `data-sync-svc` имеет готовую доменную модель (9 entity types), Flyway-миграции (V001–V011), JPA/JDBC-репозитории
с upsert-логикой и инфраструктуру sync jobs/checkpoints. Модули `jolpica`, `scheduled` и `use-case` — пустые. Нужно:
реализовать HTTP-клиент Jolpica API, бизнес-логику оркестрации sync-джоб и scheduling, с checkpoint-based
отказоустойчивостью.

## Технологические решения

- **HTTP-клиент**: Spring `RestClient` (блокирующий). Обоснование: bottleneck — rate limit Jolpica API (~4 req/sec), а
  не I/O. Checkpoint-based resume требует sequential execution. `@Transactional` + JPA работают на ThreadLocal —
  реактивный стек создал бы трение без выигрыша в производительности.
- **Тестовые ассерты**: Kotest assertions (`shouldBe`, `shouldContainExactly`, `shouldThrow` и т.д.) — во всех модулях.
  Добавить зависимость `io.kotest:kotest-assertions-core` в `testing.base.convention`.
- **Мокирование**: MockK для unit-тестов.
- **Интеграционные тесты**: Testcontainers (PostgreSQL) + WireMock.

## Шаг 1. Ревизия доменных интерфейсов — убрать лишнее, добавить недостающее

**Что сделать:** Пройтись по всем `Finder`/`Persister` интерфейсам в `domain` и оставить только методы, нужные для
sync-джоб.

- **Persister-ы** — джобам нужен только `upsertAll()`. Убрать `save()` и `saveAll()` из всех Persister-интерфейсов (
  `CircuitPersister`, `ConstructorPersister`, `DriverPersister`, `GrandPrixPersister`, `StatusPersister`,
  `RaceResultPersister`, `QualifyingResultPersister`, `DriverStandingPersister`, `ConstructorStandingPersister`).
- **Finder-ы** — для инкрементальной синхронизации нужны: `count()` (определить, нужна ли full sync),
  `GrandPrixFinder.findAllSeasons()` и `GrandPrixFinder.findMaxRoundBySeason()` (определить, с какого сезона/раунда
  продолжить). Убрать `findByRef`, `findByNationality`, `findByDriverId`, `findByGrandPrixId`, `findByConstructorId`,
  `findFinalStandingsBySeason` и аналогичные — они не используются sync-логикой. Оставить `findById` там, где нужен
  lookup.
- Добавить в `SyncCheckpoint` поддержку состояния `SKIPPED` (уже есть в CHECK constraint миграции V011, но отсутствует в
  enum `SyncStatus`).
- **Несоответствие**: в миграции V011 `entity_type` CHECK содержит `RACES` и `RESULTS`, а в `SyncEntityType` —
  `GRAND_PRIX`, `RACE_RESULTS`. Нужно выровнять миграцию (V012) или enum.

## Шаг 2. Реализовать DAO-классы в модуле `db`

**Что сделать:** Все 11 DAO-классов сейчас пустые (реализуют интерфейсы без тела). Имплементировать методы, делегируя в
JPA/JDBC-репозитории.

- Каждый `*Dao` делегирует `upsertAll` → `*JdbcRepository.upsertAll()`, а Finder-методы → `*JpaRepository` с маппингом
  Entity↔Domain через существующие extension-функции (`toDomain()`/`toEntity()`).
- `SyncJobDao` — делегировать в `SyncJobJpaRepository` (save, updateStatus, updateProgress, complete, find\*). Все
  `@Modifying` queries уже готовы.
- `SyncCheckpointDao` — делегировать в `SyncCheckpointJpaRepository`.
- **Технологии**: Spring Data JPA для reads, JdbcTemplate `batchUpdate` для bulk upserts (уже заложено в
  `*JdbcRepository`).
- **Тестирование**: `@DataJpaTest` + Testcontainers (PostgreSQL). Использовать `@Sql` для подготовки данных. Ассерты —
  Kotest (`result.shouldBe(expected)`, `list.shouldHaveSize(5)`).

## Шаг 3. Реализовать HTTP-клиент Jolpica API в модуле `jolpica`

**Что сделать:** Создать типизированный клиент для Jolpica F1 REST API (`https://api.jolpi.ca/ergast/f1/`).

- **HTTP-клиент**: Spring `RestClient` (Spring Boot 4.x, встроен — не нужны доп. зависимости). Добавить зависимость
  `spring-boot-starter-web` в `jolpica/build.gradle.kts` и `implementation(projects.domain)`.
- **Структура пакета** `jolpica/src/main/kotlin/dev/remsely/f1goatdeterminer/datasync/jolpica/`:
    - `config/JolpicaClientConfig.kt` — `@Configuration` с `@ConfigurationProperties` для base URL, timeout, rate limit.
    - `dto/` — Kotlin data-классы для JSON-ответов Jolpica API (обёртка `MRData` → `*Table` → `*` для каждого типа:
      circuits, constructors, drivers, races, results, qualifying, driverStandings, constructorStandings, status).
    - `mapper/` — маппинг DTO → Domain models. Jolpica отдаёт string-ID, нужен маппинг в int-ID (Jolpica возвращает
      числовые ID в URL, их можно парсить).
    - `client/JolpicaApiClient.kt` — `@Component` с методами: `fetchSeasons()`, `fetchRaces(season, offset, limit)`,
      `fetchResults(season, round)`, `fetchQualifying(season, round)`, `fetchDriverStandings(season, round)`,
      `fetchConstructorStandings(season, round)`, `fetchDrivers(offset, limit)`, `fetchConstructors(offset, limit)`,
      `fetchCircuits(offset, limit)`, `fetchStatuses(offset, limit)`.
    - `client/RateLimiter.kt` — простой rate limiter (Jolpica имеет лимит ~4 req/sec). Использовать Resilience4j
      `RateLimiter` — добавить зависимость.
    - `client/RetryableClient.kt` — обёртка с Resilience4j `Retry` (exponential backoff, 3 попытки).
- **Пагинация**: Jolpica API возвращает `MRData.total`, `MRData.limit`, `MRData.offset`. Клиент должен итерировать по
  страницам.
- **Тестирование**: WireMock для мокирования HTTP-ответов + JUnit 5 + Kotest assertions. Проверки: корректный парсинг
  JSON (`response.drivers.shouldHaveSize(30)`), пагинация, retry при 5xx, rate limiting.

## Шаг 4. Реализовать бизнес-логику в модуле `use-case`

**Что сделать:** Создать command-ы для оркестрации синхронизации.

- Изменить плагин в `use-case/build.gradle.kts` на `spring.lib.convention` (нужен Spring для `@Service`,
  `@Transactional`). Добавить `implementation(projects.domain)`.
- **Структура пакета** `use-case/src/main/kotlin/dev/remsely/f1goatdeterminer/datasync/usecase/`:
    - `sync/SyncOrchestrator.kt` — главный `@Service`, точка входа. Логика: создать `SyncJob` → создать `SyncCheckpoint`
      для каждого `SyncEntityType.syncOrdered` → последовательно вызывать `EntitySyncer` для каждого checkpoint. При
      ошибке — обновить checkpoint (fail), перейти к следующему entity type, в конце — пометить job как `FAILED` или
      `COMPLETED`.
    - `sync/EntitySyncerRegistry.kt` — `@Component`, возвращает нужный `EntitySyncer` по `SyncEntityType`.
    - `sync/entity/EntitySyncer.kt` — интерфейс с методом `fun sync(checkpoint: SyncCheckpoint): SyncCheckpoint`.
    - `sync/entity/` — 9 реализаций: `StatusSyncer`, `CircuitSyncer`, `ConstructorSyncer`, `DriverSyncer`,
      `GrandPrixSyncer`, `RaceResultSyncer`, `QualifyingSyncer`, `DriverStandingSyncer`, `ConstructorStandingSyncer`.
    - **Справочники** (statuses, circuits, constructors, drivers): Стратегия «offset-based paging» — fetch all с
      пагинацией, upsert batch. Checkpoint сохраняет `lastOffset`.
    - **Сезонные данные** (races, results, qualifying, standings): Стратегия «season+round iteration» — перебор сезонов
      от 1950 (или `lastSeason` checkpoint-а) до текущего года, раундов от 1 (или `lastRound`) до max. Checkpoint
      сохраняет `lastSeason`/`lastRound`.
    - `command/FullSync.kt` — `@Service`, создаёт `SyncJob(type=FULL)`, вызывает orchestrator.
    - `command/IncrementalSync.kt` — `@Service`, создаёт `SyncJob(type=INCREMENTAL)`, определяет начальную точку (
      последний завершённый сезон/раунд из БД), вызывает orchestrator только для новых данных.
    - `command/ResumeSync.kt` — `@Service`, находит `FAILED`/`PAUSED` job, возобновляет с последнего checkpoint-а.
- **Отказоустойчивость**: каждый upsert-batch оборачивается в `@Transactional`. Checkpoint обновляется **после**
  успешного коммита. При падении — checkpoint хранит точку для resume.
- **Тестирование**: JUnit 5 + MockK + Kotest assertions. Unit-тесты для каждого Syncer (
  `syncer.sync(checkpoint).status.shouldBe(SyncStatus.COMPLETED)`) и Orchestrator. Verify-вызовы через MockK (
  `verify { persister.upsertAll(any()) }`).

## Шаг 5. Реализовать scheduling в модуле `scheduled`

**Что сделать:** Spring Scheduling для автоматического запуска sync-джоб.

- Добавить `implementation(projects.useCase)` и `implementation(projects.domain)` в `scheduled/build.gradle.kts`.
- **Структура пакета** `scheduled/src/main/kotlin/dev/remsely/f1goatdeterminer/datasync/scheduled/`:
    - `SyncScheduler.kt` — `@Component` с `@Scheduled(cron = "...")`:
        - При первом запуске (БД пустая) — запустить `FullSync`.
        - По расписанию (например, каждый понедельник ночью во время сезона, или ежедневно) — `IncrementalSync`.
        - При старте приложения — проверить наличие `FAILED`/`PAUSED` джоб → `ResumeSync`.
    - `SyncSchedulerConfig.kt` — `@ConfigurationProperties(prefix = "sync.schedule")` для cron-выражений.
- Добавить `@EnableScheduling` в `DataSyncApplication`.
- Раскомментировать `implementation(projects.jolpica)` в `app/build.gradle.kts`.
- **Конкурентность**: использовать `@Scheduled` с `@SchedulerLock` (ShedLock с JDBC-провайдером) для защиты от
  параллельного запуска (добавить миграцию V012 для таблицы `shedlock`).
- **Тестирование**: `@SpringBootTest` + Testcontainers + WireMock для end-to-end теста полного цикла. Awaitility для
  асинхронных проверок. Kotest assertions для итоговых проверок состояния БД.

## Шаг 6. Обновить CI pipeline — lint → build → test

**Текущее состояние:**
- `pr-check.yml` — только lint (4 сервиса), нет build/test.
- `main-ci.yml` — lint → Docker build & push. Для `data-sync-svc` Docker build отключен (`if: false`).
- Lint-ы уже вынесены в reusable workflows (`lint-*.yml`).
- Цепочка `detect-changes → lint` дублируется между PR и main.

**Что сделать:** Создать reusable workflow `verify-kotlin.yml` для полной цепочки lint → build → test. Использовать его
в обоих пайплайнах.

- **Новый reusable workflow** `.github/workflows/verify-kotlin.yml`:
  - Input: `working-directory`.
  - Jobs (sequential):
    1. **lint** — `./gradlew detekt` (перенести логику из `lint-kotlin.yml`, сам `lint-kotlin.yml` удалить или
       оставить как алиас для обратной совместимости).
    2. **build** — `./gradlew assemble -x test` (компиляция без тестов, быстрая проверка что код собирается).
    3. **test** — `./gradlew test` с Testcontainers. Нужен Docker в runner'e — использовать `services` или просто
       Testcontainers с встроенным Docker (GitHub Actions runners уже имеют Docker). Кеширование Gradle через
       `gradle/actions/setup-gradle@v3`.
  - Публикация test reports: `actions/upload-artifact@v4` для HTML-отчётов, `EnricoMi/publish-unit-test-result-action`
    для красивых PR-комментариев с результатами тестов.
  - Gradle caching: `setup-gradle` уже кеширует `.gradle/caches` и wrapper. Добавить `cache-read-only: true` для PR,
    `cache-read-only: false` для main.

- **Обновить `pr-check.yml`:**
  - Заменить `uses: ./.github/workflows/lint-kotlin.yml` на `uses: ./.github/workflows/verify-kotlin.yml`.
  - Добавить `data-sync-build-test` job аналогично, или вызвать один `verify-kotlin.yml` который делает всё.
  - Добавить результаты `data-sync-verify` в `pr-check-status`.

- **Обновить `main-ci.yml`:**
  - Заменить `data-sync-lint` на `uses: ./.github/workflows/verify-kotlin.yml`.
  - `data-sync-build` (Docker build & push) — теперь `needs: data-sync-verify` вместо `needs: data-sync-lint`.
    Убрать `if: false` когда Dockerfile будет готов.

- **Опционально** — аналогичный подход для Python (`verify-python.yml`: ruff → pytest) и TypeScript
  (`verify-typescript.yml`: eslint+prettier → build → vitest), когда в этих сервисах появятся тесты.

- **Java-версия**: в `lint-kotlin.yml` сейчас `java-version: '21'`, а в `libs.versions.toml` указана `java = "25"`.
  Выровнять на Java 25 (или на LTS 21 — зависит от целевой платформы). Использовать `java-version` из input параметра
  reusable workflow для гибкости.

## Шаг 7. Обновить конфигурацию и copilot-instructions

**Что сделать:** После реализации фичи обновить `.github/copilot-instructions.md`.

- **Стратегия обновления**: после завершения каждого шага дополнять copilot-instructions разделом с описанием
  реализованных компонентов — это лучшая стратегия «progressive enrichment».
- Добавить секцию **«Data Sync Architecture»** с описанием: `SyncOrchestrator` → `EntitySyncer` → `JolpicaApiClient` →
  `*Persister`. Checkpoint-based resumability.
- Добавить секцию **«Module Dependency Graph»**: `app` → `scheduled` → `use-case` → `domain` ← `jolpica`, `db`.
- Добавить секцию **«Testing Strategy»**: Testcontainers + WireMock + MockK + **Kotest assertions**. Указать конвенции
  именования тестов.
- Добавить секцию **«Key Patterns»**: upsert-all через JDBC batch, checkpoint-based pagination, Resilience4j rate
  limiting/retry.
- Добавить секцию **«Jolpica API Reference»**: эндпоинты, структура ответов, лимиты.
- Добавить описание конфигурируемых property-ключей (`sync.schedule.cron`, `jolpica.base-url`, `jolpica.rate-limit`).
- Добавить секцию **«CI Pipeline»**: reusable workflow `verify-kotlin.yml` (lint → build → test), связь с
  `pr-check.yml` и `main-ci.yml`. Указать что тесты используют Testcontainers и требуют Docker в runner'e.

## Дополнительные соображения

1. **Несоответствие enum ↔ migration**: `SyncEntityType` содержит `GRAND_PRIX`, `RACE_RESULTS`, `QUALIFYING_RESULTS`, а
   CHECK constraint в V011 содержит `RACES`, `RESULTS`, `QUALIFYING`. Нужна миграция V012 для выравнивания, или
   переименование enum-значений. Рекомендация — **миграция V012**, т.к. доменные имена более выразительны.
2. **Jolpica ID mapping**: Jolpica API отдаёт данные без числовых ID (используются slug-ref: `hamilton`, `monza`). Нужна
   стратегия генерации `Int` ID — **вариант A**: хешировать ref → deterministic ID / **вариант B**: автогенерация в БД +
   lookup-кеш по ref / **вариант C**: парсить URL-ы из Jolpica, которые содержат Ergast-совместимые числовые ID.
   Рекомендация — **вариант B** (автогенерация + кеш), но нужно обновить миграции — убрать ручные PK, перейти на
   `GENERATED BY DEFAULT AS IDENTITY`.
3. **Зависимости для добавления в `libs.versions.toml`**: `resilience4j-ratelimiter`, `resilience4j-retry`, `wiremock`,
   `mockk`, `testcontainers`, `shedlock-spring` + `shedlock-provider-jdbc-template`, `awaitility`,
   `kotest-assertions-core`.
4. **Paths-filter**: в `detect-changes` обоих пайплайнов отсутствует `data-sync-svc/jolpica/**` — добавить при
   реализации шага 6. Также добавить ссылку на новый `verify-kotlin.yml` в фильтры.
