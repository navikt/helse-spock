CREATE TYPE utbetaling_type AS ENUM (
    'UTBETALING',
    'ANNULLERING',
    'ETTERUTBETALING',
    'REVURDERING'
    );

CREATE TABLE paminnelse (
    id bigserial primary key,
    aktor_id character varying(32) NOT NULL,
    fnr character varying(32) NOT NULL,
    organisasjonsnummer character varying(32) NOT NULL,
    vedtaksperiode_id character varying(64) NOT NULL,
    skal_reberegnes boolean DEFAULT false NOT NULL,
    tilstand character varying(64) NOT NULL,
    opprettet timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endringstidspunkt timestamp without time zone NOT NULL,
    endringstidspunkt_nanos bigint NOT NULL,
    neste_paminnelsetidspunkt timestamp without time zone NOT NULL,
    antall_ganger_paminnet integer DEFAULT 0 NOT NULL,
    data json NOT NULL
);

CREATE INDEX index_neste_paminnelsetidspunkt ON paminnelse USING btree (neste_paminnelsetidspunkt);
CREATE UNIQUE INDEX index_vedtaksperiode_id ON paminnelse USING btree (vedtaksperiode_id);

CREATE TABLE person (
    fnr bigint primary key,
    aktor_id bigint NOT NULL,
    siste_aktivitet timestamp without time zone NOT NULL,
    neste_paminnelsetidspunkt timestamp without time zone
);
CREATE INDEX idx_person_neste_paminnelsetidspunkt ON person USING btree (neste_paminnelsetidspunkt);

CREATE TABLE utbetaling (
    id uuid primary key,
    aktor_id character varying(32) NOT NULL,
    fnr character varying(32) NOT NULL,
    orgnr character varying(32) NOT NULL,
    type utbetaling_type NOT NULL,
    status character varying(64) NOT NULL,
    endringstidspunkt timestamp without time zone NOT NULL,
    endringstidspunkt_nanos integer NOT NULL,
    neste_paminnelsetidspunkt timestamp without time zone,
    antall_ganger_paminnet integer DEFAULT 0 NOT NULL,
    data json NOT NULL,
    opprettet timestamp without time zone DEFAULT now() NOT NULL
);

CREATE INDEX idx_utbetaling_neste_paminnelsetidspunkt ON utbetaling USING btree (neste_paminnelsetidspunkt);
