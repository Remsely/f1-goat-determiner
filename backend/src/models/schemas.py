from pydantic import BaseModel, Field


# ==================== Common ====================

class HealthResponse(BaseModel):
    """Ответ проверки здоровья API."""

    name: str = Field(example="F1 GOAT Determiner")
    version: str = Field(example="0.1.0")
    status: str = Field(example="working")


# ==================== Seasons ====================

class SeasonsResponse(BaseModel):
    """Список доступных сезонов."""

    count: int = Field(description="Количество сезонов", example=75)
    first: int = Field(description="Первый сезон", example=1950)
    last: int = Field(description="Последний сезон", example=2024)
    seasons: list[int] = Field(description="Список всех сезонов")


# ==================== Stats ====================

class SeasonStats(BaseModel):
    """Статистика по сезонам."""

    first: int = Field(example=1950)
    last: int = Field(example=2024)
    count: int = Field(example=75)


class DataStatsResponse(BaseModel):
    """Статистика по данным."""

    total_races: int = Field(description="Всего гонок", example=1125)
    total_drivers: int = Field(description="Всего пилотов", example=859)
    total_results: int = Field(description="Всего записей результатов", example=26080)
    seasons: SeasonStats


# ==================== Tier List ====================

class DriverStats(BaseModel):
    """Статистика пилота."""

    races: int = Field(description="Количество гонок", example=308)
    wins: int = Field(description="Победы", example=91)
    podiums: int = Field(description="Подиумы", example=155)
    titles: int = Field(description="Чемпионские титулы", example=7)
    win_rate: float = Field(description="Процент побед", example=29.55)
    podium_rate: float = Field(description="Процент подиумов", example=50.32)
    title_rate: float = Field(description="Процент титулов от сезонов", example=36.84)
    avg_championship_pct: float = Field(
        description="Средняя позиция в чемпионате (0-100%, где 100% = 1 место)",
        example=84.26
    )
    avg_finish: float = Field(
        description="Средняя финишная позиция",
        example=4.5
    )


class Driver(BaseModel):
    """Информация о пилоте."""

    id: int = Field(description="ID пилота", example=30)
    ref: str = Field(description="Короткое имя", example="michael_schumacher")
    name: str = Field(description="Полное имя", example="Michael Schumacher")
    nationality: str = Field(description="Национальность", example="German")
    stats: DriverStats


class Tier(BaseModel):
    """Информация о тире."""

    count: int = Field(description="Количество пилотов в тире", example=10)
    avg_win_rate: float = Field(description="Средний процент побед", example=28.5)
    avg_podium_rate: float = Field(description="Средний процент подиумов", example=48.2)
    avg_finish: float = Field(description="Средняя финишная позиция", example=4.5)
    drivers: list[Driver] = Field(description="Пилоты в тире")


class TierListMeta(BaseModel):
    """Метаданные тир-листа."""

    analyzer: str = Field(example="K-Means Clustering")
    seasons: list[int] | None = Field(
        description="Фильтр по сезонам (null = все)",
        example=[2020, 2021, 2022, 2023, 2024]
    )
    n_tiers: int = Field(description="Количество тиров", example=4)
    min_races: int = Field(description="Минимум гонок для включения", example=10)
    total_drivers: int = Field(description="Всего пилотов в анализе", example=374)
    silhouette_score: float = Field(
        description="Коэффициент силуэта (качество кластеризации, от -1 до 1)",
        example=0.339
    )


class TierListResponse(BaseModel):
    """Ответ тир-листа."""

    meta: TierListMeta
    tiers: dict[str, Tier] = Field(description="Тиры (S, A, B, C... )")


# ==================== Errors ====================

class ErrorResponse(BaseModel):
    """Ответ с ошибкой."""

    detail: str = Field(description="Описание ошибки", example="Неверный формат сезонов")
