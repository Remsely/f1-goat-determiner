from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    """Ответ проверки здоровья API."""

    name: str = Field(examples=["F1 GOAT Determiner"])
    version: str = Field(examples=["0.1.0"])
    status: str = Field(examples=["working"])


class SeasonsResponse(BaseModel):
    """Список доступных сезонов."""

    count: int = Field(description="Количество сезонов", examples=[75])
    first: int = Field(description="Первый сезон", examples=[1950])
    last: int = Field(description="Последний сезон", examples=[2024])
    seasons: list[int] = Field(description="Список всех сезонов")


class SeasonStats(BaseModel):
    """Статистика по сезонам."""

    first: int = Field(examples=[1950])
    last: int = Field(examples=[2024])
    count: int = Field(examples=[75])


class DataStatsResponse(BaseModel):
    """Статистика по данным."""

    total_races: int = Field(description="Всего гонок", examples=[1125])
    total_drivers: int = Field(description="Всего пилотов", examples=[859])
    total_results: int = Field(description="Всего записей результатов", examples=[26080])
    seasons: SeasonStats


class DriverStats(BaseModel):
    """Статистика пилота."""

    races: int = Field(description="Количество гонок", examples=[308])
    wins: int = Field(description="Победы", examples=[91])
    podiums: int = Field(description="Подиумы", examples=[155])
    poles: int = Field(description="Поул-позиции", examples=[68])
    titles: int = Field(description="Чемпионские титулы", examples=[7])
    win_rate: float = Field(description="Процент побед", examples=[29.55])
    podium_rate: float = Field(description="Процент подиумов", examples=[50.32])
    pole_rate: float = Field(description="Процент поулов", examples=[22.08])
    title_rate: float = Field(description="Процент титулов от сезонов", examples=[36.84])
    avg_championship_pct: float = Field(
        description="Средняя позиция в чемпионате (0-100%, где 100% = 1 место)", examples=[84.26]
    )
    avg_finish: float = Field(description="Средняя финишная позиция", examples=[4.5])


class Driver(BaseModel):
    """Информация о пилоте."""

    id: int = Field(description="ID пилота", examples=[30])
    ref: str = Field(description="Короткое имя", examples=["michael_schumacher"])
    name: str = Field(description="Полное имя", examples=["Michael Schumacher"])
    nationality: str = Field(description="Национальность", examples=["German"])
    stats: DriverStats


class Tier(BaseModel):
    """Информация о тире."""

    count: int = Field(description="Количество пилотов в тире", examples=[10])
    avg_win_rate: float = Field(description="Средний процент побед", examples=[28.5])
    avg_podium_rate: float = Field(description="Средний процент подиумов", examples=[48.2])
    avg_pole_rate: float = Field(description="Средний процент поулов", examples=[15.3])
    avg_finish: float = Field(description="Средняя финишная позиция", examples=[4.5])
    drivers: list[Driver] = Field(description="Пилоты в тире")


class TierListMeta(BaseModel):
    """Метаданные тир-листа."""

    analyzer: str = Field(examples=["K-Means Clustering"])
    seasons: list[int] | None = Field(
        description="Фильтр по сезонам (null = все)", examples=[[2020, 2021, 2022, 2023, 2024]]
    )
    n_tiers: int = Field(description="Количество тиров", examples=[4])
    min_races: int = Field(description="Минимум гонок для включения", examples=[10])
    total_drivers: int = Field(description="Всего пилотов в анализе", examples=[374])
    silhouette_score: float = Field(
        description="Коэффициент силуэта (качество кластеризации, от -1 до 1)", examples=[0.339]
    )


class TierListResponse(BaseModel):
    """Ответ тир-листа."""

    meta: TierListMeta
    tiers: dict[str, Tier] = Field(description="Тиры (S, A, B, C... )")


class ErrorResponse(BaseModel):
    """Ответ с ошибкой."""

    detail: str = Field(description="Описание ошибки", examples=["Неверный формат сезонов"])
