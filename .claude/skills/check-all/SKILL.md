---
name: check-all
description: >
    Full lint and test run across all services (data-sync-svc, analytics-api, frontend).
    Use when the user asks to verify everything is green, run all checks, or validate the project
    before a commit or PR.
allowed-tools: Bash
---

# Check All — Full Lint & Test Run Across All Services

Run lint and tests for all three services. All steps are mandatory. Report results at the end.

## Step 1: data-sync-svc — Lint

```bash
cd data-sync-svc && ./gradlew detekt
```

Record: passed / failed (with violations listed).

## Step 2: data-sync-svc — Tests

```bash
cd data-sync-svc && ./gradlew test
```

Record: total tests, passed, failed, skipped. If failures — list the failing test names and error summaries.

## Step 3: analytics-api — Lint

```bash
cd analytics-api && ruff check . && ruff format --check .
```

Record: passed / failed (with file and rule for each violation).

## Step 4: analytics-api — Tests

```bash
cd analytics-api && pytest
```

Record: total tests, passed, failed. If failures — list failing test names and error summaries.

## Step 5: frontend — Lint & Format

```bash
cd frontend && bun run lint && bun run format:check
```

Record: ESLint passed / failed, Prettier passed / failed.

## Step 6: frontend — Tests

```bash
cd frontend && bun run test:run
```

Record: total tests, passed, failed. If failures — list failing test names and error summaries.

## Step 7: Summary Report

Produce a concise table:

| Service       | Lint    | Tests       |
|---------------|---------|-------------|
| data-sync-svc | ✅ / ❌ | ✅ N/N / ❌ |
| analytics-api | ✅ / ❌ | ✅ N/N / ❌ |
| frontend      | ✅ / ❌ | ✅ N/N / ❌ |

If any step failed — list the issues grouped by service and suggest fixes.
Overall verdict: **all green** / **action required**.
