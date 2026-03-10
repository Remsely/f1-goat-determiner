# Sync Report — Full Analysis of data-sync-svc Run

Perform a comprehensive analysis of the latest data-sync-svc run. All steps are mandatory.

## Step 1: Decompress Logs

Find all `.gz` files in `logs/` and decompress them with `gzip -dk`.

## Step 2: Verify Data Against Jolpica API

For each entity, fetch `total` from the Jolpica API (request with `?limit=1`) and compare with `count(*)` from the DB.
Before running any query, check actual column names via `information_schema.columns` to avoid errors.

| Entity       | API endpoint         |
|--------------|----------------------|
| Statuses     | `/status.json`       |
| Circuits     | `/circuits.json`     |
| Constructors | `/constructors.json` |
| Drivers      | `/drivers.json`      |
| Races        | `/races.json`        |
| Results      | `/results.json`      |
| Qualifying   | `/qualifying.json`   |

API base: `https://api.jolpi.ca/ergast/f1/`

For standings, verify season coverage: count distinct seasons in `races`, then count distinct seasons that have
driver standings, and those that have constructor standings.
Constructor standings don't exist for some early seasons (before 1958) — this is expected.

Use the MCP PostgreSQL tool for all DB queries.

## Step 3: Referential Integrity

Check for orphaned records (FK without parent):

- `results` → `races`, `drivers`, `constructors`, `statuses`
- `qualifying` → `races`, `drivers`, `constructors`
- `driver_standings` → `races`, `drivers`
- `constructor_standings` → `races`, `constructors`
- `races` → `circuits`

## Step 4: Sync Jobs Analysis

Fetch all records from `sync_jobs` and `sync_checkpoints`. Check actual column names first via
`information_schema.columns`. For each job:

- Type (FULL/INCREMENTAL), status, start/completion time
- Number of API requests, failed requests
- For FAILED jobs: which step failed, reason, retry attempts
- Cooldown ticks (scheduler skips)
- Resume: which steps were skipped, which were executed

## Step 5: Log Analysis

Extract key events from decompressed log files (excluding response bodies):

```
grep -n "step\|COMPLETED\|FAILED\|ERROR\|Rate\|Cooldown\|skip\|Created\|started\|shutdown\|Graceful\|API calls\|season=" logs/*.log logs/*.0 logs/*.1 | grep -vi "JolpicaResponse\|MRData\|DEBUG"
```

## Step 6: Final Report

Produce a report with the following sections:

### 1. Data Integrity

Table: Entity | Jolpica API | DB | Status (OK/MISMATCH)

### 2. Referential Integrity

Table: Check | Result

### 3. System Timeline

For each job — a visual diagram with timeline:

```
Job #N [TYPE] ─── step1 ─── step2 ─── ... ─── COMPLETED/FAILED
                  │ records  │ records         │ error details
                  │ duration │ duration        │
```

When drawing diagrams, carefully align each line to the same character width to avoid broken lines or misaligned columns.

### 4. Error Handling

If there were failures: chain of failure → cooldown → resume → completion

### 5. Incremental Sync

Behavior of incremental jobs: number of API requests, 0 new records = data is up to date

### 6. Conclusion

Overall assessment: data is consistent / discrepancies found, system is stable / issues detected