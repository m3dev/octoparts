--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: cache_group; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE cache_group (
    id integer NOT NULL,
    name text NOT NULL,
    owner text,
    description text,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    CONSTRAINT cache_group_name_check CHECK ((name <> ''::text))
);


--
-- Name: cache_group_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE cache_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cache_group_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE cache_group_id_seq OWNED BY cache_group.id;


--
-- Name: http_part_config; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE http_part_config (
    id integer NOT NULL,
    part_id text NOT NULL,
    owner text NOT NULL,
    uri_to_interpolate text NOT NULL,
    description text,
    deprecated_in_favour_of text,
    method text NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    cache_ttl integer DEFAULT 0,
    alert_mails_enabled boolean DEFAULT false NOT NULL,
    alert_absolute_threshold integer,
    alert_percent_threshold real,
    alert_interval integer DEFAULT 60 NOT NULL,
    alert_mail_recipients text,
    additional_valid_statuses text,
    CONSTRAINT alert_mail_recipients_not_blank CHECK (((alert_mails_enabled = false) OR ((alert_mail_recipients IS NOT NULL) AND (alert_mail_recipients <> ''::text)))),
    CONSTRAINT alert_thresholds_not_both_null CHECK (((alert_mails_enabled = false) OR ((alert_absolute_threshold IS NOT NULL) OR (alert_percent_threshold IS NOT NULL)))),
    CONSTRAINT http_part_config_alert_absolute_threshold_check CHECK (((alert_absolute_threshold IS NULL) OR (alert_absolute_threshold >= 0))),
    CONSTRAINT http_part_config_alert_interval_check CHECK ((alert_interval > 0)),
    CONSTRAINT http_part_config_alert_percent_threshold_check CHECK (((alert_percent_threshold IS NULL) OR ((alert_percent_threshold >= (0)::double precision) AND (alert_percent_threshold <= (100)::double precision)))),
    CONSTRAINT http_part_config_deprecated_in_favour_of_check CHECK ((deprecated_in_favour_of <> ''::text)),
    CONSTRAINT http_part_config_method_check CHECK ((method <> ''::text)),
    CONSTRAINT http_part_config_part_id_check CHECK ((part_id <> ''::text)),
    CONSTRAINT http_part_config_uri_to_interpolate_check CHECK ((uri_to_interpolate <> ''::text))
);


--
-- Name: COLUMN http_part_config.cache_ttl; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN http_part_config.cache_ttl IS 'in seconds';


--
-- Name: COLUMN http_part_config.alert_absolute_threshold; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN http_part_config.alert_absolute_threshold IS 'Number of errors or timeouts to trigger an alert mail';


--
-- Name: COLUMN http_part_config.alert_percent_threshold; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN http_part_config.alert_percent_threshold IS 'Ratio of errors or timeouts (as % of total requests) to trigger an alert mail';


--
-- Name: COLUMN http_part_config.alert_interval; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN http_part_config.alert_interval IS 'Timespan over which to count errors (now - X seconds)';


--
-- Name: COLUMN http_part_config.alert_mail_recipients; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN http_part_config.alert_mail_recipients IS 'Mail To address (can be a comma-separated list)';


--
-- Name: http_part_config_cache_group; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE http_part_config_cache_group (
    id integer NOT NULL,
    cache_group_id bigint NOT NULL,
    http_part_config_id bigint NOT NULL
);


--
-- Name: http_part_config_cache_group_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE http_part_config_cache_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: http_part_config_cache_group_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE http_part_config_cache_group_id_seq OWNED BY http_part_config_cache_group.id;


--
-- Name: http_part_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE http_part_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: http_part_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE http_part_config_id_seq OWNED BY http_part_config.id;


--
-- Name: hystrix_config; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE hystrix_config (
    id integer NOT NULL,
    http_part_config_id bigint NOT NULL,
    thread_pool_config_id bigint NOT NULL,
    command_key text NOT NULL,
    command_group_key text NOT NULL,
    timeout_in_ms bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    CONSTRAINT hystrix_config_command_group_key_check CHECK ((command_group_key <> ''::text)),
    CONSTRAINT hystrix_config_command_key_check CHECK ((command_key <> ''::text))
);


--
-- Name: hystrix_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE hystrix_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: hystrix_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE hystrix_config_id_seq OWNED BY hystrix_config.id;


--
-- Name: part_param; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE part_param (
    id integer NOT NULL,
    http_part_config_id bigint NOT NULL,
    required boolean DEFAULT false NOT NULL,
    param_type text NOT NULL,
    output_name text NOT NULL,
    input_name_override text,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    versioned boolean DEFAULT false NOT NULL,
    CONSTRAINT part_param_input_name_override_check CHECK ((input_name_override <> ''::text)),
    CONSTRAINT part_param_output_name_check CHECK ((output_name <> ''::text)),
    CONSTRAINT part_param_param_type_check CHECK ((param_type <> ''::text))
);


--
-- Name: part_param_cache_group; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE part_param_cache_group (
    id integer NOT NULL,
    cache_group_id bigint NOT NULL,
    part_param_id bigint NOT NULL
);


--
-- Name: part_param_cache_group_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE part_param_cache_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: part_param_cache_group_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE part_param_cache_group_id_seq OWNED BY part_param_cache_group.id;


--
-- Name: part_param_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE part_param_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: part_param_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE part_param_id_seq OWNED BY part_param.id;

--
-- Name: thread_pool_config; Type: TABLE; Schema: public; Owner: -; Tablespace:
--

