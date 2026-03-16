#!/bin/bash
cat | grep -q '\.py"' || exit 0
ruff format analytics-api/
ruff check --fix --quiet analytics-api/ || true
