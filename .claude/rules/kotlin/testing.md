# Testing Requirements

## Minimum Test Coverage: 65%

The following test types are **mandatory**:

1. **Unit Tests** – Individual functions, utilities, and components.
2. **Integration Tests** – API endpoints, database operations, and external service integrations.
3. **E2E Tests** – Critical user scenarios.

## Testing Stack

* **Runner:** JUnit 6
* **Mocking:** MockK (`mockk()`, `every { }`, `verify { }`, `slot<T>()`)
* **Assertions:** **Kotest assertions only** (`shouldBe`, `shouldNotBeNull()`, `shouldThrow<T> { }`, etc.). Do **not** use JUnit assertions like `assertEquals`, `assertTrue`, or `assertThrows` in project tests.

## Reuse of Test Utilities

Before writing a new test:

1. **Check `support/**` – It may contain ready-to-use fixtures, helpers, and mocks.
2. **Do Not Duplicate** – If a fixture or helper already exists, use it instead of creating a new one.

When writing tests:

1. **Move reusable code to `support/**` – This includes test object factories, common mock stubs, and extensions for `MockMvc`/`WebTestClient`.
2. **Criterion** – If a construct is likely to be needed in 2+ test classes, it belongs in `support/`.

## MockK: Common Pitfalls

### Reified Extensions in Mock Stubs: Production and Test Must Match

The `body<T>()` function compiles to `body(ParameterizedTypeReference<T>)`, whereas `body(T::class.java)` calls `body(Class<T>)`. These are **different overloads**. A mock stub must call the same overload as the production code.

If production code uses `body<T>()`, the test must also use it:

```kotlin
// Production: .retrieve().body<MyDto>()
// Test:
every { responseSpec.body<MyDto>() } returns myDto  // OK — same overload

```

If production code uses `body(Class<T>)`, the test must follow suit:

```kotlin
// Production: .retrieve().body(MyDto::class.java)
// Test:
every { responseSpec.body(MyDto::class.java) } returns myDto  // OK — same overload

```

**Preference:** Use reified extensions (`body<T>()`) in both production and tests.

### OncePerRequestFilter + Relaxed Mocks

Using `relaxed = true` for `HttpServletRequest` breaks `OncePerRequestFilter` because `getAttribute()` will return a non-null value, leading the filter to believe it has already been executed.
**Fix:** `every { request.getAttribute(any()) } returns null`

## Workflow

1. Write a minimal implementation.
2. Write tests.
3. Run tests — they must **PASS**.
4. Refactor the code (**IMPROVE**).
5. Check coverage (**65%+**).

## Test Isolation and Strictness

### Tests Must Not Depend on Each Other

All tests — especially integration and e2e tests — must be runnable in isolation and in any order.

* Do **not** rely on `@Order` or execution sequence to share state.
* Reset database, mocks, WireMock stubs, caches, and any other mutable state per test.
* If a test needs prior state, it must create that state itself inside the same test.

### Assertions Must Be Strict

Do not weaken assertions just to make tests pass.

* Prefer exact expectations (`count shouldBe 1`) over loose checks (`count > 0`).
* Prefer exact collection sizes (`shouldHaveSize(2)`) over lower bounds (`size >= 2`).
* If a strict assertion is failing because of shared state, fix test isolation instead of relaxing the assertion.

## Troubleshooting Tests

If tests fail:

1. Check test isolation (ensure they don't affect each other).
2. Verify that mocks are configured correctly.
3. Fix the implementation or the test setup, not the assertions (unless the assertions contradict business rules).
