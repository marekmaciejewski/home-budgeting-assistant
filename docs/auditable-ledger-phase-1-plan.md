# Phase 1 Plan: Auditable Ledger Backend Foundation

Date: 2026-07-15

## Summary

Implement the first usable backend slice of the auditable ledger: schema fields, operation model changes, deterministic
SHA-256 hashing, startup backfill for existing operations, and automatic hash-chaining for newly created recharges and
transfers.

This phase will not add `/ledger/head`, `/ledger/verify`, operation proof endpoints, UI changes, or demo tamper
simulation yet.

## Key Changes

- Add Liquibase changelog `07` for `OPERATION` ledger columns:
  `OPERATION_TYPE`, `SEQUENCE_NUMBER`, `PREVIOUS_HASH`, `PAYLOAD_HASH`, `OPERATION_HASH`.
- Keep new columns nullable for migration safety, but add uniqueness for `SEQUENCE_NUMBER` and `OPERATION_HASH`.
- Add `OperationType` enum with `RECHARGE` and `TRANSFER`.
- Extend `Operation` entity with the new ledger fields.
- Update operation creation:
  - recharge sets `operationType=RECHARGE`;
  - transfer sets `operationType=TRANSFER`;
  - before saving the operation, assign next sequence, previous hash, payload hash, and operation hash.
- Add startup backfill:
  - if all existing operations have empty ledger fields, backfill all operations ordered by `TIMESTAMP ASC, ID ASC`;
  - infer type from existing source/target fields;
  - fail startup on mixed partial ledger state rather than guessing.
- Extend `OperationResponse` in OpenAPI with required:
  - `operationType`
  - `sequenceNumber`
  - `operationHash`
- Do not expose `previousHash` or `payloadHash` in normal operation responses yet.

## Hashing Rules

- Genesis hash:
  `0000000000000000000000000000000000000000000000000000000000000000`
- Payload canonical format, no trailing newline:

```text
ledgerVersion=1
operationType=RECHARGE
timestamp=2026-06-01T10:15:30Z
amount=2500.00
sourceRegisterId=<null>
targetRegisterId=Wallet
```

- Payload hash for that vector:
  `cf5abd73908d6285aa2d7b02c2f3058521b5163333332dafcf7828407629dfbd`
- Operation hash input, no trailing newline:

```text
chainVersion=home-budget-ledger-v1
sequenceNumber=1
previousHash=0000000000000000000000000000000000000000000000000000000000000000
payloadHash=cf5abd73908d6285aa2d7b02c2f3058521b5163333332dafcf7828407629dfbd
```

- Operation hash for that vector:
  `308732eb898941007e56c0881ad80d47c5a4cd22450392d0cc42855db577bbc7`
- Normalize timestamps with `Instant.toString()`.
- Normalize money with scale `2` and `toPlainString()`.
- Use UTF-8 and lowercase hexadecimal SHA-256.

## Test Plan

- Unit-test `LedgerHasher` with the exact recharge vector above.
- Unit-test transfer payload canonicalization, including `BigDecimal` normalization to two decimals.
- Integration-test first recharge:
  - sequence number is `1`;
  - previous hash is genesis internally;
  - response contains `RECHARGE`, sequence `1`, and the expected operation hash.
- Integration-test multiple operations:
  - sequences increment `1, 2, 3...`;
  - operation hashes are 64-character lowercase hex;
  - later writes remain compatible with current balance behavior.
- Backfill-test existing pre-ledger rows:
  - insert old-style operations with null ledger fields;
  - run backfill service;
  - assert deterministic sequence order by timestamp then ID.
- Backfill-test invalid mixed state:
  - one operation has ledger fields and another does not;
  - service fails fast.
- Update existing API/integration assertions for the new required operation response fields.
- Run final verification with JDK 21:
  `.\mvnw.cmd clean verify`.

## Assumptions

- Phase 1 starts chaining new operations immediately.
- Existing local operation history should be preserved by deterministic backfill.
- Concurrency hardening beyond unique constraints is deferred; this app is still demo/local scale.
- Ledger verification, proof APIs, frontend audit UI, browser-side verification, and demo tamper simulation are later
  phases.
