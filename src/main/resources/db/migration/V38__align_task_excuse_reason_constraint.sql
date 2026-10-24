-- Align historical Hibernate-generated CHECK with TaskExcuseReason enum.
-- Older populated databases omitted EXCEPTION_PERIOD in this constraint.
-- A new versioned migration preserves checksums for already-applied V1..V37.
ALTER TABLE public.task_instances
    DROP CONSTRAINT IF EXISTS task_instances_excuse_reason_check;

ALTER TABLE public.task_instances
    ADD CONSTRAINT task_instances_excuse_reason_check
    CHECK (excuse_reason IN (
        'ILLNESS', 'FAMILY_EVENT', 'TRAVEL', 'PARENT_DECISION',
        'OTHER', 'EXCEPTION_PERIOD'
    ));
