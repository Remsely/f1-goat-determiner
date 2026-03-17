"""Фабрики тестовых DataFrame для unit-тестов."""

import pandas as pd


def make_drivers() -> pd.DataFrame:
    """8 пилотов: 3 топовых + 5 обычных."""
    return pd.DataFrame(
        {
            "driverId": [1, 2, 3, 4, 5, 6, 7, 8],
            "driverRef": [
                "hamilton",
                "verstappen",
                "schumacher",
                "alonso",
                "bottas",
                "stroll",
                "latifi",
                "mazepin",
            ],
            "number": [44, 1, 0, 14, 77, 18, 6, 9],
            "code": ["HAM", "VER", "MSC", "ALO", "BOT", "STR", "LAT", "MAZ"],
            "forename": ["Lewis", "Max", "Michael", "Fernando", "Valtteri", "Lance", "Nicholas", "Nikita"],
            "surname": [
                "Hamilton",
                "Verstappen",
                "Schumacher",
                "Alonso",
                "Bottas",
                "Stroll",
                "Latifi",
                "Mazepin",
            ],
            "dob": [
                "1985-01-07",
                "1997-09-30",
                "1969-01-03",
                "1981-07-29",
                "1989-08-28",
                "1998-10-29",
                "1995-06-29",
                "1999-03-02",
            ],
            "nationality": [
                "British",
                "Dutch",
                "German",
                "Spanish",
                "Finnish",
                "Canadian",
                "Canadian",
                "Russian",
            ],
        }
    )


def make_races() -> pd.DataFrame:
    """6 гонок: по 3 за 2 сезона."""
    return pd.DataFrame(
        {
            "raceId": [1, 2, 3, 4, 5, 6],
            "year": [2023, 2023, 2023, 2024, 2024, 2024],
            "round": [1, 2, 3, 1, 2, 3],
            "circuitId": [1, 2, 3, 1, 2, 3],
            "name": [
                "Bahrain GP",
                "Saudi GP",
                "Australian GP",
                "Bahrain GP",
                "Saudi GP",
                "Australian GP",
            ],
            "date": [
                "2023-03-05",
                "2023-03-19",
                "2023-04-02",
                "2024-03-02",
                "2024-03-09",
                "2024-03-24",
            ],
            "time": ["15:00:00"] * 6,
        }
    )


def make_results() -> pd.DataFrame:
    """Результаты для 6 гонок x 8 пилотов = 48 записей.

    Позиции:
    - Hamilton: всегда 1-2 (wins ~50%)
    - Verstappen: всегда 1-2 (wins ~50%)
    - Schumacher: всегда 3-4 (podiums)
    - Alonso: 4-6
    - Bottas: 5-8
    - Stroll: 7-10
    - Latifi: 12-16
    - Mazepin: 15-20
    """
    rows = []
    result_id = 1

    positions_per_race = [
        # race 1: HAM wins
        {1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 7, 7: 12, 8: 16},
        # race 2: VER wins
        {1: 2, 2: 1, 3: 3, 4: 5, 5: 6, 6: 8, 7: 14, 8: 18},
        # race 3: HAM wins
        {1: 1, 2: 2, 3: 4, 4: 6, 5: 7, 6: 9, 7: 15, 8: 19},
        # race 4: VER wins
        {1: 2, 2: 1, 3: 3, 4: 4, 5: 8, 6: 10, 7: 13, 8: 20},
        # race 5: HAM wins
        {1: 1, 2: 3, 3: 4, 4: 5, 5: 6, 6: 7, 7: 16, 8: 17},
        # race 6: VER wins
        {1: 3, 2: 1, 3: 2, 4: 6, 5: 5, 6: 8, 7: 12, 8: 15},
    ]

    # Очки по позициям (упрощённо, первые 10)
    points_map = {1: 25, 2: 18, 3: 15, 4: 12, 5: 10, 6: 8, 7: 6, 8: 4, 9: 2, 10: 1}

    for race_idx, positions in enumerate(positions_per_race):
        race_id = race_idx + 1
        for driver_id, pos in positions.items():
            rows.append(
                {
                    "resultId": result_id,
                    "raceId": race_id,
                    "driverId": driver_id,
                    "constructorId": 1 if driver_id <= 2 else (2 if driver_id <= 4 else 3),
                    "number": driver_id * 10,
                    "grid": pos,
                    "position": pos,
                    "positionText": str(pos),
                    "positionOrder": pos,
                    "points": points_map.get(pos, 0),
                    "laps": 57,
                    "time": None,
                    "milliseconds": None,
                    "fastestLap": None,
                    "rank": None,
                    "fastestLapTime": None,
                    "fastestLapSpeed": None,
                    "statusId": 1,
                }
            )
            result_id += 1

    return pd.DataFrame(rows)


