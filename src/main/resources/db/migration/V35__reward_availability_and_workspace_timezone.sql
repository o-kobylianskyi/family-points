-- Existing families explicitly start at UTC until an admin selects their IANA timezone.
-- Legacy timestamp-without-timezone task deadlines/purchase quota history is unchanged.
ALTER TABLE families ADD COLUMN time_zone varchar(64) NOT NULL DEFAULT 'UTC';

-- Reward acquisition availability is distinct from DIRECT/REQUEST and durationMinutes.
ALTER TABLE reward_definitions
  ADD COLUMN availability_mode varchar(24) NOT NULL DEFAULT 'ALWAYS',
  ADD COLUMN availability_starts_at timestamptz,
  ADD COLUMN availability_ends_at timestamptz;
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_availability_mode
  CHECK (availability_mode IN ('ALWAYS','ONE_TIME','RECURRING_WEEKLY'));
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_one_time_range
  CHECK (availability_mode <> 'ONE_TIME' OR
         (availability_starts_at IS NOT NULL AND availability_ends_at IS NOT NULL
          AND availability_starts_at < availability_ends_at));

CREATE TABLE reward_weekly_windows (
  reward_definition_id bigint NOT NULL REFERENCES reward_definitions(id) ON DELETE CASCADE,
  position integer NOT NULL,
  window_id varchar(64) NOT NULL,
  day_of_week integer NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
  local_start_time time NOT NULL,
  local_end_time time NOT NULL,
  effective_from_date date,
  effective_to_date date,
  enabled boolean NOT NULL DEFAULT true,
  PRIMARY KEY (reward_definition_id, position),
  CONSTRAINT chk_reward_weekly_order CHECK (local_start_time < local_end_time),
  CONSTRAINT chk_reward_weekly_date_order CHECK
    (effective_from_date IS NULL OR effective_to_date IS NULL OR effective_from_date <= effective_to_date)
);
CREATE INDEX idx_reward_weekly_windows_reward ON reward_weekly_windows(reward_definition_id);
