---
name: e2e-test
description: >
    Full browser E2E testing of F1 GOAT Determiner via Playwright.
    Use after significant changes to frontend components, API endpoints, response schemas,
    or data processing logic that affects what users see.
    Also use when the user asks to test the app, verify it works, or run e2e tests.
allowed-tools: Bash, Read, Glob, Grep
---

# E2E Testing — Full Browser Testing via Playwright

Run full E2E testing of the application through a real browser using the Playwright MCP plugin.

## Prerequisites

Before starting, verify the infrastructure is running:

### Step 1: Start the Database

Use the database from data-sync-svc — it already contains all synced F1 data.

```bash
docker compose -f data-sync-svc/docker-compose.yaml up -d
```

Wait for the health check to pass:

```bash
docker compose -f data-sync-svc/docker-compose.yaml ps
```

If the database is empty (first run), populate it:

```bash
cd data-sync-svc && ./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

### Step 2: Start analytics-api

First, check if port 8000 is free (IntelliJ IDEA or other tools may occupy it):

```bash
netstat -ano | grep ":8000" | grep LISTEN
```

If port 8000 is free:
```bash
cd analytics-api && .venv/Scripts/python.exe -m uvicorn src.main:app --reload --port 8000
```

If port 8000 is occupied, use 8001 and update `frontend/vite.config.ts` proxy target to `http://localhost:8001`:
```bash
cd analytics-api && .venv/Scripts/python.exe -m uvicorn src.main:app --reload --port 8001
```

Verify: `GET http://localhost:8000/` (or 8001) should return `{"status": "working"}`.

### Step 3: Start frontend

```bash
cd frontend && bun run dev
```

Verify: `http://localhost:3000` should be accessible.

---

## Test Execution

Use the Playwright MCP plugin to navigate and interact with the browser.
Base URL: `http://localhost:3000`

Execute ALL test cases below. For each test, report PASS/FAIL with a screenshot if the test fails.

---

## Test Cases

### TC-01: Home Page Loads

1. Navigate to `http://localhost:3000`
2. **Verify:** Page title or heading contains "F1 GOAT"
3. **Verify:** Three feature cards are visible: "Tier List", "ELO Rating", "Teammate Battles"
4. **Verify:** "Tier List" card is clickable (not disabled)
5. **Verify:** "ELO Rating" and "Teammate Battles" are disabled / show "Coming Soon"

### TC-02: Navigation to Tier List

1. From Home Page, click the "Tier List" card
2. **Verify:** URL changes to `/tier-list`
3. **Verify:** Header with "Tier List" title is visible
4. **Verify:** Loading spinner appears initially
5. **Verify:** After loading, tier cards (S, A, B, C...) appear with driver data

### TC-03: Default Tier List Data

1. On `/tier-list`, wait for data to load
2. **Verify:** "All Seasons" preset is selected by default
3. **Verify:** Number of tiers is 4 by default
4. **Verify:** Minimum races is 10 by default
5. **Verify:** At least one tier card is visible with drivers
6. **Verify:** Info row shows total drivers count > 0 and silhouette score

### TC-04: Era Presets

Test each era preset produces different results:

1. Click "Modern Era" (or "Hybrid Era") preset
2. **Verify:** Data reloads (spinner or data change)
3. **Verify:** Tier list updates with drivers from the selected era
4. Click "Last 5 Years" preset
5. **Verify:** Data changes again (different driver counts or tier composition)
6. Click "All Seasons" to reset
7. **Verify:** Full data is restored

### TC-05: Custom Season Picker

1. Open the season picker (click "Select Seasons..." or similar)
2. **Verify:** Grid of years is displayed
3. Click on year "2023"
4. **Verify:** Year is highlighted/selected
5. Click on year "2024"
6. **Verify:** Both years are selected, count shows "2 selected"
7. **Verify:** Tier list reloads with data for only 2023-2024
8. Click "Clear" button
9. **Verify:** All selections are cleared
10. Click "Select All" button
11. **Verify:** All years are selected

### TC-06: Number of Tiers Filter

