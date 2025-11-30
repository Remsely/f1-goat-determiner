from pathlib import Path

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "F1 GOAT Determiner"
    app_version: str = "0.1.0"
    debug: bool = False

    base_dir: Path = Path(__file__).resolve().parent.parent.parent

    @property
    def raw_data_dir(self) -> Path:
        return self.base_dir / "data" / "raw"

    api_host: str = "0.0.0.0"
    api_port: int = 8000


settings = Settings()
