#!/bin/bash
# Warn when creating new Flyway migration files.
INPUT=$(cat)

# Extract file_path, normalize separators
FILE=$(echo "$INPUT" | grep -o '"file_path" *: *"[^"]*"' | head -1 | sed 's/"file_path" *: *"//;s/"$//' | tr '\\' '/' | tr -s '/')

# Skip if not a .sql file in a migration directory
[[ "$FILE" == */migration/*.sql ]] || exit 0

echo "Политика Flyway: создавай новую миграцию только для НОВЫХ таблиц или структур. Если изменяешь уже существующую таблицу — модифицируй соответствующую существующую миграцию."
