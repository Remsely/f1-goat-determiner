CREATE TABLE constructor_standings
(
    id             INTEGER PRIMARY KEY,
    race_id        INTEGER       NOT NULL,
    constructor_id INTEGER       NOT NULL,
    points         DECIMAL(6, 2) NOT NULL,
    position       INTEGER       NOT NULL,
    position_text  VARCHAR(3)    NOT NULL,
    wins           INTEGER       NOT NULL,

    CONSTRAINT fk_constructor_standings_race
        FOREIGN KEY (race_id) REFERENCES races (id),

    CONSTRAINT fk_constructor_standings_constructor
        FOREIGN KEY (constructor_id) REFERENCES constructors (id),

    CONSTRAINT uk_constructor_standings_race_constructor
        UNIQUE (race_id, constructor_id)
);

CREATE INDEX idx_constructor_standings_constructor_id ON constructor_standings (constructor_id);
CREATE INDEX idx_constructor_standings_race_id ON constructor_standings (race_id);

COMMENT ON TABLE constructor_standings IS 'Constructor championship standings after each race';
COMMENT ON COLUMN constructor_standings.race_id IS 'Race after which standings are recorded';
