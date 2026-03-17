"""Unit-тесты FakeDataLoader — сезонная фильтрация."""

import pytest

from tests.conftest import FakeDataLoader


class TestFakeDataLoaderSeasonFiltering:
    """Проверяем что фильтрация по сезонам работает корректно."""

    @pytest.fixture()
    def loader(self) -> FakeDataLoader:
        return FakeDataLoader()

    def test_races_without_seasons_returns_all(self, loader: FakeDataLoader) -> None:
        df = loader.races()
        assert len(df) == 6

    def test_races_with_single_season(self, loader: FakeDataLoader) -> None:
        df = loader.races(seasons=[2024])
        assert len(df) == 3
        assert (df["year"] == 2024).all()

    def test_races_with_multiple_seasons(self, loader: FakeDataLoader) -> None:
        df = loader.races(seasons=[2023, 2024])
        assert len(df) == 6

    def test_races_with_nonexistent_season(self, loader: FakeDataLoader) -> None:
        df = loader.races(seasons=[1950])
        assert len(df) == 0

    def test_results_filtered_by_season(self, loader: FakeDataLoader) -> None:
        all_results = loader.results()
        filtered = loader.results(seasons=[2024])

        assert len(filtered) < len(all_results)
        # 3 гонки в 2024 * 8 пилотов = 24
        assert len(filtered) == 24

    def test_qualifying_filtered_by_season(self, loader: FakeDataLoader) -> None:
        filtered = loader.qualifying(seasons=[2023])
        # 3 гонки * 8 пилотов = 24
        assert len(filtered) == 24

    def test_driver_standings_filtered_by_season(self, loader: FakeDataLoader) -> None:
        all_standings = loader.driver_standings()
        filtered = loader.driver_standings(seasons=[2024])

        assert len(filtered) < len(all_standings)

    def test_constructor_standings_filtered_by_season(self, loader: FakeDataLoader) -> None:
        all_standings = loader.constructor_standings()
        filtered = loader.constructor_standings(seasons=[2023])

        assert len(filtered) < len(all_standings)

    def test_drivers_not_filtered(self, loader: FakeDataLoader) -> None:
        """drivers() не принимает seasons — всегда возвращает всех."""
        df = loader.drivers()
        assert len(df) == 8

    def test_available_seasons(self, loader: FakeDataLoader) -> None:
        seasons = loader.get_available_seasons()
        assert seasons == [2023, 2024]
