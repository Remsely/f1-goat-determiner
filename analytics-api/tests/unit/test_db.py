"""Тесты для src/core/db.py — пул соединений и read_sql."""

from collections.abc import Iterator
from unittest.mock import MagicMock, patch

import psycopg2
import pytest

import src.core.db as db_module


@pytest.fixture(autouse=True)
def _reset_connection_pool() -> Iterator[None]:
    """Сохраняет и восстанавливает глобальный пул после каждого теста."""
    original = db_module._connection_pool
    yield
    db_module._connection_pool = original


class TestReadSql:
    """Тесты read_sql() — обработка ошибок и управление соединениями."""

    def _mock_pool(self) -> tuple[MagicMock, MagicMock]:
        mock_conn = MagicMock()
        mock_pool = MagicMock()
        mock_pool.getconn.return_value = mock_conn
        return mock_pool, mock_conn

    def test_returns_dataframe_on_success(self) -> None:
        import pandas as pd

        mock_pool, mock_conn = self._mock_pool()
        expected_df = pd.DataFrame({"col": [1, 2, 3]})

        with patch("src.core.db.get_connection_pool", return_value=mock_pool):
            with patch("src.core.db.pd.read_sql", return_value=expected_df):
                result = db_module.read_sql("SELECT 1")

        assert result.equals(expected_df)

    def test_rollback_and_putconn_called_on_success(self) -> None:
        mock_pool, mock_conn = self._mock_pool()

        with patch("src.core.db.get_connection_pool", return_value=mock_pool):
            with patch("src.core.db.pd.read_sql", return_value=MagicMock()):
                db_module.read_sql("SELECT 1")

        mock_conn.reset.assert_not_called()
        mock_conn.rollback.assert_called_once()
        mock_pool.putconn.assert_called_once_with(mock_conn)

    def test_conn_reset_called_on_psycopg2_error(self) -> None:
        mock_pool, mock_conn = self._mock_pool()

        with patch("src.core.db.get_connection_pool", return_value=mock_pool):
            with patch("src.core.db.pd.read_sql", side_effect=psycopg2.Error("db error")):
                with pytest.raises(psycopg2.Error):
                    db_module.read_sql("SELECT 1")

        mock_conn.reset.assert_called_once()

    def test_rollback_and_putconn_called_even_on_psycopg2_error(self) -> None:
        mock_pool, mock_conn = self._mock_pool()

        with patch("src.core.db.get_connection_pool", return_value=mock_pool):
            with patch("src.core.db.pd.read_sql", side_effect=psycopg2.Error("db error")):
                with pytest.raises(psycopg2.Error):
                    db_module.read_sql("SELECT 1")

        mock_conn.rollback.assert_called_once()
        mock_pool.putconn.assert_called_once_with(mock_conn)

    def test_psycopg2_error_is_reraised(self) -> None:
        mock_pool, _ = self._mock_pool()
        original_error = psycopg2.Error("original error")

        with patch("src.core.db.get_connection_pool", return_value=mock_pool):
            with patch("src.core.db.pd.read_sql", side_effect=original_error):
                with pytest.raises(psycopg2.Error) as exc_info:
                    db_module.read_sql("SELECT 1")

        assert exc_info.value is original_error


class TestCloseConnectionPool:
    """Тесты close_connection_pool() — корректное завершение и идемпотентность."""

    def test_closes_open_pool(self) -> None:
        mock_pool = MagicMock()
        mock_pool.closed = False
        db_module._connection_pool = mock_pool

        db_module.close_connection_pool()

        mock_pool.closeall.assert_called_once()
        assert db_module._connection_pool is None

    def test_idempotent_when_pool_is_none(self) -> None:
        db_module._connection_pool = None

        db_module.close_connection_pool()  # не должно кидать исключение

        assert db_module._connection_pool is None

    def test_idempotent_on_second_call(self) -> None:
        mock_pool = MagicMock()
        mock_pool.closed = False
        db_module._connection_pool = mock_pool

        db_module.close_connection_pool()
        db_module.close_connection_pool()  # второй вызов — пул уже None

        mock_pool.closeall.assert_called_once()

    def test_skips_already_closed_pool(self) -> None:
        mock_pool = MagicMock()
        mock_pool.closed = True
        db_module._connection_pool = mock_pool

        db_module.close_connection_pool()

        mock_pool.closeall.assert_not_called()
