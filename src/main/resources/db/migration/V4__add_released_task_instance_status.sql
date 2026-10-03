ALTER TABLE task_instances
    DROP CONSTRAINT task_instances_status_check;

ALTER TABLE task_instances
    ADD CONSTRAINT task_instances_status_check
    CHECK (
        status IN (
            'PENDING',
            'IN_PROGRESS',
            'PAUSED',
            'RELEASED',
            'COMPLETED',
            'MISSED',
            'EXCUSED',
            'CANCELLED'
        )
    );
