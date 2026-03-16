"""Тесты для F1Preprocessor."""

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
