import pandas as pd

from .db import read_sql


class F1DataLoader:
    """Загрузка данных F1 из PostgreSQL."""

    def results(self, seasons: list[int] | None = None) -> pd.DataFrame:
        query = """
            SELECT
                r.id            AS "resultId",
                r.race_id       AS "raceId",
                r.driver_id     AS "driverId",
                r.constructor_id AS "constructorId",
                r.number,
                r.grid,
                r.position,
                r.position_text  AS "positionText",
                r.position_order AS "positionOrder",
                r.points,
                r.laps,
                r.time,
                r.milliseconds,
                r.fastest_lap       AS "fastestLap",
                r.fastest_lap_rank  AS "rank",
                r.fastest_lap_time  AS "fastestLapTime",
                r.fastest_lap_speed AS "fastestLapSpeed",
                r.status_id         AS "statusId"
            FROM results r
        """
        if seasons:
            query += " JOIN races ra ON ra.id = r.race_id WHERE ra.season = ANY(%(seasons)s)"
            return read_sql(query, {"seasons": seasons})

        return read_sql(query)

    def races(self, seasons: list[int] | None = None) -> pd.DataFrame:
        query = """
            SELECT
                ra.id         AS "raceId",
                ra.season     AS "year",
                ra.round,
                ra.circuit_id AS "circuitId",
                ra.name,
                ra.date,
                ra.time
            FROM races ra
        """
        if seasons:
            query += " WHERE ra.season = ANY(%(seasons)s)"
            return read_sql(query, {"seasons": seasons})

        return read_sql(query)

    def drivers(self) -> pd.DataFrame:
        return read_sql("""
            SELECT
                d.id  AS "driverId",
                d.ref AS "driverRef",
                d.number,
                d.code,
                d.forename,
                d.surname,
                d.dob,
                d.nationality
            FROM drivers d
        """)

    def driver_standings(self, seasons: list[int] | None = None) -> pd.DataFrame:
        query = """
            SELECT
                ds.id            AS "driverStandingsId",
                ds.race_id       AS "raceId",
                ds.driver_id     AS "driverId",
                ds.points,
                ds.position,
                ds.position_text AS "positionText",
                ds.wins
            FROM driver_standings ds
        """
        if seasons:
            query += " JOIN races ra ON ra.id = ds.race_id WHERE ra.season = ANY(%(seasons)s)"
            return read_sql(query, {"seasons": seasons})

        return read_sql(query)

    def constructor_standings(self, seasons: list[int] | None = None) -> pd.DataFrame:
        query = """
            SELECT
                cs.id              AS "constructorStandingsId",
                cs.race_id         AS "raceId",
                cs.constructor_id  AS "constructorId",
                cs.points,
                cs.position,
                cs.position_text   AS "positionText",
                cs.wins
            FROM constructor_standings cs
        """
        if seasons:
            query += " JOIN races ra ON ra.id = cs.race_id WHERE ra.season = ANY(%(seasons)s)"
            return read_sql(query, {"seasons": seasons})

        return read_sql(query)

    def qualifying(self, seasons: list[int] | None = None) -> pd.DataFrame:
        query = """
            SELECT
                q.id              AS "qualifyId",
                q.race_id         AS "raceId",
                q.driver_id       AS "driverId",
                q.constructor_id  AS "constructorId",
                q.number,
                q.position,
                q.q1,
                q.q2,
                q.q3
            FROM qualifying q
        """
        if seasons:
            query += " JOIN races ra ON ra.id = q.race_id WHERE ra.season = ANY(%(seasons)s)"
            return read_sql(query, {"seasons": seasons})

        return read_sql(query)

    def get_available_seasons(self) -> list[int]:
        """Список всех доступных сезонов."""
        df = read_sql("SELECT DISTINCT season FROM races ORDER BY season")
        return df["season"].tolist()


_loader: F1DataLoader | None = None


def get_data_loader() -> F1DataLoader:
    """Возвращает загрузчик данных."""
    global _loader
    if _loader is None:
        _loader = F1DataLoader()
    return _loader
