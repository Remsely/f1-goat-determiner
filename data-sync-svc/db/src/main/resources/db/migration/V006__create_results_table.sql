CREATE TABLE results
(
    id                INTEGER PRIMARY KEY,
    race_id           INTEGER       NOT NULL,
    driver_id         INTEGER       NOT NULL,
    constructor_id    INTEGER       NOT NULL,
    number            INTEGER,
    grid              INTEGER       NOT NULL,
    position          INTEGER,
    position_text     VARCHAR(3)    NOT NULL,
    position_order    INTEGER       NOT NULL,
    points            DECIMAL(5, 2) NOT NULL,
    laps              INTEGER       NOT NULL,
    time              VARCHAR(20),
    milliseconds      BIGINT,
    fastest_lap       INTEGER,
    fastest_lap_rank  INTEGER,
    fastest_lap_time  VARCHAR(20),
    fastest_lap_speed DECIMAL(7, 3),
    status_id         INTEGER       NOT NULL,

    CONSTRAINT fk_results_race
        FOREIGN KEY (race_id) REFERENCES races (id),

    CONSTRAINT fk_results_driver
        FOREIGN KEY (driver_id) REFERENCES drivers (id),

    CONSTRAINT fk_results_constructor
        FOREIGN KEY (constructor_id) REFERENCES constructors (id),

    CONSTRAINT fk_results_status
        FOREIGN KEY (status_id) REFERENCES statuses (id),

    CONSTRAINT uk_results_race_driver
        UNIQUE (race_id, driver_id)
);

CREATE INDEX idx_results_driver_id ON results (driver_id);
CREATE INDEX idx_results_constructor_id ON results (constructor_id);
CREATE INDEX idx_results_race_id ON results (race_id);
CREATE INDEX idx_results_position ON results (position) WHERE position IS NOT NULL;
CREATE INDEX idx_results_points ON results (points) WHERE points > 0;

COMMENT ON TABLE results IS 'Race results';
COMMENT ON COLUMN results.grid IS 'Starting grid position (0 = pit lane start)';
COMMENT ON COLUMN results.position IS 'Finishing position. NULL means not classified (DNF/DSQ)';
COMMENT ON COLUMN results.position_text IS 'Position as text: number, R (retired), D (disqualified), W (withdrawn), N (not classified)';
COMMENT ON COLUMN results.position_order IS 'Numeric order for sorting (DNFs placed after classified finishers)';
COMMENT ON COLUMN results.milliseconds IS 'Finish time in milliseconds';
