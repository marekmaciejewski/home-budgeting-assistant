--liquibase formatted sql
--changeset Marek:ledger-startup-initialized-fixture
--comment Initialized ledger inserted after ledger migrations to simulate an application restart
INSERT INTO OPERATIONS (
    ID,
    TIMESTAMP,
    AMOUNT,
    OPERATION_TYPE,
    SEQUENCE_NUMBER,
    PREVIOUS_HASH,
    PAYLOAD_HASH,
    OPERATION_HASH,
    SOURCE_REGISTER_ID,
    TARGET_REGISTER_ID
) VALUES
(
    201,
    TIMESTAMP WITH TIME ZONE '2026-06-01 10:15:30+00:00',
    2500.00,
    'RECHARGE',
    1,
    '0000000000000000000000000000000000000000000000000000000000000000',
    '296ec6d6025acbdc12da09bf833e445d2e4028c461d11b85cc3dc341c77dbc04',
    'bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c',
    NULL,
    'Wallet'
),
(
    202,
    TIMESTAMP WITH TIME ZONE '2026-06-01 10:00:00+00:00',
    100.00,
    'TRANSFER',
    2,
    'bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c',
    '817a5da85aa5ca3facb14b7799a7fa6d45a0a869aab9010038604791645bcb39',
    '4698920a2def2f9525311972bf908dc3c81107c63c4c5e0b2ebe32063090d23c',
    'Wallet',
    'Savings'
);
