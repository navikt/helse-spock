CREATE TABLE oppgave_makstid_paminnelse
(
    id                        BIGSERIAL,
    oppgave_id                 BIGINT NOT NULL,
    fodselsnummer             VARCHAR(32)  NOT NULL,
    neste_paminnelsetidspunkt TIMESTAMP    NOT NULL,
    event_id                   VARCHAR(64)  NOT NULL,
    antall_ganger_paminnet    INT          NOT NULL DEFAULT 0,
    opprettet                 TIMESTAMPTZ  NOT NULL default (now() at time zone 'utc'),
    oppdatert                 TIMESTAMPTZ    NOT NULL default (now() at time zone 'utc'),

    PRIMARY KEY (id)
);
