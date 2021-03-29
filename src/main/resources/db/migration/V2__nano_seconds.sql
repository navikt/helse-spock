ALTER TABLE paminnelse ADD COLUMN endringstidspunkt_nanos BIGINT NOT NULL DEFAULT 0;
ALTER TABLE paminnelse ALTER COLUMN endringstidspunkt_nanos DROP DEFAULT;
