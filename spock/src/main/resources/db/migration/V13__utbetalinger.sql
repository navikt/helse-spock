CREATE TYPE utbetaling_type AS ENUM ('UTBETALING', 'ANNULLERING', 'ETTERUTBETALING');
CREATE TABLE utbetaling
(
    id                        UUID PRIMARY KEY,
    aktor_id                  VARCHAR(32)                 NOT NULL,
    fnr                       VARCHAR(32)                 NOT NULL,
    orgnr                     VARCHAR(32)                 NOT NULL,
    type                      utbetaling_type             NOT NULL,
    status                    VARCHAR(64)                 NOT NULL,
    endringstidspunkt         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    endringstidspunkt_nanos   INT                         NOT NULL,
    neste_paminnelsetidspunkt TIMESTAMP WITHOUT TIME ZONE,
    antall_ganger_paminnet    INT                         NOT NULL DEFAULT 0,
    data                      JSON                        NOT NULL,
    opprettet                 TIMESTAMP WITHOUT TIME ZONE NOT NULL default (now())
);

create index "idx_utbetaling_neste_paminnelsetidspunkt" on utbetaling using btree (neste_paminnelsetidspunkt);
