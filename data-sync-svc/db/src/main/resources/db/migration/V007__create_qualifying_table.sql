CREATE TABLE qualifying
(
    id             INTEGER PRIMARY KEY,
    race_id        INTEGER NOT NULL,
    driver_id      INTEGER NOT NULL,
    constructor_id INTEGER NOT NULL,
    number         INTEGER,
    position       INTEGER NOT NULL,
    q1             VARCHAR(20),
    q2             VARCHAR(20),
    q3             VARCHAR(20),

    CONSTRAINT fk_qualifying_race
        FOREIGN KEY (race_id) REFERENCES races (id),

    CONSTRAINT fk_qualifying_driver
        FOREIGN KEY (driver_id) REFERENCES drivers (id),

    CONSTRAINT fk_qualifying_constructor
        FOREIGN KEY (constructor_id) REFERENCES constructors (id),

    CONSTRAINT uk_qualifying_race_driver
        UNIQUE (race_id, driver_id)
);

CREATE INDEX idx_qualifying_driver_id ON qualifying (driver_id);
CREATE INDEX idx_qualifying_race_id ON qualifying (race_id);
CREATE INDEX idx_qualifying_position ON qualifying (position);

COMMENT ON TABLE qualifying IS 'Qualifying results. Data available since 1994.';
COMMENT ON COLUMN qualifying.q1 IS 'Q1 lap time. NULL if driver did not participate.';
COMMENT ON COLUMN qualifying.q2 IS 'Q2 lap time. NULL if eliminated in Q1 or format without Q2.';
COMMENT ON COLUMN qualifying.q3 IS 'Q3 lap time. NULL if eliminated earlier or format without Q3.';
