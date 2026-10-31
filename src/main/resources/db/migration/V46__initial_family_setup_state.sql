-- Issue #18: additive durable wizard state, no destructive writes and no legacy-family backfill.
CREATE TABLE workspace_setup_states (
    workspace_id BIGINT PRIMARY KEY REFERENCES families(id) ON DELETE RESTRICT,
    data_epoch BIGINT NOT NULL DEFAULT 0 CHECK (data_epoch >= 0),
    revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
      CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    steps_json TEXT NOT NULL DEFAULT '{}',
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Created only for families inserted AFTER this migration. Existing families never get
-- a synthetic wizard row that might overwrite their established customizations.
CREATE OR REPLACE FUNCTION initialize_new_family_setup() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO workspace_setup_states(workspace_id, data_epoch)
  VALUES (NEW.id, NEW.data_epoch);
  RETURN NEW;
END;
$$;
CREATE TRIGGER family_setup_on_insert AFTER INSERT ON families
FOR EACH ROW EXECUTE FUNCTION initialize_new_family_setup();
