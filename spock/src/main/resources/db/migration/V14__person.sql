CREATE TABLE person
(
    fnr                       BIGINT PRIMARY KEY,
    aktor_id                  BIGINT    NOT NULL,
    siste_aktivitet           TIMESTAMP NOT NULL,
    neste_paminnelsetidspunkt TIMESTAMP
);

create index "idx_person_neste_paminnelsetidspunkt" on person (neste_paminnelsetidspunkt);
