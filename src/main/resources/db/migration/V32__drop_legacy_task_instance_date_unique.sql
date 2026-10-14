-- A pre-slot unique constraint prevents multiple occurrences of one task per day.
-- V26 introduced the slot-aware unique constraint; preserve it and all task data.
-- Fail safely if the legacy name was reused for a different constraint.
DO $$
DECLARE actual_definition TEXT;
BEGIN
  SELECT pg_get_constraintdef(c.oid) INTO actual_definition
  FROM pg_constraint c
  WHERE c.conrelid = 'task_instances'::regclass
    AND c.conname = 'uk_task_instance_definition_date'
    AND c.contype = 'u';

  IF actual_definition IS NOT NULL THEN
    IF replace(actual_definition, ' ', '')
       <> 'UNIQUE(task_definition_id,scheduled_date)' THEN
      RAISE EXCEPTION 'Unexpected legacy constraint definition: %', actual_definition;
    END IF;
    ALTER TABLE task_instances DROP CONSTRAINT uk_task_instance_definition_date;
  END IF;
END $$;

-- The newer constraint (task_definition_id, member_id, scheduled_date, slot_key)
-- remains unchanged and guards duplicate occurrences per member and slot.
