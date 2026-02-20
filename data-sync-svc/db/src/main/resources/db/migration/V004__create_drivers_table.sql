CREATE TABLE drivers
(
    id          INTEGER PRIMARY KEY,
    ref         VARCHAR(50) NOT NULL,
    number      INTEGER,
    code        VARCHAR(3),
    forename    VARCHAR(50) NOT NULL,
    surname     VARCHAR(50) NOT NULL,
    dob         DATE,
    nationality VARCHAR(50)
);

CREATE UNIQUE INDEX idx_drivers_ref ON drivers (ref);
CREATE INDEX idx_drivers_nationality ON drivers (nationality);

COMMENT ON TABLE drivers IS 'Reference table for F1 drivers';
COMMENT ON COLUMN drivers.ref IS 'Unique driver identifier (driverRef in CSV, driverId in API)';
COMMENT ON COLUMN drivers.number IS 'Permanent driver number (since 2014)';
COMMENT ON COLUMN drivers.code IS 'Three-letter driver code (since 2014)';
COMMENT ON COLUMN drivers.dob IS 'Date of birth';
