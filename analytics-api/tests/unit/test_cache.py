"""Тесты кеша TierListAnalyzer."""

import threading
import time
from unittest.mock import patch

from src.analyzers.tier_list import (
    _CACHE_MAX_SIZE,
    _cache,
    _cache_result,
    _get_cached_result,
    clear_cache,
)


class TestCache:
    def setup_method(self) -> None:
        clear_cache()

    def teardown_method(self) -> None:
        clear_cache()

    def test_cache_miss_returns_none(self) -> None:
        assert _get_cached_result("nonexistent") is None

    def test_cache_hit_returns_result(self) -> None:
        _cache_result("key1", {"data": "value"})
        assert _get_cached_result("key1") == {"data": "value"}

    def test_cache_ttl_expires(self) -> None:
        _cache_result("key1", {"data": "value"})

        with patch("src.analyzers.tier_list.time") as mock_time:
            mock_time.monotonic.return_value = time.monotonic() + 301
            assert _get_cached_result("key1") is None

    def test_cache_ttl_not_expired(self) -> None:
        _cache_result("key1", {"data": "value"})
        assert _get_cached_result("key1") == {"data": "value"}

    def test_cache_eviction_when_full(self) -> None:
        for i in range(_CACHE_MAX_SIZE):
            _cache_result(f"key_{i}", {"data": i})

        assert len(_cache) == _CACHE_MAX_SIZE

        _cache_result("new_key", {"data": "new"})

        assert len(_cache) == _CACHE_MAX_SIZE
        assert _get_cached_result("new_key") == {"data": "new"}

    def test_clear_cache_empties_all(self) -> None:
        _cache_result("key1", {"data": 1})
        _cache_result("key2", {"data": 2})

        clear_cache()

        assert len(_cache) == 0
        assert _get_cached_result("key1") is None

    def test_concurrent_writes_do_not_corrupt_cache(self) -> None:
        """Конкурентные записи не нарушают состояние кеша."""

        def write_entries(thread_id: int) -> None:
            for i in range(10):
                _cache_result(f"key_{thread_id}_{i}", {"thread": thread_id, "i": i})

        threads = [threading.Thread(target=write_entries, args=(n,)) for n in range(10)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        assert len(_cache) <= _CACHE_MAX_SIZE

    def test_concurrent_reads_and_writes_thread_safe(self) -> None:
        """Чтение и запись в разных потоках не вызывают ошибок."""
        _cache_result("shared_key", {"value": 42})

        errors: list[Exception] = []

        def read_loop() -> None:
            for _ in range(50):
                try:
                    _get_cached_result("shared_key")
                except Exception as e:
                    errors.append(e)

        def write_loop() -> None:
            for i in range(50):
                try:
                    _cache_result(f"write_{i}", {"i": i})
                except Exception as e:
                    errors.append(e)

        threads = [threading.Thread(target=read_loop) for _ in range(3)]
        threads += [threading.Thread(target=write_loop) for _ in range(3)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        assert errors == []
