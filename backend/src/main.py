from fastapi import FastAPI, HTTPException

from .core import settings, get_data_loader

app = FastAPI(
    title=settings.app_name,
    description="API для определения лучшего пилота F1",
    version=settings.app_version
)


@app.get("/")
def root():
    """Главная страница."""
    return {
        "name": settings.app_name,
        "version": settings.app_version,
        "status": "working"
    }


@app.get("/seasons")
def get_seasons():
    """Список доступных сезонов."""
    try:
        loader = get_data_loader()
        seasons = loader.get_available_seasons()
        return {
            "count": len(seasons),
            "first": seasons[0],
            "last": seasons[-1],
            "seasons": seasons
        }
    except FileNotFoundError as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/stats")
def get_stats():
    """Статистика по данным."""
    try:
        loader = get_data_loader()
        return {
            "total_races": len(loader.races()),
            "total_drivers": len(loader.drivers()),
            "total_results": len(loader.results()),
            "seasons": {
                "first": loader.get_available_seasons()[0],
                "last": loader.get_available_seasons()[-1],
                "count": len(loader.get_available_seasons())
            }
        }
    except FileNotFoundError as e:
        raise HTTPException(status_code=500, detail=str(e))
