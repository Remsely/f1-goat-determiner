from fastapi import APIRouter

from ..core import check_db_connection, settings
from ..models import HealthResponse

router = APIRouter(tags=["Health"])


@router.get("/", response_model=HealthResponse, summary="Проверка работоспособности")
def root() -> HealthResponse:
    """Возвращает информацию о сервере."""
    db_ok = check_db_connection()
    return HealthResponse(
        name=settings.app_name,
        version=settings.app_version,
        status="working" if db_ok else "degraded",
    )
