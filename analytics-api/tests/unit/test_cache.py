"""Тесты кеша TierListAnalyzer."""

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
