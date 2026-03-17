"""Интеграционные тесты API endpoints с реальной PostgreSQL."""

from fastapi.testclient import TestClient

from src.main import app


class TestHealthEndpoint:
    def test_health_returns_working(self) -> None:
        client = TestClient(app)
        response = client.get("/")

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "working"
        assert data["name"] == "F1 GOAT Determiner"

    def test_health_returns_version(self) -> None:
        client = TestClient(app)
        response = client.get("/")

        assert response.json()["version"] == "0.1.0"


class TestSeasonsEndpoint:
    def test_seasons_returns_available_seasons(self) -> None:
        client = TestClient(app)
        response = client.get("/seasons")

        assert response.status_code == 200
        data = response.json()
        assert data["count"] == 1
        assert data["first"] == 2024
        assert data["last"] == 2024
        assert data["seasons"] == [2024]


class TestStatsEndpoint:
    def test_stats_returns_data_counts(self) -> None:
        client = TestClient(app)
        response = client.get("/stats")

        assert response.status_code == 200
        data = response.json()
        assert data["total_races"] == 1
        assert data["total_drivers"] == 2
        assert data["total_results"] == 2
        assert data["seasons"]["first"] == 2024
        assert data["seasons"]["count"] == 1


class TestTierListEndpoint:
    def test_tier_list_with_insufficient_data_returns_error(self) -> None:
        """С 2 пилотами в тестовой БД кластеризация невозможна."""
        client = TestClient(app)
        response = client.get("/tier-list?n_tiers=2&min_races=1")

        # 2 пилота → ValueError от sklearn (silhouette_score needs >= 2 clusters with >= 2 samples each)
        assert response.status_code in (400, 500)

    def test_tier_list_invalid_seasons_format(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?seasons=abc")

        assert response.status_code == 400