1. Change "Number of Tiers" dropdown to 2
2. **Verify:** Only 2 tier cards are displayed (S and A)
3. Change to 6
4. **Verify:** Up to 6 tier cards (S, A, B, C, D, F) are displayed
5. Change back to 4
6. **Verify:** 4 tier cards are displayed

### TC-07: Minimum Races Filter

1. Set minimum races to "1+"
2. **Verify:** Total drivers count increases (more drivers included)
3. Set minimum races to "100+"
4. **Verify:** Total drivers count decreases significantly
5. Set minimum races back to "10+"
6. **Verify:** Count returns to original value

### TC-08: Driver Card Display

1. Find any tier with drivers
2. **Verify:** Each driver card shows:
    - Driver name
    - Nationality
    - Stat badges (championships, wins, poles, podiums) if applicable
    - Races count and win rate

### TC-09: Driver Modal

1. Click on any driver card (preferably a well-known driver like Hamilton, Schumacher, or Verstappen)
2. **Verify:** Modal opens with animation
3. **Verify:** Modal shows driver name as title
4. **Verify:** Modal contains "Career Stats" section (Races, Championships, Wins, Pole Positions, Podiums)
5. **Verify:** Modal contains "Performance" section (Win Rate, Pole Rate, Podium Rate, etc.)
6. Close modal by clicking X button
7. **Verify:** Modal closes

### TC-10: Modal Close Methods

1. Open a driver modal
2. Press Escape key
3. **Verify:** Modal closes
4. Open the modal again
5. Click outside the modal (on the overlay)
6. **Verify:** Modal closes

### TC-11: Badge Legend

1. On the tier list page
2. **Verify:** Badge legend is visible showing icons for Championships, Wins, Poles, Podiums
3. **Verify:** Legend colors/icons match those used in driver cards

### TC-12: Header Navigation

1. On `/tier-list`, look for the header
2. Click the logo or home link in the header
3. **Verify:** Navigated back to Home Page (`/`)

### TC-13: Back Link

1. On `/tier-list`, find the "Back" link
2. Click it
3. **Verify:** Navigated back to Home Page

### TC-14: Responsive Layout (Optional)

1. Resize browser window to mobile width (< 480px)
2. **Verify:** Layout adapts - cards stack vertically, filters are still usable
3. Resize to tablet (768px)
4. **Verify:** Layout adapts appropriately
5. Resize back to desktop (> 1024px)

### TC-15: Data Sanity Check

1. On tier list with "All Seasons" and default filters
2. Find the S-tier (top tier)
3. **Verify:** Contains expected legendary drivers (e.g., Hamilton, Schumacher, Fangio, Prost, Senna, or Verstappen)
4. Find the lowest tier
5. **Verify:** Drivers there have significantly lower stats (low win rate, few podiums)

### TC-17: Latest Season Works

This test guards against regressions where the current (in-progress) season breaks the tier list
due to incomplete standings data.

1. Open the season picker
2. Select **only the current/latest season** (the most recent year with any race results)
3. Set "Minimum Races" to "1+"
4. **Verify:** Tier list loads successfully — no "Failed to load" error
5. **Verify:** Drivers count > 0
6. **Verify:** Number of tiers equals the selected value (e.g. 4)
7. Set "Minimum Races" higher than the number of races completed in the season (e.g. "5+" for a season with only 2 races done)
8. **Verify:** An informative error message is shown (NOT a generic "Failed to load data", but a message about insufficient races)

### TC-16: Error State (Optional)

1. Stop the analytics-api server
2. Reload the tier list page
3. **Verify:** Error message is displayed to the user (not a blank page)
4. Restart the analytics-api server

---

## Report Format

After running all tests, produce a summary:

```
E2E Test Report — F1 GOAT Determiner
Date: YYYY-MM-DD

Infrastructure: DB ✓/✗ | API ✓/✗ | Frontend ✓/✗

Results:
| Test Case | Status | Notes |
|-----------|--------|-------|
| TC-01     | PASS   |       |
| TC-02     | PASS   |       |
| ...       | ...    | ...   |

Passed: X/17
Failed: Y/17

Issues Found:
- [List any bugs or UI issues discovered during testing]
```