def make_qualifying() -> pd.DataFrame:
    """Квалификация: Hamilton и Verstappen делят поулы."""
    rows = []
    q_id = 1
    pole_drivers = [1, 2, 1, 2, 1, 2]  # чередование

    for race_id in range(1, 7):
        for driver_id in range(1, 9):
            if driver_id == pole_drivers[race_id - 1]:
                pos = 1
            elif driver_id <= 2:
                pos = 2
            elif driver_id <= 4:
                pos = driver_id + 1
            else:
                pos = driver_id + 3

            rows.append(
                {
                    "qualifyId": q_id,
                    "raceId": race_id,
                    "driverId": driver_id,
                    "constructorId": 1 if driver_id <= 2 else (2 if driver_id <= 4 else 3),
                    "number": driver_id * 10,
                    "position": pos,
                    "q1": "1:30.000",
                    "q2": "1:29.000" if pos <= 15 else None,
                    "q3": "1:28.000" if pos <= 10 else None,
                }
            )
            q_id += 1

    return pd.DataFrame(rows)


def make_driver_standings() -> pd.DataFrame:
    """Standings на конец каждого сезона (race 3 и race 6).

    Hamilton и Verstappen — чемпионы (по одному каждый).
    """
    rows = []
    ds_id = 1

    # Standings после каждой гонки (6 гонок x 8 пилотов)
    # Финальные standings (на конец сезона) — race 3 для 2023, race 6 для 2024
    for race_id in range(1, 7):
        for driver_id in range(1, 9):
            if driver_id == 1:
                position = 1 if race_id <= 3 else 2
                points = 68.0 if race_id == 3 else 58.0 if race_id == 6 else float(race_id * 20)
                wins = 3 if race_id == 3 else 2 if race_id == 6 else race_id
            elif driver_id == 2:
                position = 2 if race_id <= 3 else 1
                points = 54.0 if race_id == 3 else 69.0 if race_id == 6 else float(race_id * 16)
                wins = 3 if race_id >= 4 else race_id
            elif driver_id == 3:
                position = 3
                points = float(race_id * 12)
                wins = 0
            elif driver_id == 4:
                position = 4
                points = float(race_id * 8)
                wins = 0
            elif driver_id == 5:
                position = 5
                points = float(race_id * 5)
                wins = 0
            elif driver_id == 6:
                position = 6
                points = float(race_id * 2)
                wins = 0
            else:
                position = driver_id
                points = 0.0
                wins = 0

            rows.append(
                {
                    "driverStandingsId": ds_id,
                    "raceId": race_id,
                    "driverId": driver_id,
                    "points": points,
                    "position": position,
                    "positionText": str(position),
                    "wins": wins,
                }
            )
            ds_id += 1

    return pd.DataFrame(rows)


def make_constructor_standings() -> pd.DataFrame:
    """Constructor standings: команда 1 — лидер, 2 — середняк, 3 — аутсайдер."""
    rows = []
    cs_id = 1

    for race_id in range(1, 7):
        for constructor_id in range(1, 4):
            rows.append(
                {
                    "constructorStandingsId": cs_id,
                    "raceId": race_id,
                    "constructorId": constructor_id,
                    "points": float(race_id * (30 - constructor_id * 8)),
                    "position": constructor_id,
                    "positionText": str(constructor_id),
                    "wins": 3 if constructor_id == 1 and race_id == 3 else 0,
                }
            )
            cs_id += 1

    return pd.DataFrame(rows)
