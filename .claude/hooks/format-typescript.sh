#!/bin/bash
INPUT=$(cat)
echo "$INPUT" | grep -q '\.tsx\?"' || exit 0
FILE=$(echo "$INPUT" | sed -n 's/.*"file_path" *: *"\([^"]*\)".*/\1/p' | tr '\\' '/')
[[ "$FILE" == *.ts || "$FILE" == *.tsx ]] || exit 0
cd frontend
bunx prettier --write "$FILE"
bunx eslint --fix "$FILE" || true
