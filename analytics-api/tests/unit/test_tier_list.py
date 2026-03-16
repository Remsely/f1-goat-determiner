"""Тесты для TierListAnalyzer."""

from collections.abc import Generator

import pytest

from src.analyzers.tier_list import TierListAnalyzer, clear_cache


@pytest.fixture(autouse=True)
def _clear_tier_cache() -> Generator[None]:
    """Очищаем кэш перед каждым тестом."""
    clear_cache()
    yield
    clear_cache()


@pytest.mark.usefixtures("_patch_data_loader")
class TestTierListAnalyzer:
    """Тесты analyze()."""

    def test_returns_valid_response_structure(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=4, min_races=1)
        result = analyzer.analyze()

        assert "meta" in result
        assert "tiers" in result
        assert result["meta"]["analyzer"] == "K-Means Clustering"
        assert result["meta"]["n_tiers"] == 4

    def test_all_drivers_assigned_to_tiers(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=4, min_races=1)
        result = analyzer.analyze()

        total_in_tiers = sum(tier["count"] for tier in result["tiers"].values())
        assert total_in_tiers == result["meta"]["total_drivers"]

    def test_tier_labels_are_valid(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=4, min_races=1)
        result = analyzer.analyze()

        valid_labels = {"S", "A", "B", "C", "D", "F"}
        for label in result["tiers"]:
            assert label in valid_labels

    def test_driver_stats_structure(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=2, min_races=1)
        result = analyzer.analyze()

        first_tier = next(iter(result["tiers"].values()))
        driver = first_tier["drivers"][0]

        assert "id" in driver
        assert "ref" in driver
        assert "name" in driver
        assert "nationality" in driver
        assert "stats" in driver

        stats = driver["stats"]
        expected_keys = {
            "races",
            "wins",
            "podiums",
            "poles",
            "titles",
            "win_rate",
            "podium_rate",
            "pole_rate",
            "title_rate",
            "avg_championship_pct",
            "avg_finish",
        }
        assert set(stats.keys()) == expected_keys

    def test_silhouette_score_in_range(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=3, min_races=1)
        result = analyzer.analyze()

        score = result["meta"]["silhouette_score"]
        assert -1.0 <= score <= 1.0

    def test_season_filter_works(self) -> None:
        analyzer = TierListAnalyzer(seasons=[2024], n_tiers=2, min_races=1)
        result = analyzer.analyze()

        assert result["meta"]["seasons"] == [2024]
        assert result["meta"]["total_drivers"] > 0

    def test_too_few_drivers_raises(self) -> None:
        with pytest.raises(ValueError):
            analyzer = TierListAnalyzer(n_tiers=6, min_races=100)
            analyzer.analyze()

    def test_cache_returns_same_result(self) -> None:
        analyzer1 = TierListAnalyzer(n_tiers=3, min_races=1)
        result1 = analyzer1.analyze()

        analyzer2 = TierListAnalyzer(n_tiers=3, min_races=1)
        result2 = analyzer2.analyze()

        assert result1 == result2

    def test_n_tiers_capped_at_max_labels(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=10, min_races=1)
        # TIER_LABELS has 6 entries, so n_tiers should be capped
        assert analyzer.n_tiers == 6

    def test_different_params_produce_different_cache_keys(self) -> None:
        a1 = TierListAnalyzer(n_tiers=2, min_races=1)
        a2 = TierListAnalyzer(n_tiers=3, min_races=1)
        a3 = TierListAnalyzer(seasons=[2024], n_tiers=2, min_races=1)

        assert a1._make_cache_key() != a2._make_cache_key()
        assert a1._make_cache_key() != a3._make_cache_key()

    def test_meta_contains_all_fields(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=3, min_races=1)
        result = analyzer.analyze()

        meta = result["meta"]
        assert "analyzer" in meta
        assert "seasons" in meta
        assert "n_tiers" in meta
        assert "min_races" in meta
        assert "total_drivers" in meta
        assert "silhouette_score" in meta

    def test_tier_drivers_sorted_by_win_rate(self) -> None:
        analyzer = TierListAnalyzer(n_tiers=2, min_races=1)
        result = analyzer.analyze()

        for tier in result["tiers"].values():
            win_rates = [d["stats"]["win_rate"] for d in tier["drivers"]]
            assert win_rates == sorted(win_rates, reverse=True)
