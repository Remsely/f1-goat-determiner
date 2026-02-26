CREATE TABLE driver_standings
(
    id            INTEGER PRIMARY KEY,
    race_id       INTEGER       NOT NULL,
    driver_id     INTEGER       NOT NULL,
    points        DECIMAL(6, 2) NOT NULL,
    position      INTEGER       NOT NULL,
    position_text VARCHAR(3)    NOT NULL,
    wins          INTEGER       NOT NULL,

    CONSTRAINT fk_driver_standings_race
        FOREIGN KEY (race_id) REFERENCES races (id),

    CONSTRAINT fk_driver_standings_driver
        FOREIGN KEY (driver_id) REFERENCES drivers (id),

    CONSTRAINT uk_driver_standings_race_driver
        UNIQUE (race_id, driver_id)
);

CREATE INDEX idx_driver_standings_driver_id ON driver_standings (driver_id);
CREATE INDEX idx_driver_standings_race_id ON driver_standings (race_id);
CREATE INDEX idx_driver_standings_position ON driver_standings (position);

COMMENT ON TABLE driver_standings IS 'Driver championship standings after each race';
COMMENT ON COLUMN driver_standings.race_id IS 'Race after which standings are recorded. For final standings - last race of season.';
COMMENT ON COLUMN driver_standings.wins IS 'Number of wins at the time of this race';
