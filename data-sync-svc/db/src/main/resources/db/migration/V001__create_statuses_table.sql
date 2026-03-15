CREATE TABLE statuses
(
    id     INTEGER PRIMARY KEY,
    status VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX idx_statuses_status ON statuses (status);

COMMENT ON TABLE statuses IS 'Reference table for race finish statuses';
COMMENT ON COLUMN statuses.id IS 'Status ID from Ergast/Jolpica API';
COMMENT ON COLUMN statuses.status IS 'Status description: Finished, +1 Lap, Engine, Accident, etc.';