CREATE TABLE thread_pool_config (
    id integer NOT NULL,
    thread_pool_key text NOT NULL,
    core_size integer NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    CONSTRAINT thread_pool_config_thread_pool_key_check CHECK ((thread_pool_key <> ''::text))
);


--
-- Name: thread_pool_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE thread_pool_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: thread_pool_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE thread_pool_config_id_seq OWNED BY thread_pool_config.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY cache_group ALTER COLUMN id SET DEFAULT nextval('cache_group_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY http_part_config ALTER COLUMN id SET DEFAULT nextval('http_part_config_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY http_part_config_cache_group ALTER COLUMN id SET DEFAULT nextval('http_part_config_cache_group_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY hystrix_config ALTER COLUMN id SET DEFAULT nextval('hystrix_config_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY part_param ALTER COLUMN id SET DEFAULT nextval('part_param_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY part_param_cache_group ALTER COLUMN id SET DEFAULT nextval('part_param_cache_group_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY thread_pool_config ALTER COLUMN id SET DEFAULT nextval('thread_pool_config_id_seq'::regclass);


--
-- Name: cache_group_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY cache_group
    ADD CONSTRAINT cache_group_name_key UNIQUE (name);


--
-- Name: cache_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY cache_group
    ADD CONSTRAINT cache_group_pkey PRIMARY KEY (id);


--
-- Name: http_part_config_cache_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY http_part_config_cache_group
    ADD CONSTRAINT http_part_config_cache_group_pkey PRIMARY KEY (id);


--
-- Name: http_part_config_part_id_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY http_part_config
    ADD CONSTRAINT http_part_config_part_id_key UNIQUE (part_id);


--
-- Name: http_part_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY http_part_config
    ADD CONSTRAINT http_part_config_pkey PRIMARY KEY (id);


--
-- Name: hystrix_config_command_key_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY hystrix_config
    ADD CONSTRAINT hystrix_config_command_key_key UNIQUE (command_key);


--
-- Name: hystrix_config_http_part_config_id_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY hystrix_config
    ADD CONSTRAINT hystrix_config_http_part_config_id_key UNIQUE (http_part_config_id);


--
-- Name: hystrix_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY hystrix_config
    ADD CONSTRAINT hystrix_config_pkey PRIMARY KEY (id);


--
-- Name: part_param_cache_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY part_param_cache_group
    ADD CONSTRAINT part_param_cache_group_pkey PRIMARY KEY (id);


--
-- Name: part_param_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY part_param
    ADD CONSTRAINT part_param_pkey PRIMARY KEY (id);

--
-- Name: thread_pool_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY thread_pool_config
    ADD CONSTRAINT thread_pool_config_pkey PRIMARY KEY (id);


--
-- Name: thread_pool_config_thread_pool_key_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace:
--

ALTER TABLE ONLY thread_pool_config
    ADD CONSTRAINT thread_pool_config_thread_pool_key_key UNIQUE (thread_pool_key);


--
-- Name: cache_config_http_part_config_idx; Type: INDEX; Schema: public; Owner: -; Tablespace:
--

CREATE UNIQUE INDEX cache_config_http_part_config_idx ON http_part_config_cache_group USING btree (cache_group_id, http_part_config_id);


--
-- Name: cache_config_part_param_idx; Type: INDEX; Schema: public; Owner: -; Tablespace:
--

CREATE UNIQUE INDEX cache_config_part_param_idx ON part_param_cache_group USING btree (cache_group_id, part_param_id);


--
-- Name: part_param_http_part_config_idx; Type: INDEX; Schema: public; Owner: -; Tablespace:
--

CREATE UNIQUE INDEX part_param_http_part_config_idx ON part_param USING btree (http_part_config_id, param_type, output_name);


--
-- Name: http_part_config_cache_group_cache_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY http_part_config_cache_group
    ADD CONSTRAINT http_part_config_cache_group_cache_group_id_fkey FOREIGN KEY (cache_group_id) REFERENCES cache_group(id) ON DELETE RESTRICT;


--
-- Name: http_part_config_cache_group_http_part_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY http_part_config_cache_group
    ADD CONSTRAINT http_part_config_cache_group_http_part_config_id_fkey FOREIGN KEY (http_part_config_id) REFERENCES http_part_config(id) ON DELETE CASCADE;


--
-- Name: hystrix_config_http_part_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY hystrix_config
    ADD CONSTRAINT hystrix_config_http_part_config_id_fkey FOREIGN KEY (http_part_config_id) REFERENCES http_part_config(id) ON DELETE CASCADE;


--
-- Name: hystrix_config_thread_pool_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY hystrix_config
    ADD CONSTRAINT hystrix_config_thread_pool_config_id_fkey FOREIGN KEY (thread_pool_config_id) REFERENCES thread_pool_config(id) ON DELETE RESTRICT;


--
-- Name: part_param_cache_group_cache_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY part_param_cache_group
    ADD CONSTRAINT part_param_cache_group_cache_group_id_fkey FOREIGN KEY (cache_group_id) REFERENCES cache_group(id) ON DELETE RESTRICT;


--
-- Name: part_param_cache_group_part_param_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY part_param_cache_group
    ADD CONSTRAINT part_param_cache_group_part_param_id_fkey FOREIGN KEY (part_param_id) REFERENCES part_param(id) ON DELETE CASCADE;


--
-- Name: part_param_http_part_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY part_param
    ADD CONSTRAINT part_param_http_part_config_id_fkey FOREIGN KEY (http_part_config_id) REFERENCES http_part_config(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

