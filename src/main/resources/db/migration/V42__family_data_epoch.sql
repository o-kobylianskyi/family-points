-- BKL-002: monotonic workspace data generation, initially zero for existing families.
-- No reset or mutation is performed by this migration.
ALTER TABLE families ADD COLUMN data_epoch BIGINT NOT NULL DEFAULT 0
    CONSTRAINT families_data_epoch_nonnegative CHECK (data_epoch >= 0);

-- A reset transaction MUST increment data_epoch atomically with its deletions.
-- Offline writes must supply and validate the epoch before performing any mutation.
