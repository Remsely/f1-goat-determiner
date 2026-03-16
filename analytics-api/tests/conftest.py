"""Общие фикстуры для тестов."""

import pandas as pd
import pytest

from tests.support.factories import (
    make_constructor_standings,
    make_driver_standings,
    make_drivers,
    make_qualifying,
    make_races,
    make_results,
)


class FakeDataLoader:
    """In-memory загрузчик для unit-тестов."""

    def __init__(self) -> None:
        self._results = make_results()
        self._races = make_races()
        self._drivers = make_drivers()
        self._driver_standings = make_driver_standings()
        self._constructor_standings = make_constructor_standings()
        self._qualifying = make_qualifying()

    def _filter_by_seasons(
        self, df: pd.DataFrame, seasons: list[int] | None, race_id_col: str = "raceId"
    ) -> pd.DataFrame:
        if not seasons:
            return df.copy()
        race_ids = self._races[self._races["year"].isin(seasons)]["raceId"].tolist()
        return df[df[race_id_col].isin(race_ids)].copy()

    def results(self, seasons: list[int] | None = None) -> pd.DataFrame:
        return self._filter_by_seasons(self._results, seasons)

    def races(self, seasons: list[int] | None = None) -> pd.DataFrame:
        if not seasons:
            return self._races.copy()
        return self._races[self._races["year"].isin(seasons)].copy()

    def drivers(self) -> pd.DataFrame:
        return self._drivers.copy()

    def driver_standings(self, seasons: list[int] | None = None) -> pd.DataFrame:
        return self._filter_by_seasons(self._driver_standings, seasons)

    def constructor_standings(self, seasons: list[int] | None = None) -> pd.DataFrame:
        return self._filter_by_seasons(self._constructor_standings, seasons)

    def qualifying(self, seasons: list[int] | None = None) -> pd.DataFrame:
        return self._filter_by_seasons(self._qualifying, seasons)

    def count_races(self) -> int:
        return len(self._races)

    def count_drivers(self) -> int:
        return len(self._drivers)

    def count_results(self) -> int:
        return len(self._results)

    def get_available_seasons(self) -> list[int]:
        return sorted(self._races["year"].unique().tolist())


@pytest.fixture()
def fake_loader() -> FakeDataLoader:
    """Возвращает FakeDataLoader."""
    return FakeDataLoader()


@pytest.fixture()
def _patch_data_loader(monkeypatch: pytest.MonkeyPatch, fake_loader: FakeDataLoader) -> None:
    """Подменяет get_data_loader на FakeDataLoader."""
    monkeypatch.setattr("src.core.data_loader.get_data_loader", lambda: fake_loader)
    monkeypatch.setattr("src.core.get_data_loader", lambda: fake_loader)
    monkeypatch.setattr("src.core.preprocessing.get_data_loader", lambda: fake_loader)
