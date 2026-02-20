CREATE TABLE circuits
(
    id       INTEGER PRIMARY KEY,
    ref      VARCHAR(50)  NOT NULL,
    name     VARCHAR(100) NOT NULL,
    locality VARCHAR(100),
    country  VARCHAR(100)
);

CREATE UNIQUE INDEX idx_circuits_ref ON circuits (ref);

COMMENT ON TABLE circuits IS 'Reference table for F1 racing circuits';
COMMENT ON COLUMN circuits.ref IS 'Unique circuit identifier (circuitId from API)';
COMMENT ON COLUMN circuits.locality IS 'City or locality where circuit is located';
