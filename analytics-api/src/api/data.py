import logging

import psycopg2
from fastapi import APIRouter, HTTPException

from ..core import get_data_loader
from ..models import DataStatsResponse, ErrorResponse, SeasonsResponse, SeasonStats

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Data"])


@router.get(
    "/seasons",
    response_model=SeasonsResponse,
    summary="Список сезонов",
    responses={500: {"model": ErrorResponse}, 503: {"model": ErrorResponse}},
)
def get_seasons() -> SeasonsResponse:
    """Возвращает список всех доступных сезонов F1."""
    try:
        loader = get_data_loader()
        seasons = loader.get_available_seasons()
    except psycopg2.Error as e:
        logger.error("Database error in /seasons", exc_info=True)
        raise HTTPException(status_code=500, detail="Ошибка при обращении к базе данных") from e

    if not seasons:
        raise HTTPException(status_code=503, detail="Нет данных. Выполните синхронизацию через data-sync-svc.")

    return SeasonsResponse(count=len(seasons), first=seasons[0], last=seasons[-1], seasons=seasons)


@router.get(
    "/stats",
    response_model=DataStatsResponse,
    summary="Статистика данных",
    responses={500: {"model": ErrorResponse}, 503: {"model": ErrorResponse}},
)
def get_stats() -> DataStatsResponse:
    """Возвращает общую статистику по данным."""
    try:
        loader = get_data_loader()
        seasons = loader.get_available_seasons()
    except psycopg2.Error as e:
        logger.error("Database error in /stats", exc_info=True)
        raise HTTPException(status_code=500, detail="Ошибка при обращении к базе данных") from e

    if not seasons:
        raise HTTPException(status_code=503, detail="Нет данных. Выполните синхронизацию через data-sync-svc.")

    return DataStatsResponse(
        total_races=loader.count_races(),
        total_drivers=loader.count_drivers(),
        total_results=loader.count_results(),
        seasons=SeasonStats(first=seasons[0], last=seasons[-1], count=len(seasons)),
    )
