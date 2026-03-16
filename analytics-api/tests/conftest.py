"""Общие фикстуры для тестов."""

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

    def __init__(self):
        self._results = make_results()
        self._races = make_races()
        self._drivers = make_drivers()
        self._driver_standings = make_driver_standings()
        self._constructor_standings = make_constructor_standings()
        self._qualifying = make_qualifying()

    def _filter_by_seasons(self, df, seasons, race_id_col="raceId"):
        if not seasons:
            return df.copy()
        race_ids = self._races[self._races["year"].isin(seasons)]["raceId"].tolist()
        return df[df[race_id_col].isin(race_ids)].copy()

    def results(self, seasons=None):
        return self._filter_by_seasons(self._results, seasons)

    def races(self, seasons=None):
        if not seasons:
            return self._races.copy()
        return self._races[self._races["year"].isin(seasons)].copy()

    def drivers(self):
        return self._drivers.copy()

    def driver_standings(self, seasons=None):
        return self._filter_by_seasons(self._driver_standings, seasons)

    def constructor_standings(self, seasons=None):
        return self._filter_by_seasons(self._constructor_standings, seasons)

    def qualifying(self, seasons=None):
        return self._filter_by_seasons(self._qualifying, seasons)

    def get_available_seasons(self) -> list[int]:
        return sorted(self._races["year"].unique().tolist())


@pytest.fixture()
def fake_loader():
    """Возвращает FakeDataLoader."""
    return FakeDataLoader()


@pytest.fixture()
def _patch_data_loader(monkeypatch, fake_loader):
    """Подменяет get_data_loader на FakeDataLoader."""
    monkeypatch.setattr("src.core.data_loader.get_data_loader", lambda: fake_loader)
    monkeypatch.setattr("src.core.get_data_loader", lambda: fake_loader)
    monkeypatch.setattr("src.core.preprocessing.get_data_loader", lambda: fake_loader)
