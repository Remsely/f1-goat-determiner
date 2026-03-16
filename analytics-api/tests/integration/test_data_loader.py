"""Интеграционные тесты F1DataLoader с реальной PostgreSQL."""

from src.core.data_loader import get_data_loader


class TestDataLoaderResults:
    def test_results_returns_dataframe_with_correct_columns(self) -> None:
        loader = get_data_loader()
        df = loader.results()

        expected_columns = {
            "resultId",
            "raceId",
            "driverId",
            "constructorId",
            "number",
            "grid",
            "position",
            "positionText",
            "positionOrder",
            "points",
            "laps",
            "time",
            "milliseconds",
            "fastestLap",
            "rank",
            "fastestLapTime",
            "fastestLapSpeed",
            "statusId",
        }
        assert set(df.columns) == expected_columns

    def test_results_returns_correct_count(self) -> None:
        loader = get_data_loader()
        df = loader.results()

        assert len(df) == 2

    def test_results_contains_correct_data(self) -> None:
        loader = get_data_loader()
        df = loader.results()

        hamilton = df[df["driverId"] == 1].iloc[0]
        assert hamilton["grid"] == 1
        assert hamilton["position"] == 1
        assert float(hamilton["points"]) == 25.0


class TestDataLoaderRaces:
    def test_races_has_year_column(self) -> None:
        loader = get_data_loader()
        df = loader.races()

        assert "year" in df.columns
        assert "raceId" in df.columns

    def test_races_returns_correct_season(self) -> None:
        loader = get_data_loader()
        df = loader.races()

        assert df.iloc[0]["year"] == 2024


class TestDataLoaderDrivers:
    def test_drivers_has_correct_columns(self) -> None:
        loader = get_data_loader()
        df = loader.drivers()

        assert "driverId" in df.columns
        assert "driverRef" in df.columns
        assert "forename" in df.columns
        assert "surname" in df.columns

    def test_drivers_returns_correct_count(self) -> None:
        loader = get_data_loader()
        df = loader.drivers()

        assert len(df) == 2


class TestDataLoaderStandings:
    def test_driver_standings_columns(self) -> None:
        loader = get_data_loader()
        df = loader.driver_standings()

        expected = {"driverStandingsId", "raceId", "driverId", "points", "position", "positionText", "wins"}
        assert set(df.columns) == expected

    def test_constructor_standings_columns(self) -> None:
        loader = get_data_loader()
        df = loader.constructor_standings()

        expected = {"constructorStandingsId", "raceId", "constructorId", "points", "position", "positionText", "wins"}
        assert set(df.columns) == expected


class TestDataLoaderQualifying:
    def test_qualifying_columns(self) -> None:
        loader = get_data_loader()
        df = loader.qualifying()

        expected = {"qualifyId", "raceId", "driverId", "constructorId", "number", "position", "q1", "q2", "q3"}
        assert set(df.columns) == expected

    def test_qualifying_returns_correct_count(self) -> None:
        loader = get_data_loader()
        df = loader.qualifying()

        assert len(df) == 2


class TestGetAvailableSeasons:
    def test_returns_sorted_seasons(self) -> None:
        loader = get_data_loader()
        seasons = loader.get_available_seasons()

        assert seasons == [2024]
