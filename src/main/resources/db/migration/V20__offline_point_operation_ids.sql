-- Exactly-once effects for retried offline point operations.
ALTER TABLE point_transactions
    ADD COLUMN client_operation_id UUID;
CREATE UNIQUE INDEX uq_point_transaction_client_operation
    ON point_transactions(client_operation_id)
    WHERE client_operation_id IS NOT NULL;
