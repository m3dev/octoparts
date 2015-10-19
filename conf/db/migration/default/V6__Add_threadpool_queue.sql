ALTER TABLE thread_pool_config
ADD COLUMN queue_size INTEGER
  NOT NULL
  DEFAULT 256
  CHECK (queue_size >= -1);

COMMENT ON COLUMN thread_pool_config.queue_size IS 'size of the execution queue for a specific thread pool';