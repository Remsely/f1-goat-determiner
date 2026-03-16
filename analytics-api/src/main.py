from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .api import analysis_router, data_router, health_router
from .core import close_connection_pool, settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    close_connection_pool()


app = FastAPI(
    title=settings.app_name,
    description="""
    F1 GOAT Determiner API - Инструмент для определения лучшего пилота Формулы-1 всех времён.
    Используются официальные данные F1 с 1950 года по настоящее время.
    """,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(data_router)
app.include_router(analysis_router)
