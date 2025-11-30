from functools import lru_cache

import pandas as pd

from .config import settings


class F1DataLoader:
    """Загрузка и кэширование данных F1."""

    def __init__(self):
        self._data_dir = settings.raw_data_dir
        self._validate()

    def _validate(self) -> None:
        """Проверяет наличие данных."""
        if not self._data_dir.exists():
            raise FileNotFoundError(
                f"Data directory not found: {self._data_dir}"
            )

        required_files = [
            "results.csv",
            "races.csv",
            "drivers.csv",
            "driver_standings.csv",
            "constructor_standings.csv",
            "qualifying.csv"
        ]

        missing = [f for f in required_files if not (self._data_dir / f).exists()]

        if missing:
            raise FileNotFoundError(
                f"Data files not found: {missing}\n"
                f"Download in from Kaggle - "
                f"https://www.kaggle.com/datasets/rohanrao/formula-1-world-championship-1950-2020 "
                f"and place in {self._data_dir}"
            )

    @lru_cache(maxsize=1)
    def results(self) -> pd.DataFrame:
        return pd.read_csv(self._data_dir / "results.csv")

    @lru_cache(maxsize=1)
    def races(self) -> pd.DataFrame:
        return pd.read_csv(self._data_dir / "races.csv")

    @lru_cache(maxsize=1)
    def drivers(self) -> pd.DataFrame:
        return pd.read_csv(self._data_dir / "drivers.csv")

    @lru_cache(maxsize=1)
    def driver_standings(self) -> pd.DataFrame:
        return pd.read_csv(self._data_dir / "driver_standings.csv")

    @lru_cache(maxsize=1)
    def constructor_standings(self) -> pd.DataFrame:
        return pd.read_csv(self._data_dir / "constructor_standings.csv")

    @lru_cache(maxsize=1)
    def qualifying(self) -> pd.DataFrame:
        return pd.read_csv(self._data_dir / "qualifying.csv")

    def get_available_seasons(self) -> list[int]:
        """Список всех сезонов."""
        return sorted(self.races()["year"].unique().tolist())


_loader: F1DataLoader | None = None


def get_data_loader() -> F1DataLoader:
    """Возвращает загрузчик данных."""
    global _loader
    if _loader is None:
        _loader = F1DataLoader()
    return _loader
