"""Тесты для F1Preprocessor."""

import pandas as pd
import pytest

from src.core.preprocessing import F1Preprocessor


@pytest.mark.usefixtures("_patch_data_loader")
class TestBuildFeatures:
    """Тесты build_features()."""

    def test_returns_all_drivers_above_min_races(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        assert len(features) == 8

    def test_min_races_filters_drivers(self) -> None:
        preprocessor = F1Preprocessor(min_races=100)
        features = preprocessor.build_features()

        assert len(features) == 0

    def test_win_rate_for_top_drivers(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        hamilton = features[features["driverRef"] == "hamilton"].iloc[0]
        verstappen = features[features["driverRef"] == "verstappen"].iloc[0]

        # Hamilton: 3 wins / 6 races = 50%
        assert hamilton["win_rate"] == pytest.approx(50.0)
        # Verstappen: 3 wins / 6 races = 50%
        assert verstappen["win_rate"] == pytest.approx(50.0)

    def test_no_wins_driver_has_zero_win_rate(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        latifi = features[features["driverRef"] == "latifi"].iloc[0]
        assert latifi["win_rate"] == pytest.approx(0.0)

    def test_podium_rate_calculation(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        hamilton = features[features["driverRef"] == "hamilton"].iloc[0]
        # Hamilton: finishes 1,2,1,2,1,3 → 6 podiums / 6 races = 100%
        assert hamilton["podium_rate"] == pytest.approx(100.0)

    def test_pole_rate_from_qualifying(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        hamilton = features[features["driverRef"] == "hamilton"].iloc[0]
        # Hamilton: 3 poles / 6 races = 50%
        assert hamilton["pole_rate"] == pytest.approx(50.0)

    def test_title_rate_calculation(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        hamilton = features[features["driverRef"] == "hamilton"].iloc[0]
        # Hamilton: 1 title / 2 seasons = 50%
        assert hamilton["title_rate"] == pytest.approx(50.0)

    def test_season_filter(self) -> None:
        preprocessor = F1Preprocessor(seasons=[2024], min_races=1)
        features = preprocessor.build_features()

        # Все 8 пилотов участвовали в 2024, но каждый только в 3 гонках
        hamilton = features[features["driverRef"] == "hamilton"].iloc[0]
        assert hamilton["total_races"] == 3

    def test_full_name_column(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        hamilton = features[features["driverRef"] == "hamilton"].iloc[0]
        assert hamilton["full_name"] == "Lewis Hamilton"

    def test_feature_columns_present(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        for col in F1Preprocessor.FEATURE_COLS:
            assert col in features.columns, f"Отсутствует колонка: {col}"

    def test_grid_vs_finish_calculation(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        for _, row in features.iterrows():
            expected = row["avg_finish"] - row["avg_grid"]
            assert row["grid_vs_finish"] == pytest.approx(expected)

    def test_performance_vs_team_calculation(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        features = preprocessor.build_features()

        for _, row in features.iterrows():
            expected = row["avg_team_position"] - row["avg_finish"]
            assert row["performance_vs_team"] == pytest.approx(expected)

    def test_all_drivers_filtered_returns_empty(self) -> None:
        preprocessor = F1Preprocessor(min_races=999)
        features = preprocessor.build_features()

        assert len(features) == 0

    def test_multiple_season_filter(self) -> None:
        preprocessor = F1Preprocessor(seasons=[2023, 2024], min_races=1)
        features = preprocessor.build_features()

        assert len(features) == 8

    def test_nonexistent_season_returns_empty(self) -> None:
        preprocessor = F1Preprocessor(seasons=[1900], min_races=1)
        features = preprocessor.build_features()

        assert len(features) == 0


@pytest.mark.usefixtures("_patch_data_loader")
class TestGetScaledFeatures:
    """Тесты get_scaled_features()."""

    def test_returns_data_and_scaled(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        data, scaled_df = preprocessor.get_scaled_features()

        assert len(data) == len(scaled_df)
        assert len(data) > 0

    def test_scaled_columns_have_suffix(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        _, scaled_df = preprocessor.get_scaled_features()

        for col in scaled_df.columns:
            assert col.endswith("_scaled")

    def test_build_features_called_lazily(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        assert preprocessor._data is None

        preprocessor.get_scaled_features()
        assert preprocessor._data is not None

    def test_scaled_features_same_length_as_data(self) -> None:
        preprocessor = F1Preprocessor(min_races=1)
        data, scaled_df = preprocessor.get_scaled_features()

        assert len(data) == len(scaled_df)
        assert len(data.columns) > len(scaled_df.columns)


class TestSmartFillPoleRate:
    """Тесты _smart_fill_pole_rate() — умное заполнение pole_rate."""

    def _preprocessor(self) -> F1Preprocessor:
        # Создаём экземпляр без __init__ — метод не использует self._loader
        return F1Preprocessor.__new__(F1Preprocessor)

    def _make_df(
        self,
        total_poles: list[int],
        win_rate: list[float],
        podium_rate: list[float],
        pole_rate: list[float],
    ) -> pd.DataFrame:
        return pd.DataFrame(
            {
                "total_poles": total_poles,
                "win_rate": win_rate,
                "podium_rate": podium_rate,
                "pole_rate": pole_rate,
            }
        )

    def test_fewer_than_5_pole_drivers_returns_unchanged(self) -> None:
        """Если < 5 пилотов с поулами — DataFrame не изменяется."""
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 0],  # 4 с поулами < 5
            win_rate=[50.0, 40.0, 30.0, 20.0, 10.0],
            podium_rate=[80.0, 60.0, 50.0, 40.0, 30.0],
            pole_rate=[25.0, 20.0, 10.0, 5.0, 0.0],
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        assert result.loc[4, "pole_rate"] == pytest.approx(0.0)

    def test_fewer_than_3_poles_and_wins_returns_unchanged(self) -> None:
        """Если < 3 пилотов с поулами И победами — DataFrame не изменяется."""
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 1, 0],  # 5 с поулами, но только 2 с победами
            win_rate=[50.0, 40.0, 0.0, 0.0, 0.0, 10.0],
            podium_rate=[80.0, 60.0, 50.0, 40.0, 30.0, 20.0],
            pole_rate=[25.0, 15.0, 10.0, 8.0, 5.0, 0.0],
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        assert result.loc[5, "pole_rate"] == pytest.approx(0.0)

    def test_fills_pole_rate_from_win_rate(self) -> None:
        """Пилот без поулов, но с победами — pole_rate заполняется через win_rate."""
        # Все поул-пилоты имеют одинаковое соотношение pole_rate/win_rate = 0.5
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 1, 0],
            win_rate=[40.0, 30.0, 20.0, 10.0, 5.0, 20.0],
            podium_rate=[80.0, 70.0, 60.0, 50.0, 30.0, 60.0],
            pole_rate=[20.0, 15.0, 10.0, 5.0, 2.5, 0.0],  # pole/win = 0.5 везде
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        # avg_pole_win_ratio = 0.5; заполнено: 20.0 * 0.5 = 10.0
        assert result.loc[5, "pole_rate"] == pytest.approx(10.0)

    def test_fills_pole_rate_from_podium_rate_using_actual_ratio(self) -> None:
        """Нет побед, есть подиумы — pole_rate заполняется через podium_rate с реальным соотношением."""
        # Поул-пилот без побед: pole_rate=8, podium_rate=40 → ratio=0.2
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 1, 0],
            win_rate=[40.0, 30.0, 20.0, 10.0, 0.0, 0.0],  # driver 4: poles+no wins
            podium_rate=[80.0, 70.0, 60.0, 50.0, 40.0, 30.0],
            pole_rate=[20.0, 15.0, 10.0, 5.0, 8.0, 0.0],  # driver 4: pole/podium=0.2
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        # podium_pole_ratio = 8/40 = 0.2; заполнено: 30 * 0.2 = 6.0
        assert result.loc[5, "pole_rate"] == pytest.approx(6.0)

    def test_fills_pole_rate_from_podium_rate_with_default_ratio(self) -> None:
        """Нет пилотов с поулами+без побед — используется дефолтный ratio=0.2."""
        # Все поул-пилоты имеют победы → with_poles_no_wins пуст → podium_pole_ratio=0.2
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 1, 0],
            win_rate=[40.0, 30.0, 20.0, 10.0, 5.0, 0.0],  # driver 5: no wins, no poles
            podium_rate=[80.0, 70.0, 60.0, 50.0, 30.0, 30.0],
            pole_rate=[20.0, 15.0, 10.0, 5.0, 2.5, 0.0],
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        # podium_pole_ratio = 0.2 (default); заполнено: 30 * 0.2 = 6.0
        assert result.loc[5, "pole_rate"] == pytest.approx(6.0)

    def test_no_wins_no_podiums_stays_zero(self) -> None:
        """Нет ни побед, ни подиумов — pole_rate остаётся 0."""
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 1, 0],
            win_rate=[40.0, 30.0, 20.0, 10.0, 5.0, 0.0],
            podium_rate=[80.0, 70.0, 60.0, 50.0, 30.0, 0.0],  # driver 5: no podiums
            pole_rate=[20.0, 15.0, 10.0, 5.0, 2.5, 0.0],
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        assert result.loc[5, "pole_rate"] == pytest.approx(0.0)

    def test_drivers_with_poles_pole_rate_unchanged(self) -> None:
        """Поул-ставки пилотов с реальными поулами не изменяются."""
        df = self._make_df(
            total_poles=[1, 1, 1, 1, 1, 0],
            win_rate=[40.0, 30.0, 20.0, 10.0, 5.0, 20.0],
            podium_rate=[80.0, 70.0, 60.0, 50.0, 30.0, 60.0],
            pole_rate=[20.0, 15.0, 10.0, 5.0, 2.5, 0.0],
        )

        result = self._preprocessor()._smart_fill_pole_rate(df)

        assert result.loc[0, "pole_rate"] == pytest.approx(20.0)
        assert result.loc[1, "pole_rate"] == pytest.approx(15.0)
        assert result.loc[2, "pole_rate"] == pytest.approx(10.0)
