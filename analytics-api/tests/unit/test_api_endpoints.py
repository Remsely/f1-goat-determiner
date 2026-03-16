"""Unit-тесты API endpoints (с моками, без БД)."""

from unittest.mock import patch

import psycopg2
from fastapi.testclient import TestClient

from src.analyzers.tier_list import clear_cache
from src.main import app


class TestHealthDegraded:
    def test_returns_degraded_when_db_unavailable(self) -> None:
        with patch("src.api.health.check_db_connection", return_value=False):
            client = TestClient(app)
            response = client.get("/")

        assert response.status_code == 200
        assert response.json()["status"] == "degraded"


class TestSeasonsErrorPaths:
    def test_returns_503_when_no_data(self) -> None:
        with patch("src.api.data.get_data_loader") as mock_loader:
            mock_loader.return_value.get_available_seasons.return_value = []
            client = TestClient(app)
            response = client.get("/seasons")

        assert response.status_code == 503
        assert "Нет данных" in response.json()["detail"]

    def test_returns_500_on_db_error(self) -> None:
        with patch("src.api.data.get_data_loader") as mock_loader:
            mock_loader.return_value.get_available_seasons.side_effect = psycopg2.OperationalError("connection refused")
            client = TestClient(app)
            response = client.get("/seasons")

        assert response.status_code == 500
        assert "БД" in response.json()["detail"]


class TestStatsErrorPaths:
    def test_returns_503_when_no_data(self) -> None:
        with patch("src.api.data.get_data_loader") as mock_loader:
            mock_loader.return_value.get_available_seasons.return_value = []
            client = TestClient(app)
            response = client.get("/stats")

        assert response.status_code == 503

    def test_returns_500_on_db_error(self) -> None:
        with patch("src.api.data.get_data_loader") as mock_loader:
            mock_loader.return_value.get_available_seasons.side_effect = psycopg2.OperationalError("connection refused")
            client = TestClient(app)
            response = client.get("/stats")

        assert response.status_code == 500


class TestTierListValidation:
    def setup_method(self) -> None:
        clear_cache()

    def teardown_method(self) -> None:
        clear_cache()

    def test_n_tiers_below_minimum_returns_422(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?n_tiers=1")
        assert response.status_code == 422

    def test_n_tiers_above_maximum_returns_422(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?n_tiers=7")
        assert response.status_code == 422

    def test_min_races_below_minimum_returns_422(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?min_races=0")
        assert response.status_code == 422

    def test_min_races_above_maximum_returns_422(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?min_races=101")
        assert response.status_code == 422

    def test_whitespace_seasons_string_returns_400(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?seasons=%20")
        # Пробел: split(",") → [" "], int(" ") → ValueError → 400
        assert response.status_code == 400

    def test_mixed_valid_invalid_seasons_returns_400(self) -> None:
        client = TestClient(app)
        response = client.get("/tier-list?seasons=2020,abc,2021")
        assert response.status_code == 400
