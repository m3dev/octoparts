ALTER TABLE http_part_config ADD COLUMN http_pool_size INTEGER CHECK (http_pool_size > 0);
COMMENT ON COLUMN http_part_config.http_pool_size IS 'size of the HTTP connection pool (per octoparts instance)';
UPDATE http_part_config SET http_pool_size = 20;
ALTER TABLE http_part_config ALTER COLUMN http_pool_size SET NOT NULL;

ALTER TABLE http_part_config ADD COLUMN http_connection_timeout BIGINT CHECK (http_connection_timeout >= 0);
COMMENT ON COLUMN http_part_config.http_connection_timeout IS 'In milliseconds. 0 means none.';
UPDATE http_part_config SET http_connection_timeout = 1000;
ALTER TABLE http_part_config ALTER COLUMN http_connection_timeout SET NOT NULL;

ALTER TABLE http_part_config ADD COLUMN http_socket_timeout BIGINT CHECK (http_socket_timeout >= 0);
COMMENT ON COLUMN http_part_config.http_socket_timeout IS 'In milliseconds. 0 means none.';
UPDATE http_part_config SET http_socket_timeout = 5000;
ALTER TABLE http_part_config ALTER COLUMN http_socket_timeout SET NOT NULL;

ALTER TABLE http_part_config ADD COLUMN http_default_encoding TEXT;
COMMENT ON COLUMN http_part_config.http_default_encoding IS 'A valid Java charset name';
UPDATE http_part_config SET http_default_encoding = 'UTF-8';
ALTER TABLE http_part_config ALTER COLUMN http_default_encoding SET NOT NULL;

ALTER TABLE http_part_config ADD COLUMN http_proxy TEXT;
COMMENT ON COLUMN http_part_config.http_proxy IS 'A proxy specification. Format should be host[:port]';

ALTER TABLE hystrix_config RENAME timeout_in_ms TO timeout;
COMMENT ON COLUMN hystrix_config.timeout IS 'In milliseconds';