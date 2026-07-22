--liquibase formatted sql
--changeset Marek:ledger-startup-backfill-fixture
--comment Legacy operations inserted before operation schema and ledger migrations
INSERT INTO OPERATION (
    ID,
    TIMESTAMP,
    AMOUNT,
    OPERATIONS_FROM,
    OPERATIONS_TO
) VALUES
(
    101,
    TIMESTAMP '2026-06-01 10:15:30',
    2500.00,
    NULL,
    'Wallet'
),
(
    102,
    TIMESTAMP '2026-06-01 10:00:00',
    100.00,
    'Wallet',
    'Savings'
),
(
    103,
    TIMESTAMP '2026-06-01 10:00:00',
    50.00,
    NULL,
    'Savings'
);
