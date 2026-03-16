# Python Coding Style (analytics-api)

## Type Hints

All functions must have full type annotations — arguments and return type.

```python
# Good
def get_driver_standings(season: int) -> list[DriverStanding]:
    ...


# Bad
def get_driver_standings(season):
    ...
```

Use `from __future__ import annotations` only when needed for forward references.
Prefer built-in generics (`list[str]`, `dict[str, int]`) over `typing.List`, `typing.Dict` (Python 3.10+).

## Pydantic v2

Always use v2 API. Never use deprecated v1 patterns.

```python
# Good
class DriverResult(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    driver_id: str = Field(alias="driverId")

    @field_validator("points", mode="before")
    @classmethod
    def parse_points(cls, v: str) -> float:
        return float(v)


# Bad — v1 patterns
class DriverResult(BaseModel):
    class Config:
        allow_population_by_field_name = True

    @validator("points")
    def parse_points(cls, v):
        ...
```

## FastAPI Patterns

Use `APIRouter` per domain area. Never put routes directly in `main.py`.

```python
# Good — api/drivers.py
router = APIRouter(prefix="/drivers", tags=["drivers"])


@router.get("/{driver_id}", response_model=DriverResponse)
async def get_driver(driver_id: str) -> DriverResponse:
    ...
```

Raise `HTTPException` with explicit `status_code` and descriptive `detail`.
Never return raw dicts — always use response Pydantic models.

## Exception Handling

Never catch broad exceptions (`Exception`, `BaseException`). Catch specific types.

```python
# Good
try:
    result = await db.fetch_one(query)
except asyncpg.PostgresError as e:
    raise HTTPException(status_code=500, detail="Database error") from e

# Bad
try:
    result = await db.fetch_one(query)
except Exception:
    ...
```

## Naming

- Files, functions, variables: `snake_case`
- Classes: `PascalCase`
- Constants: `UPPER_SNAKE_CASE`
- Private helpers: `_leading_underscore`

## Imports

Sorted by ruff (isort rules): stdlib → third-party → local. Never use wildcard imports (`from module import *`).

## File Size

Target 100–200 lines per file, max 300. Extract helpers into separate modules.

## Testing (pytest)

Use `pytest` with `assert` statements. No unittest-style `self.assert*`.
One logical scenario per test function. Name tests: `test_<what>_<when>_<expected>`.

```python
# Good
def test_cluster_drivers_returns_four_tiers_for_full_dataset(sample_results: pd.DataFrame) -> None:
    tiers = cluster_drivers(sample_results)
    assert len(tiers) == 4
```

Use `pytest.raises` for exception checks:

```python
with pytest.raises(ValueError, match="season must be positive"):
    get_standings(season=-1)
```
