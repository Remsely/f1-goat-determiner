from abc import ABC, abstractmethod
from typing import Any


class BaseAnalyzer(ABC):
    """Базовый класс для всех анализаторов."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Название анализатора."""
        pass

    @abstractmethod
    def analyze(self) -> dict[str, Any]:
        """Выполняет анализ и возвращает результат."""
        pass
