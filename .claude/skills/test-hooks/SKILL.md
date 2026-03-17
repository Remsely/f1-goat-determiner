---
name: test-hooks
description: >
    Проверяет, что все Claude Code хуки работают корректно: format-python (ruff),
    format-typescript (prettier/eslint), guard-migration (предупреждение).
    Используй когда хуки перестали срабатывать, после изменения settings.json,
    или после смены рабочей директории.
allowed-tools: Edit, Write, Read, Bash
---

# Test Hooks — Проверка всех хуков

Хуки срабатывают автоматически на Edit/Write. Процесс: намеренно портим файл →
смотрим, исправился ли он после операции → делаем вывод.

## Шаг 1: format-python.sh (PostToolUse → Edit/Write на .py)

Сделай Edit на любой существующий .py файл с намеренно плохим форматированием.
Например, в `analytics-api/tests/unit/test_db.py` переставь импорты в неправильный порядок:

```python
# Было (правильно):
from collections.abc import Iterator
from unittest.mock import MagicMock, patch

import psycopg2
import pytest

# Стало (неправильно — stdlib после third-party, нет пробела после запятой):
import psycopg2
import pytest
from collections.abc import Iterator
from unittest.mock import MagicMock,patch
```

После Edit прочитай файл через Read и проверь: ruff должен был восстановить правильный порядок и пробел.

**Ожидаемый результат:** импорты отсортированы stdlib → third-party → local, пробел после запятой.
**Провал:** файл остался с неправильным форматированием.

## Шаг 2: format-typescript.sh (PostToolUse → Edit/Write на .ts)

Сделай Edit на любой существующий .ts файл с плохим форматированием.
Например, в `frontend/src/api/client.ts` добавь лишние пробелы или измени кавычки:

```typescript
// Неправильно: двойные кавычки вместо одинарных, лишний пробел
const API_URL = import.meta.env.VITE_API_URL   ||   "http://localhost:8000";
```

После Edit прочитай файл через Read и проверь: prettier должен был исправить.

**Ожидаемый результат:** одинарные кавычки, нормальные пробелы.
**Провал:** файл остался с двойными кавычками / лишними пробелами.

## Шаг 3: guard-migration.sh (PreToolUse → Write на .sql в migration/)

Попробуй сделать Write на файл вида `data-sync-svc/db/src/main/resources/db/migration/V999__test.sql`.

**Ожидаемый результат:** хук выводит предупреждение про политику Flyway перед операцией.
**Провал:** файл создался без предупреждения.

Если файл всё же создался — удали его через Bash:
```bash
rm data-sync-svc/db/src/main/resources/db/migration/V999__test.sql
```

## Шаг 4: Итоговый отчёт

Сообщи результат по каждому хуку:

| Хук                  | Файл                        | Результат     |
|----------------------|-----------------------------|---------------|
| format-python.sh     | tests/unit/test_db.py       | ✅ / ❌       |
| format-typescript.sh | frontend/src/api/client.ts  | ✅ / ❌       |
| guard-migration.sh   | V999__test.sql              | ✅ / ❌       |

Если хук не сработал — диагностируй причину:
1. Проверь команду в `.claude/settings.json`
2. Запусти хук вручную: `bash .claude/hooks/format-python.sh` из корня репо
3. Проверь `git rev-parse --show-cdup` в текущей директории — должен дать `../` или пустую строку
