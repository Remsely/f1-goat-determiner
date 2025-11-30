from fastapi import FastAPI

app = FastAPI(
    title="F1 GOAT Determiner",
    description="API для анализа лучших пилотов F1",
    version="0. 1.0"
)

@app. get("/")
def root():
    return {"message": "F1 GOAT Analyzer API", "status": "working"}


@app.get("/hello/{name}")
def hello(name: str):
    return {"message": f"Привет, {name}! "}
