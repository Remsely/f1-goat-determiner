import logging

import psycopg2
from fastapi import APIRouter, HTTPException, Query

from ..analyzers import TierListAnalyzer
from ..models import ErrorResponse, TierListResponse

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Analysis"])


@router.get(
    "/tier-list",
    response_model=TierListResponse,
    summary="Тир-лист пилотов",
    responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}},
)
def get_tier_list(
    seasons: str | None = Query(None, description="Сезоны через запятую", examples=["2020,2021,2022,2023,2024"]),
    n_tiers: int = Query(4, ge=2, le=6, description="Количество тиров (от 2 до 6)"),
    min_races: int = Query(10, ge=1, le=100, description="Минимум гонок для включения пилота"),
) -> TierListResponse:
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
            raise HTTPException(status_code=400, detail="Неверный формат сезонов. Используйте: 2020,2021,2022") from e

    try:
        analyzer = TierListAnalyzer(seasons=season_list, n_tiers=n_tiers, min_races=min_races)
        return analyzer.analyze()
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except psycopg2.Error as e:
        logger.error("Database error during tier list generation", exc_info=True)
        raise HTTPException(status_code=500, detail="Ошибка при обращении к базе данных") from e
