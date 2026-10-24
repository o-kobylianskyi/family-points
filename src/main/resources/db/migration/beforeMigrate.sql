-- Repair only the historical V34 -> V35 migration boundary on pre-existing
-- databases where legacy constraint names differ or constraints were removed.
-- Keep V35 immutable: changing an already-applied Flyway migration breaks checksums.
-- The real, strict allowed-source CHECK is installed by V35 immediately afterward.
DO $$
BEGIN
    IF to_regclass('public.flyway_schema_history') IS NULL
       OR to_regclass('public.point_transactions') IS NULL
       OR to_regclass('public.group_point_transactions') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.flyway_schema_history WHERE version = '34' AND success
    ) OR EXISTS (
        SELECT 1 FROM public.flyway_schema_history WHERE version = '35' AND success
    ) THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.point_transactions'::regclass
          AND conname = 'point_transactions_source_type_check'
    ) THEN
        ALTER TABLE public.point_transactions
            ADD CONSTRAINT point_transactions_source_type_check
            CHECK (source_type IS NOT NULL) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.group_point_transactions'::regclass
          AND conname = 'group_point_transactions_source_type_check'
    ) THEN
        ALTER TABLE public.group_point_transactions
            ADD CONSTRAINT group_point_transactions_source_type_check
            CHECK (source_type IS NOT NULL) NOT VALID;
    END IF;
END
$$;
