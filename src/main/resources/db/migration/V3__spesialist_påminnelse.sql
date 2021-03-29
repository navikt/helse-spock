CREATE TABLE spesialist_paminnelse
(
    id                        BIGSERIAL,
    referanse                 VARCHAR(64) NOT NULL,
    fodselsnummer             VARCHAR(32)  NOT NULL,
    endringstidspunkt         TIMESTAMP    NOT NULL,
    neste_paminnelsetidspunkt TIMESTAMP    NOT NULL,
    timeout                   BIGINT       NOT NULL,
    antall_ganger_paminnet    INT          NOT NULL DEFAULT 0,
    opprettet                 TIMESTAMPTZ  NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);
