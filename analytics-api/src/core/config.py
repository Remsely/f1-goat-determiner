from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "F1 GOAT Determiner"
    app_version: str = "0.1.0"
    debug: bool = False

    api_host: str = "0.0.0.0"
    api_port: int = 8000

    db_host: str = "localhost"
    db_port: int = 5433
    db_name: str = "f1_goat_determiner"
    db_user: str = "f1user"
    db_password: str = "f1password"


settings = Settings()
