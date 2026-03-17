"""Фикстуры для интеграционных тестов с реальной PostgreSQL."""

import re
from pathlib import Path

import psycopg2

_FLYWAY_MIGRATION_DIR = (
    Path(__file__).parents[3] / "data-sync-svc" / "db" / "src" / "main" / "resources" / "db" / "migration"
)

# Миграции, относящиеся к схеме данных (V001-V009).
# V010+ (sync_jobs, sync_checkpoints, shedlock) специфичны для data-sync-svc.
_MAX_DATA_MIGRATION_VERSION = 9


def apply_schema(conn: psycopg2.extensions.connection) -> None:
    """Применяет Flyway-миграции данных (V001-V009) к тестовой БД."""
    migration_files = sorted(_FLYWAY_MIGRATION_DIR.glob("V*.sql"))

    for path in migration_files:
        version = _parse_version(path.name)
        if version is not None and version <= _MAX_DATA_MIGRATION_VERSION:
            sql = path.read_text(encoding="utf-8")
            with conn.cursor() as cur:
                cur.execute(sql)

    conn.commit()


def _parse_version(filename: str) -> int | None:
    """Извлекает номер версии из имени файла Flyway-миграции (V001__... → 1)."""
    match = re.match(r"V(\d+)__", filename)
    return int(match.group(1)) if match else None


def truncate_all(conn: psycopg2.extensions.connection) -> None:
    """Очищает все таблицы данных (в правильном порядке FK)."""
    tables = [
        "constructor_standings",
        "driver_standings",
        "qualifying",
        "results",
        "races",
        "drivers",
        "constructors",
        "circuits",
        "statuses",
    ]
    with conn.cursor() as cur:
        for table in tables:
            cur.execute(f"TRUNCATE TABLE {table} RESTART IDENTITY CASCADE")
    conn.commit()


def insert_test_data(conn: psycopg2.extensions.connection) -> None:
    """Вставляет минимальный набор данных для тестов."""
    with conn.cursor() as cur:
        # Статусы
        cur.execute("INSERT INTO statuses (id, status) VALUES (1, 'Finished')")

        # Контуры
        cur.execute(
            "INSERT INTO circuits (ref, name, locality, country) "
            "VALUES ('bahrain', 'Bahrain International Circuit', 'Sakhir', 'Bahrain')"
        )

        # Конструкторы
        cur.execute("INSERT INTO constructors (ref, name, nationality) VALUES ('mercedes', 'Mercedes', 'German')")
        cur.execute("INSERT INTO constructors (ref, name, nationality) VALUES ('red_bull', 'Red Bull', 'Austrian')")

        # Пилоты
        cur.execute(
            "INSERT INTO drivers (ref, number, code, forename, surname, dob, nationality) "
            "VALUES ('hamilton', 44, 'HAM', 'Lewis', 'Hamilton', '1985-01-07', 'British')"
        )
        cur.execute(
            "INSERT INTO drivers (ref, number, code, forename, surname, dob, nationality) "
            "VALUES ('verstappen', 1, 'VER', 'Max', 'Verstappen', '1997-09-30', 'Dutch')"
        )

        # Гонка
        cur.execute(
            "INSERT INTO races (season, round, circuit_id, name, date) "
            "VALUES (2024, 1, 1, 'Bahrain Grand Prix', '2024-03-02')"
        )

        # Результаты
        cur.execute(
            "INSERT INTO results "
            "(race_id, driver_id, constructor_id, grid, position, position_text, "
            "position_order, points, laps, status_id) "
            "VALUES (1, 1, 1, 1, 1, '1', 1, 25, 57, 1)"
        )
        cur.execute(
            "INSERT INTO results "
            "(race_id, driver_id, constructor_id, grid, position, position_text, "
            "position_order, points, laps, status_id) "
            "VALUES (1, 2, 2, 2, 2, '2', 2, 18, 57, 1)"
        )

        # Квалификация
        cur.execute(
            "INSERT INTO qualifying (race_id, driver_id, constructor_id, position, q1, q2, q3) "
            "VALUES (1, 1, 1, 1, '1:30.000', '1:29.000', '1:28.000')"
        )
        cur.execute(
            "INSERT INTO qualifying (race_id, driver_id, constructor_id, position, q2, q3) "
            "VALUES (1, 2, 2, 2, '1:29.500', '1:28.500')"
        )

        # Driver standings
        cur.execute(
            "INSERT INTO driver_standings (race_id, driver_id, points, position, position_text, wins) "
            "VALUES (1, 1, 25, 1, '1', 1)"
        )
        cur.execute(
            "INSERT INTO driver_standings (race_id, driver_id, points, position, position_text, wins) "
            "VALUES (1, 2, 18, 2, '2', 0)"
        )

        # Constructor standings
        cur.execute(
            "INSERT INTO constructor_standings (race_id, constructor_id, points, position, position_text, wins) "
            "VALUES (1, 1, 25, 1, '1', 1)"
        )
        cur.execute(
            "INSERT INTO constructor_standings (race_id, constructor_id, points, position, position_text, wins) "
            "VALUES (1, 2, 18, 2, '2', 0)"
        )

    conn.commit()
