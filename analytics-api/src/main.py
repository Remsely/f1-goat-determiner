from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from .analyzers import TierListAnalyzer
from .core import get_data_loader, settings
from .models import (
    DataStatsResponse,
    ErrorResponse,
    HealthResponse,
    SeasonsResponse,
    SeasonStats,
    TierListResponse,
)

app = FastAPI(
    title=settings.app_name,
    description="""
    F1 GOAT Determiner API - Инструмент для определения лучшего пилота Формулы-1 всех времён.
    Используются официальные данные F1 с 1950 по 2024 год.
    """,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
)

#
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/", response_model=HealthResponse, summary="Проверка работоспособности", tags=["Health"])
def root():
    """Возвращает информацию о сервере."""
    return HealthResponse(name=settings.app_name, version=settings.app_version, status="working")


@app.get(
    "/seasons",
    response_model=SeasonsResponse,
    summary="Список сезонов",
    tags=["Data"],
    responses={500: {"model": ErrorResponse}},
)
def get_seasons():
    """Возвращает список всех доступных сезонов F1."""
    try:
        loader = get_data_loader()
        seasons = loader.get_available_seasons()
        return SeasonsResponse(count=len(seasons), first=seasons[0], last=seasons[-1], seasons=seasons)
    except FileNotFoundError as e:
        raise HTTPException(status_code=500, detail=str(e)) from e


@app.get(
    "/stats",
    response_model=DataStatsResponse,
    summary="Статистика данных",
    tags=["Data"],
    responses={500: {"model": ErrorResponse}},
)
def get_stats():
    """Возвращает общую статистику по данным."""
    try:
        loader = get_data_loader()
        seasons = loader.get_available_seasons()
        return DataStatsResponse(
            total_races=len(loader.races()),
            total_drivers=len(loader.drivers()),
            total_results=len(loader.results()),
            seasons=SeasonStats(first=seasons[0], last=seasons[-1], count=len(seasons)),
        )
    except FileNotFoundError as e:
        raise HTTPException(status_code=500, detail=str(e)) from e


@app.get(
    "/tier-list",
    response_model=TierListResponse,
    summary="Тир-лист пилотов",
    tags=["Analysis"],
    responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}},
)
def get_tier_list(
    seasons: str | None = Query(None, description="Сезоны через запятую", example="2020,2021,2022,2023,2024"),
    n_tiers: int = Query(4, ge=2, le=6, description="Количество тиров (от 2 до 6)"),
    min_races: int = Query(10, ge=1, le=100, description="Минимум гонок для включения пилота"),
):
    """
    Генерирует тир-лист пилотов на основе кластеризации K-Means.

    Кластеризация учитывает:
    - Процент побед и подиумов
    - Процент чемпионских титулов
    - Среднюю позицию в чемпионате
    - Выступление относительно силы команды
    """
    season_list = None
    if seasons:
        try:
            season_list = [int(s.strip()) for s in seasons.split(",")]
        except ValueError as e:
            raise HTTPException(status_code=400, detail="Неверный формат сезонов.  Используйте: 2020,2021,2022") from e

    try:
        analyzer = TierListAnalyzer(seasons=season_list, n_tiers=n_tiers, min_races=min_races)
        return analyzer.analyze()
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) from e
