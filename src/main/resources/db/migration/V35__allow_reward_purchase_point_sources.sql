-- Hotfix for approved reward requests: the Java ledger enum already emits
-- REWARD_PURCHASE, but the V1 baseline CHECK does not accept it.
-- Preserve all legacy accepted source types. Flyway applies this atomically.
ALTER TABLE public.point_transactions
    DROP CONSTRAINT point_transactions_source_type_check;

ALTER TABLE public.point_transactions
    ADD CONSTRAINT point_transactions_source_type_check
    CHECK (source_type IN (
        'MANUAL','TASK','TASK_MILESTONE','TASK_PENALTY','LEARNING',
        'ACHIEVEMENT','ACCESS_PURCHASE','REWARD_PURCHASE','POINT_EXCHANGE',
        'REFUND','PENALTY_REVERSAL','SYSTEM'
    ));

-- Group ledger has the same baseline CHECK. Keep it aligned with the enum,
-- while preserving its existing accepted source types.
ALTER TABLE public.group_point_transactions
    DROP CONSTRAINT group_point_transactions_source_type_check;

ALTER TABLE public.group_point_transactions
    ADD CONSTRAINT group_point_transactions_source_type_check
    CHECK (source_type IN (
        'MANUAL','TASK','TASK_MILESTONE','TASK_PENALTY','LEARNING',
        'ACHIEVEMENT','ACCESS_PURCHASE','REWARD_PURCHASE','POINT_EXCHANGE',
        'REFUND','PENALTY_REVERSAL','SYSTEM'
    ));
