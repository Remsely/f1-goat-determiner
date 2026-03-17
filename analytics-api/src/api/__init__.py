from .analysis import router as analysis_router
from .data import router as data_router
from .health import router as health_router

__all__ = ["health_router", "data_router", "analysis_router"]
