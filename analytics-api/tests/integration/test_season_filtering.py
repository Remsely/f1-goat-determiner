"""Интеграционные тесты сезонной фильтрации в SQL-запросах."""

import psycopg2

from src.core.data_loader import get_data_loader


class TestSeasonFilteringInSQL:
    """Проверяем что параметризованные запросы с seasons работают корректно."""

    def test_results_with_matching_season(self) -> None:
        loader = get_data_loader()
        df = loader.results(seasons=[2024])
        assert len(df) == 2

    def test_results_without_season_returns_all(self) -> None:
        loader = get_data_loader()
        df = loader.results()
        assert len(df) == 2

    def test_results_with_nonexistent_season_returns_empty(self) -> None:
        loader = get_data_loader()
        df = loader.results(seasons=[1900])
        assert len(df) == 0

    def test_races_with_matching_season(self) -> None:
        loader = get_data_loader()
        df = loader.races(seasons=[2024])
        assert len(df) == 1

    def test_races_with_nonexistent_season(self) -> None:
        loader = get_data_loader()
        df = loader.races(seasons=[1900])
        assert len(df) == 0

    def test_driver_standings_with_season(self) -> None:
        loader = get_data_loader()
        df = loader.driver_standings(seasons=[2024])
        assert len(df) == 2

    def test_constructor_standings_with_season(self) -> None:
        loader = get_data_loader()
        df = loader.constructor_standings(seasons=[2024])
        assert len(df) == 2

    def test_qualifying_with_season(self) -> None:
        loader = get_data_loader()
        df = loader.qualifying(seasons=[2024])
        assert len(df) == 2

    def test_multiple_seasons_filter(self, db_params) -> None:
        """Добавляем вторую гонку в другом сезоне и проверяем мульти-фильтр."""
        conn = psycopg2.connect(**db_params)
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO races (season, round, circuit_id, name, date) "
                "VALUES (2023, 1, 1, 'Test GP 2023', '2023-03-05')"
            )
        conn.commit()
        conn.close()

        loader = get_data_loader()

        both = loader.races(seasons=[2023, 2024])
        assert len(both) == 2

        only_2023 = loader.races(seasons=[2023])
        assert len(only_2023) == 1
        assert only_2023.iloc[0]["year"] == 2023
