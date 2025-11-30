import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score

from .base import BaseAnalyzer
from ..core.preprocessing import F1Preprocessor


class TierListAnalyzer(BaseAnalyzer):
    """Создание тир-листа на основе кластеризации K-Means."""

    TIER_LABELS = ["S", "A", "B", "C", "D", "F"]

    SCALED_COLS = [
        "win_rate_scaled",
        "podium_rate_scaled",
        "avg_grid_scaled",
        "avg_finish_scaled",
        "best_finish_scaled",
        "grid_vs_finish_scaled",
        "avg_championship_position_pct_scaled",
        "title_rate_scaled",
        "performance_vs_team_scaled",
        "avg_team_position_scaled",
    ]

    def __init__(
            self,
            seasons: list[int] | None = None,
            n_tiers: int = 4,
            min_races: int = 10
    ):
        self.seasons = seasons
        self.n_tiers = min(n_tiers, len(self.TIER_LABELS))
        self.min_races = min_races

        self._preprocessor = F1Preprocessor(seasons=seasons, min_races=min_races)
        self._result_data: pd.DataFrame | None = None
        self._silhouette_score: float = 0.0

    @property
    def name(self) -> str:
        return "K-Means Clustering"

    def analyze(self) -> dict:
        """Выполняет кластеризацию и возвращает тир-лист."""

        cache_key = self._make_cache_key()
        cached = _get_cached_result(cache_key)
        if cached is not None:
            return cached

        data, scaled_df = self._preprocessor.get_scaled_features()

        full_data = pd.concat(
            [data.reset_index(drop=True), scaled_df.reset_index(drop=True)],
            axis=1
        )

        if len(full_data) < self.n_tiers:
            raise ValueError(
                f"Недостаточно данных: {len(full_data)} пилотов для {self.n_tiers} тиров"
            )

        available_cols = [c for c in self.SCALED_COLS if c in full_data.columns]
        features = full_data[available_cols]

        kmeans = KMeans(n_clusters=self.n_tiers, random_state=42, n_init="auto")
        full_data["cluster"] = kmeans.fit_predict(features)

        self._silhouette_score = silhouette_score(features, full_data["cluster"])

        cluster_quality = (
            full_data
            .groupby("cluster")
            .agg({
                "win_rate": "mean",
                "podium_rate": "mean",
                "avg_championship_position_pct": "mean",
                "avg_finish": "mean",
            })
        )

        cluster_quality["composite_score"] = (
                cluster_quality["win_rate"] * 0.3 +
                cluster_quality["podium_rate"] * 0.2 +
                cluster_quality["avg_championship_position_pct"] * 0.3 +
                (20 - cluster_quality["avg_finish"]) * 2  # Инвертируем и масштабируем
        )

        cluster_quality = cluster_quality.sort_values("composite_score", ascending=False)

        cluster_to_tier = {
            cluster: self.TIER_LABELS[i]
            for i, cluster in enumerate(cluster_quality.index)
        }
        full_data["tier"] = full_data["cluster"].map(cluster_to_tier)

        self._result_data = full_data

        result = self._build_response()

        _cache_result(cache_key, result)

        return result

    def _make_cache_key(self) -> str:
        """Создаёт ключ для кэша."""
        seasons_str = ",".join(map(str, sorted(self.seasons))) if self.seasons else "all"
        return f"{seasons_str}_{self.n_tiers}_{self.min_races}"

    def _build_response(self) -> dict:
        """Формирует ответ API."""

        tiers = {}

        for tier_label in self.TIER_LABELS[:self.n_tiers]:
            tier_data = self._result_data[self._result_data["tier"] == tier_label].copy()

            if len(tier_data) == 0:
                continue

            tier_data = tier_data.sort_values(
                ["win_rate", "podium_rate", "avg_championship_position_pct"],
                ascending=[False, False, False]
            )

            drivers = []
            for _, row in tier_data.iterrows():
                drivers.append({
                    "id": int(row["driverId"]),
                    "ref": row["driverRef"],
                    "name": row["full_name"],
                    "nationality": row["nationality"],
                    "stats": {
                        "races": int(row["total_races"]),
                        "wins": int(row["total_wins"]),
                        "podiums": int(row["total_podiums"]),
                        "titles": int(row["total_titles"]),
                        "win_rate": round(row["win_rate"], 2),
                        "podium_rate": round(row["podium_rate"], 2),
                        "title_rate": round(row["title_rate"], 2),
                        "avg_championship_pct": round(row["avg_championship_position_pct"], 2),
                        "avg_finish": round(row["avg_finish"], 2),
                    }
                })

            tiers[tier_label] = {
                "count": len(drivers),
                "avg_win_rate": round(tier_data["win_rate"].mean(), 2),
                "avg_podium_rate": round(tier_data["podium_rate"].mean(), 2),
                "avg_finish": round(tier_data["avg_finish"].mean(), 2),
                "drivers": drivers
            }

        return {
            "meta": {
                "analyzer": self.name,
                "seasons": self.seasons,
                "n_tiers": self.n_tiers,
                "min_races": self.min_races,
                "total_drivers": len(self._result_data),
                "silhouette_score": round(self._silhouette_score, 3),
            },
            "tiers": tiers
        }


_cache: dict[str, dict] = {}
_CACHE_MAX_SIZE = 100


def _get_cached_result(key: str) -> dict | None:
    """Получает результат из кэша."""
    return _cache.get(key)


def _cache_result(key: str, result: dict) -> None:
    """Сохраняет результат в кэш."""
    if len(_cache) >= _CACHE_MAX_SIZE:
        first_key = next(iter(_cache))
        del _cache[first_key]
    _cache[key] = result


def clear_cache() -> None:
    """Очищает кэш (для тестов)."""
    _cache.clear()
