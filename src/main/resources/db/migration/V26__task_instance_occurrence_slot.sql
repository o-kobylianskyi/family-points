-- Preserve all historical executions as the implicit 'default' occurrence.
ALTER TABLE task_instances
    ADD COLUMN slot_key VARCHAR(40) NOT NULL DEFAULT 'default';

ALTER TABLE task_instances
    DROP CONSTRAINT IF EXISTS uk_task_instance_definition_member_date;

-- Existing data may have a separate actual constraint name from early migrations;
-- remove the old definition/member/day uniqueness only when it exists.
DO $$
DECLARE old_name TEXT;
BEGIN
  FOR old_name IN
    SELECT c.conname
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = 'task_instances' AND c.contype = 'u'
      AND (
        SELECT array_agg(a.attname::TEXT ORDER BY a.attname)
        FROM unnest(c.conkey) AS k(attnum)
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
      ) = ARRAY['member_id','scheduled_date','task_definition_id']::TEXT[]
  LOOP
    EXECUTE format('ALTER TABLE task_instances DROP CONSTRAINT %I', old_name);
  END LOOP;
END $$;

ALTER TABLE task_instances
    ADD CONSTRAINT uk_task_instance_definition_member_date_slot
    UNIQUE (task_definition_id, member_id, scheduled_date, slot_key);
