CREATE TABLE constructors
(
    id          INTEGER PRIMARY KEY,
    ref         VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    nationality VARCHAR(50)
);

CREATE UNIQUE INDEX idx_constructors_ref ON constructors (ref);

COMMENT ON TABLE constructors IS 'Reference table for F1 constructors (teams)';
COMMENT ON COLUMN constructors.ref IS 'Unique constructor identifier (constructorId from API)';
