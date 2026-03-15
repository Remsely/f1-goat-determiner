# Coding Style

## File Organization

**MANY SMALL FILES > FEW LARGE FILES:**

* High cohesion, low coupling.
* Target 100–200 lines, 300 lines maximum.
* Extract utilities from large components.

## Extensions Instead of Mapper Components

Prefer extension functions over separate mapper classes.

**Good:**

```kotlin
// TssTaskDocumentToDtoExtensions.kt
fun TssTaskDocument.toDto(): TssTaskDocumentDto = TssTaskDocumentDto(
    tssDocumentId = id,
    tecmDocumentId = tecmDocumentId,
    // ... other fields
)

// Usage
val dto = document.toDto()

```

**Bad:**

```kotlin
@Component
class TssTaskDocumentMapper {
    fun toDto(document: TssTaskDocument): TssTaskDocumentDto = TssTaskDocumentDto(...)
}

// Usage
val dto = documentMapper.toDto(document) // Less readable, unnecessary dependency

```

## Kotlin Reified Extensions vs. Java-style Class Parameters

When a library provides a reified inline extension function, **ALWAYS** use it instead of passing `Class<T>` / `KClass<T>` as a parameter. This applies to Spring (`getBean<T>()`, `body<T>()`, `expectBody<T>()`, `queryForObject<T>()`), Jackson (`readValue<T>()`), and any other libraries with Kotlin extensions.

**Good:**

```kotlin
context.getBean<MyService>()
jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM drivers")
responseSpec.body<BlsSignTypeResponse>()
webTestClient.expectBody<String>()
objectMapper.readValue<MyDto>(json)

```

**Bad:**

```kotlin
context.getBean(MyService::class.java)
jdbcTemplate.queryForObject("SELECT count(*) FROM drivers", Long::class.java)
responseSpec.body(BlsSignTypeResponse::class.java)
webTestClient.expectBody(String::class.java)
objectMapper.readValue(json, MyDto::class.java)

```

Use `Class<T>` / `KClass<T>` overloads only when the API does **not** offer a Kotlin extension for that operation.

## No Default Values in Data Classes

**NEVER** use default values in data class constructors. All fields must be explicitly passed by the caller to keep contracts transparent and prevent hidden dependencies.

**Good:**

```kotlin
data class OperationContext(
    val session: SessionInfo,
    val device: DeviceInfo,
    val connection: ConnectionInfo,
)

```

**Bad:**

```kotlin
data class OperationContext(
    val session: SessionInfo,
    val device: DeviceInfo = DeviceInfo(),       // Hides dependency
    val connection: ConnectionInfo = ConnectionInfo(), // Implicit "empty" value
)

```

**Exception — Jackson DTOs only:** `= null` and `= emptyList()` are acceptable in data classes that Jackson deserializes from JSON, where the field may be absent from the response. This does **NOT** apply to domain models, port DTOs, or any other data classes.

```kotlin
// Jackson DTO — exception allowed
data class MRData(
    @JsonProperty("RaceTable")
    val raceTable: RaceTable? = null,   // May be absent in JSON
)

// Domain model — NO defaults allowed
data class DriverStanding(
    val id: Int?,          // Nullable but no default
    val grandPrixId: Int,
    val position: Int?,    // Nullable but no default
)
```

## Named Parameters

When calling methods with **more than 3 parameters**, ALWAYS use named parameters.

**Good:**

```kotlin
documentService.processDocument(
    tssDocumentId = documentId,
    tecmDocumentId = "TECM-456",
    documentType = "CONTRACT",
    format = "PDF",
    priority = Priority.HIGH,
    async = true
)

```

## ID Placement in Data Classes

ID fields must **ALWAYS** come first.

**Good:**

```kotlin
data class TssTaskDocumentDto(
    val tssDocumentId: UUID?,
    val tecmDocumentId: String?,
    // ... other fields
)

```

## Constants

If constants are used in only one class/file, place them above that class and make them private. If constants are used across multiple files, move them to a dedicated file with a meaningful name. Group constants by logical domain/feature.

**NEVER** create a "junk" file for all constants (e.g., `Constants.kt`).

## IDs in Logs and Errors

* **Logs:** DO NOT include IDs—they should already be present in the MDC (Mapped Diagnostic Context).
* **Error API Responses:** ALWAYS include relevant IDs.

**Good:**

```kotlin
// Logging
log.info("Document processing completed") // ID is already in MDC

// API Response
throw NotFoundException("Document not found: tssDocumentId=$tssDocumentId")

```

## Assertions in Tests

Use **Kotest Assertions** (`shouldBe`) for all test checks instead of standard JUnit assertions.

**Good:**

```kotlin
@Test
fun `should create document successfully`() {
    val document = documentService.create(request)
    document.documentName shouldBe "Contract.pdf"
}

```

## Code Quality Checklist

Before finalizing your work:

* [ ] Code is readable with clear naming.
* [ ] Functions are small (< 50 lines).
* [ ] Files are focused (< 800 lines).
* [ ] Proper error handling is implemented.
* [ ] No debug logs remain.
* [ ] No hardcoded values (use constants).
* [ ] ID fields come first in data classes.
* [ ] IDs are excluded from logs but included in error messages.
* [ ] Named parameters are used for 4+ parameters.
* [ ] Kotest assertions are used in tests.
* [ ] Extensions are used instead of mapper components.
* [ ] No implicit default values in data classes (except `= null` in DTOs).
