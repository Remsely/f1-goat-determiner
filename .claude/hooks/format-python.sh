#!/bin/bash
# Format only the specific Python file that was edited/written.
INPUT=$(cat)

# Extract first "file_path" value, normalize path separators
FILE=$(echo "$INPUT" | grep -o '"file_path" *: *"[^"]*"' | head -1 | sed 's/"file_path" *: *"//;s/"$//' | tr '\\' '/' | tr -s '/')

# Skip if not a .py file or file doesn't exist
[[ "$FILE" == *.py ]] || exit 0
[[ -f "$FILE" ]] || exit 0

ruff format "$FILE" 2>/dev/null || true
ruff check --fix --quiet "$FILE" 2>/dev/null || true
