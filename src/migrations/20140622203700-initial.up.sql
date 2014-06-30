CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
--;;
COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';
--;;
CREATE TABLE certnames (
    certname text NOT NULL,
    deactivated time with time zone
);
--;;
ALTER TABLE public.certnames OWNER TO ken;
--;;
ALTER TABLE ONLY certnames
    ADD CONSTRAINT certnames_pkey PRIMARY KEY (certname);
--;;
CREATE TABLE value_types (
    id bigint NOT NULL,
    type character varying(32)
);
--;;
ALTER TABLE ONLY value_types
    ADD CONSTRAINT value_types_pkey PRIMARY KEY (id);
--;;
ALTER TABLE value_types OWNER TO ken;
--;;
INSERT INTO value_types (id, type) values (0, 'string');
--;;
INSERT INTO value_types (id, type) values (1, 'number');
--;;
INSERT INTO value_types (id, type) values (2, 'boolean');
--;;
INSERT INTO value_types (id, type) values (3, 'null');
--;;
CREATE TABLE fact_paths (
    id bigint NOT NULL,
    value_type_id bigint,
    path text
);
--;;
ALTER TABLE public.fact_paths OWNER TO ken;
--;;
CREATE SEQUENCE fact_paths_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--;;
ALTER TABLE public.fact_paths_id_seq OWNER TO ken;
--;;
ALTER SEQUENCE fact_paths_id_seq OWNED BY fact_paths.id;
--;;
ALTER TABLE ONLY fact_paths ALTER COLUMN id SET DEFAULT nextval('fact_paths_id_seq'::regclass);
--;;
ALTER TABLE ONLY fact_paths
    ADD CONSTRAINT fact_paths_path_type_id_key UNIQUE (path, value_type_id);
--;;
ALTER TABLE ONLY fact_paths
    ADD CONSTRAINT fact_paths_pkey PRIMARY KEY (id);
--;;
CREATE INDEX fact_paths_path_text_idx ON fact_paths USING btree (path);
--;;
CREATE INDEX fki_fact_paths_type_id ON fact_paths USING btree (value_type_id);
--;;
CREATE INDEX trgm_idx ON fact_paths USING gist (path gist_trgm_ops);
--;;
ALTER TABLE ONLY fact_paths
    ADD CONSTRAINT fact_paths_value_type_id FOREIGN KEY (value_type_id) REFERENCES value_types(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
--;;
CREATE TABLE fact_values (
    id bigint NOT NULL,
    path_id bigint,
    value text,
    value_int bigint,
    value_float real,
    value_string text,
    value_boolean boolean
);
--;;
ALTER TABLE public.fact_values OWNER TO ken;
--;;
CREATE SEQUENCE fact_values_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--;;
ALTER TABLE public.fact_values_id_seq OWNER TO ken;
--;;
ALTER SEQUENCE fact_values_id_seq OWNED BY fact_values.id;
--;;
ALTER TABLE ONLY fact_values ALTER COLUMN id SET DEFAULT nextval('fact_values_id_seq'::regclass);
--;;
ALTER TABLE ONLY fact_values
    ADD CONSTRAINT fact_values_path_id_value_key UNIQUE (path_id, value);
--;;
ALTER TABLE ONLY fact_values
    ADD CONSTRAINT fact_values_pkey PRIMARY KEY (id);
--;;
ALTER TABLE fact_values
  ADD CONSTRAINT fact_values_path_id_fkey FOREIGN KEY (path_id)
      REFERENCES fact_paths (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT;
--;;
CREATE INDEX fact_values_path_id_idx ON fact_values USING btree (path_id);
--;;
CREATE INDEX fact_values_value_idx ON fact_values USING btree (value);
--;;
CREATE INDEX fact_values_value_trgm ON fact_values USING gin (value gin_trgm_ops);
--;;
CREATE TABLE factsets (
    id bigint NOT NULL,
    certname text,
    "timestamp" timestamp with time zone,
    environment_id bigint
);
--;;
ALTER TABLE public.factsets OWNER TO ken;
--;;
CREATE SEQUENCE factsets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
--;;
ALTER TABLE public.factsets_id_seq OWNER TO ken;
--;;
ALTER SEQUENCE factsets_id_seq OWNED BY factsets.id;
--;;
ALTER TABLE ONLY factsets ALTER COLUMN id SET DEFAULT nextval('factsets_id_seq'::regclass);
--;;
ALTER TABLE ONLY factsets
    ADD CONSTRAINT factsets_pkey PRIMARY KEY (id);
--;;
CREATE INDEX fki_factsets_certname_fk ON factsets USING btree (certname);
--;;
ALTER TABLE ONLY factsets
    ADD CONSTRAINT factsets_certname_fk FOREIGN KEY (certname) REFERENCES certnames(certname);
--;;
CREATE TABLE facts (
    factset_id bigint,
    fact_values_id bigint
);
--;;
ALTER TABLE public.facts OWNER TO ken;
--;;
ALTER TABLE ONLY facts
    ADD CONSTRAINT facts_factset_id_fact_values_id_key UNIQUE (factset_id, fact_values_id);
--;;
CREATE INDEX fki_fact_values_id_fk ON facts USING btree (fact_values_id);
--;;
ALTER TABLE ONLY facts
    ADD CONSTRAINT fact_values_id_fk FOREIGN KEY (fact_values_id) REFERENCES fact_values(id);
--;;
ALTER TABLE ONLY facts
    ADD CONSTRAINT facts_factset_id_fkey FOREIGN KEY (factset_id) REFERENCES factsets(id);
