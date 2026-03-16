import logging

import pandas as pd
import psycopg2
from psycopg2 import pool

from .config import settings

logger = logging.getLogger(__name__)

_connection_pool: pool.SimpleConnectionPool | None = None


def get_connection_pool() -> pool.SimpleConnectionPool:
    """Возвращает пул соединений, создавая его при необходимости."""
    global _connection_pool
    if _connection_pool is None or _connection_pool.closed:
        _connection_pool = pool.SimpleConnectionPool(
            minconn=1,
            maxconn=5,
            host=settings.db_host,
            port=settings.db_port,
            dbname=settings.db_name,
            user=settings.db_user,
            password=settings.db_password,
        )
    return _connection_pool


def read_sql(query: str, params: tuple | dict | None = None) -> pd.DataFrame:
    """Выполняет SQL-запрос и возвращает результат как DataFrame."""
    p = get_connection_pool()
    conn = p.getconn()
    try:
        return pd.read_sql(query, conn, params=params)
    except Exception:
        conn.reset()
        raise
    finally:
        p.putconn(conn)


def check_db_connection() -> bool:
    """Проверяет доступность БД."""
    try:
        p = get_connection_pool()
        conn = p.getconn()
        try:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
            return True
        finally:
            p.putconn(conn)
    except psycopg2.Error:
        logger.warning("Database health check failed", exc_info=True)
        return False


def close_connection_pool() -> None:
    """Закрывает пул соединений."""
    global _connection_pool
    if _connection_pool is not None and not _connection_pool.closed:
        _connection_pool.closeall()
        _connection_pool = None
