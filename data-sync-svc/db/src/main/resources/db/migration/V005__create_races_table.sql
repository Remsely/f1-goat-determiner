CREATE TABLE races
(
    id         INTEGER PRIMARY KEY,
    season     INTEGER      NOT NULL,
    round      INTEGER      NOT NULL,
    circuit_id INTEGER      NOT NULL,
    name       VARCHAR(100) NOT NULL,
    date       DATE         NOT NULL,
    time       TIME,

    CONSTRAINT fk_races_circuit FOREIGN KEY (circuit_id)
        REFERENCES circuits (id),

    CONSTRAINT uk_races_season_round
        UNIQUE (season, round)
);

CREATE INDEX idx_races_season ON races (season);
CREATE INDEX idx_races_circuit_id ON races (circuit_id);
CREATE INDEX idx_races_date ON races (date);

COMMENT ON TABLE races IS 'F1 races';
COMMENT ON COLUMN races.season IS 'Season year';
COMMENT ON COLUMN races.round IS 'Round number within season (1-based)';
COMMENT ON COLUMN races.time IS 'Race start time in UTC (NULL for historical races)';
