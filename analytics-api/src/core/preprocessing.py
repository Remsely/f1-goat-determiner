import pandas as pd
from sklearn.preprocessing import StandardScaler

from .data_loader import get_data_loader


class F1Preprocessor:
    """Препроцессинг данных F1 для кластеризации."""

    FEATURE_COLS = [
        "win_rate",
        "podium_rate",
        "pole_rate",
        "avg_grid",
        "avg_finish",
        "best_finish",
        "grid_vs_finish",
        "avg_championship_position_pct",
        "title_rate",
        "performance_vs_team",
        "avg_team_position",
    ]

    def __init__(
            self,
            seasons: list[int] | None = None,
            min_races: int = 10
    ):
        self.seasons = seasons
        self.min_races = min_races
        self._loader = get_data_loader()
        self._data: pd.DataFrame | None = None

    def build_features(self) -> pd.DataFrame:
        """Строит таблицу признаков для всех пилотов."""

        results = self._loader.results().copy()
        races = self._loader.races().copy()
        drivers = self._loader.drivers().copy()
        driver_standings = self._loader.driver_standings().copy()
        constructor_standings = self._loader.constructor_standings().copy()
        qualifying = self._loader.qualifying().copy()

        if self.seasons:
            races = races[races["year"].isin(self.seasons)]
            race_ids = races["raceId"].tolist()
            results = results[results["raceId"].isin(race_ids)]
            qualifying = qualifying[qualifying["raceId"].isin(race_ids)]

        # === Поулы из квалификации ===
        poles_by_driver = (
            qualifying[qualifying["position"] == 1]
            .groupby("driverId")
            .size()
            .reset_index(name="total_poles")
        )

        # === Сила команды по сезонам ===
        year_final_race = races.groupby("year")["raceId"].max().reset_index()
        year_final_race.columns = ["year", "final_raceId"]

        team_strength = (
            constructor_standings
            .merge(year_final_race, left_on="raceId", right_on="final_raceId")
            .groupby(["constructorId", "year"])["position"]
            .min()
            .reset_index()
        )
        team_strength.columns = ["constructorId", "year", "team_position"]

        # === Позиция в чемпионате ===
        final_standings = (
            driver_standings
            .merge(year_final_race, left_on="raceId", right_on="final_raceId")
            [["driverId", "year", "position", "points"]]
        )

        if self.seasons:
            final_standings = final_standings[final_standings["year"].isin(self.seasons)]

        drivers_per_season = (
            final_standings
            .groupby("year")["driverId"]
            .nunique()
            .reset_index()
        )
        drivers_per_season.columns = ["year", "total_drivers"]

        final_standings = final_standings.merge(drivers_per_season, on="year", how="left")

        final_standings["championship_position_pct"] = (
                (final_standings["total_drivers"] - final_standings["position"]) /
                (final_standings["total_drivers"] - 1).replace(0, 1) * 100
        ).clip(0, 100)

        avg_championship = (
            final_standings
            .groupby("driverId")["championship_position_pct"]
            .mean()
            .reset_index()
        )
        avg_championship.columns = ["driverId", "avg_championship_position_pct"]

        career_seasons = (
            final_standings
            .groupby("driverId")["year"]
            .nunique()
            .reset_index()
        )
        career_seasons.columns = ["driverId", "career_seasons"]

        titles = (
            final_standings[final_standings["position"] == 1]
            .groupby("driverId")
            .size()
            .reset_index(name="total_titles")
        )

        # === Объединение результатов гонок ===
        merged = (
            results
            .merge(races[["raceId", "year"]], on="raceId", how="left")
            .merge(
                drivers[["driverId", "driverRef", "forename", "surname", "nationality"]],
                on="driverId",
                how="left"
            )
            .merge(team_strength, on=["constructorId", "year"], how="left")
        )

        merged = merged[merged["positionOrder"] > 0]

        # Заполняем пропуски в team_position медианой
        median_team_pos = merged["team_position"].median()
        merged["team_position"] = merged["team_position"].fillna(median_team_pos)

        # === Агрегация гоночных показателей ===
        features = (
            merged
            .groupby(["driverId", "driverRef", "forename", "surname", "nationality"])
            .agg(
                total_races=("raceId", "nunique"),
                total_wins=("positionOrder", lambda x: (x == 1).sum()),
                total_podiums=("positionOrder", lambda x: (x <= 3).sum()),
                avg_grid=("grid", "mean"),
                avg_finish=("positionOrder", "mean"),
                best_finish=("positionOrder", "min"),
                avg_team_position=("team_position", "mean"),
            )
            .reset_index()
        )

        # Добавляем поулы
        features = features.merge(poles_by_driver, on="driverId", how="left")
        features["total_poles"] = features["total_poles"].fillna(0).astype(int)

        # Добавляем чемпионат и титулы
        features = features.merge(avg_championship, on="driverId", how="left")
        features = features.merge(titles, on="driverId", how="left")
        features = features.merge(career_seasons, on="driverId", how="left")

        # Заполняем пропуски
        features["avg_championship_position_pct"] = features["avg_championship_position_pct"].fillna(0)
        features["total_titles"] = features["total_titles"].fillna(0).astype(int)
        features["career_seasons"] = features["career_seasons"].fillna(1).astype(int)

        # === Относительные показатели ===
        features["win_rate"] = features["total_wins"] / features["total_races"] * 100
        features["podium_rate"] = features["total_podiums"] / features["total_races"] * 100
        features["pole_rate"] = features["total_poles"] / features["total_races"] * 100
        features["grid_vs_finish"] = features["avg_finish"] - features["avg_grid"]
        features["performance_vs_team"] = features["avg_team_position"] - features["avg_finish"]
        features["title_rate"] = features["total_titles"] / features["career_seasons"] * 100

        # === Умное заполнение pole_rate ===
        features = self._smart_fill_pole_rate(features)

        features["full_name"] = features["forename"] + " " + features["surname"]

        features = features[features["total_races"] >= self.min_races]

        self._data = features
        return features

    def _smart_fill_pole_rate(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Умное заполнение pole_rate для пилотов без данных о квалификациях.
        Вместо 0 — пропорционально win_rate или podium_rate.
        """

        # Пилоты с поулами
        has_poles = df["total_poles"] > 0

        if has_poles.sum() < 5:
            # Недостаточно данных — оставляем как есть
            return df

        # Пилоты с поулами И победами — находим соотношение
        with_poles_and_wins = df[has_poles & (df["win_rate"] > 0)].copy()

        if len(with_poles_and_wins) < 3:
            return df

        with_poles_and_wins["pole_win_ratio"] = (
                with_poles_and_wins["pole_rate"] / with_poles_and_wins["win_rate"]
        )
        avg_pole_win_ratio = with_poles_and_wins["pole_win_ratio"].median()

        # Пилоты с поулами но без побед (только подиумы)
        with_poles_no_wins = df[has_poles & (df["win_rate"] == 0) & (df["podium_rate"] > 0)]
        if len(with_poles_no_wins) > 0:
            podium_pole_ratio = (
                    with_poles_no_wins["pole_rate"] / with_poles_no_wins["podium_rate"]
            ).median()
        else:
            podium_pole_ratio = 0.2

        # Заполняем для пилотов БЕЗ поулов, но с победами/подиумами
        no_poles_mask = ~has_poles

        for idx in df[no_poles_mask].index:
            row = df.loc[idx]
            if row["win_rate"] > 0:
                # Есть победы — используем соотношение от побед
                df.loc[idx, "pole_rate"] = row["win_rate"] * avg_pole_win_ratio
            elif row["podium_rate"] > 0:
                # Только подиумы — используем соотношение от подиумов
                df.loc[idx, "pole_rate"] = row["podium_rate"] * podium_pole_ratio
            # Если нет ни побед ни подиумов — оставляем 0

        return df

    def get_scaled_features(self) -> tuple[pd.DataFrame, pd.DataFrame]:
        """Возвращает исходные данные и масштабированные признаки."""
        if self._data is None:
            self.build_features()

        data = self._data.copy()

        for col in self.FEATURE_COLS:
            if col in data.columns:
                data[col] = data[col].fillna(data[col].median())

        scaler = StandardScaler()
        available_cols = [c for c in self.FEATURE_COLS if c in data.columns]
        scaled = scaler.fit_transform(data[available_cols])

        scaled_df = pd.DataFrame(
            scaled,
            columns=[f"{col}_scaled" for col in available_cols],
            index=data.index
        )

        return data, scaled_df
