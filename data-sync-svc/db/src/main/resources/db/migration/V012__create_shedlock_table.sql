CREATE TABLE shedlock
(
    name       VARCHAR(64)              NOT NULL,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by  VARCHAR(255)             NOT NULL,

    CONSTRAINT pk_shedlock_name PRIMARY KEY (name)
);

CREATE INDEX idx_shedlock_lock_until ON shedlock (lock_until);

COMMENT ON TABLE shedlock IS 'Distributed locks for scheduled sync jobs';
