"""Фикстуры для интеграционных тестов с testcontainers PostgreSQL."""

import os

import psycopg2
import pytest
from testcontainers.postgres import PostgresContainer

from tests.support.db import apply_schema, insert_test_data, truncate_all

# Отключаем Ryuk (может не работать на Windows Docker Desktop)
os.environ["TESTCONTAINERS_RYUK_DISABLED"] = "true"


@pytest.fixture(scope="session")
def postgres_container():
    """Поднимает PostgreSQL-контейнер на время тестовой сессии."""
    with PostgresContainer("postgres:18.2-alpine") as pg:
        yield pg


@pytest.fixture(scope="session")
def db_params(postgres_container) -> dict:
    """Параметры подключения к тестовой БД."""
    return {
        "host": postgres_container.get_container_host_ip(),
        "port": int(postgres_container.get_exposed_port(5432)),
        "dbname": postgres_container.dbname,
        "user": postgres_container.username,
        "password": postgres_container.password,
    }


@pytest.fixture(scope="session")
def _init_schema(db_params):
    """Создаёт схему в тестовой БД (один раз за сессию)."""
    conn = psycopg2.connect(**db_params)
    apply_schema(conn)
    conn.close()


@pytest.fixture(autouse=True)
def _setup_test_data(db_params, _init_schema):
    """Очищает и заполняет данные перед каждым тестом."""
    conn = psycopg2.connect(**db_params)
    truncate_all(conn)
    insert_test_data(conn)
    conn.close()


@pytest.fixture(autouse=True)
def _patch_db_settings(monkeypatch, db_params, _init_schema):
    """Переопределяет settings на тестовый PostgreSQL и сбрасывает пул."""
    import src.core.db as db_module
    from src.core import settings

    # Закрываем текущий пул если есть
    db_module.close_connection_pool()

    # Подменяем settings
    monkeypatch.setattr(settings, "db_host", db_params["host"])
    monkeypatch.setattr(settings, "db_port", db_params["port"])
    monkeypatch.setattr(settings, "db_name", db_params["dbname"])
    monkeypatch.setattr(settings, "db_user", db_params["user"])
    monkeypatch.setattr(settings, "db_password", db_params["password"])

    # Сбрасываем singleton data loader
    import src.core.data_loader as loader_module

    loader_module._loader = None

    yield

    # Cleanup
    db_module.close_connection_pool()
    loader_module._loader = None
