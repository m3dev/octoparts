ALTER TABLE http_part_config ADD COLUMN local_contents_enabled boolean DEFAULT false NOT NULL;
ALTER TABLE http_part_config ADD COLUMN local_contents text;