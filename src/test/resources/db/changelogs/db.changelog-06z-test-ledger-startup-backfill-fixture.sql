--liquibase formatted sql
--changeset Marek:ledger-startup-backfill-fixture context:@it-ledger-backfill
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
    TIMESTAMP WITH TIME ZONE '2026-06-01 10:15:30+00:00',
    2500.00,
    NULL,
    'Wallet'
),
(
    102,
    TIMESTAMP WITH TIME ZONE '2026-06-01 10:00:00+00:00',
    100.00,
    'Wallet',
    'Savings'
),
(
    103,
    TIMESTAMP WITH TIME ZONE '2026-06-01 10:00:00+00:00',
    50.00,
    NULL,
    'Savings'
);
