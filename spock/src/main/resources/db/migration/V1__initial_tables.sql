CREATE TABLE paminnelse
(
    id                        BIGSERIAL,
    aktor_id                  VARCHAR(32)                 NOT NULL,
    fnr                       VARCHAR(32)                 NOT NULL,
    organisasjonsnummer       VARCHAR(32)                 NOT NULL,
    vedtaksperiode_id         VARCHAR(64)                 NOT NULL,
    tilstand                  VARCHAR(64)                 NOT NULL,
    timeout                   BIGINT                      NOT NULL,
    endringstidspunkt         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    neste_paminnelsetidspunkt TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    antall_ganger_paminnet    INT                         NOT NULL DEFAULT 0,
    data                      JSON                        NOT NULL,
    opprettet                 TIMESTAMP WITH TIME ZONE    NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);

create unique index "index_vedtaksperiode_id" on paminnelse using btree (vedtaksperiode_id);
create index "index_neste_paminnelsetidspunkt" on paminnelse using btree (neste_paminnelsetidspunkt);
