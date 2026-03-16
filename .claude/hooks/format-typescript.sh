#!/bin/bash
# Format only the specific TypeScript file that was edited/written.
INPUT=$(cat)

# Extract first "file_path" value, normalize path separators
FILE=$(echo "$INPUT" | grep -o '"file_path" *: *"[^"]*"' | head -1 | sed 's/"file_path" *: *"//;s/"$//' | tr '\\' '/' | tr -s '/')

# Skip if not a .ts/.tsx file or file doesn't exist
[[ "$FILE" == *.ts || "$FILE" == *.tsx ]] || exit 0
[[ -f "$FILE" ]] || exit 0

# Make path relative to frontend/
RELATIVE="${FILE#*/frontend/}"
[[ "$RELATIVE" != "$FILE" ]] || exit 0

cd frontend || exit 0
bunx prettier --write "$RELATIVE" 2>/dev/null || true
bunx eslint --fix "$RELATIVE" 2>/dev/null || true
