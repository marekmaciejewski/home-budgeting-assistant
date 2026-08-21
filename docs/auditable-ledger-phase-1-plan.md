# Phase 1 Plan: Auditable Ledger Backend Foundation

Date: 2026-07-15

## Summary

Implement the first usable backend slice of the auditable ledger: schema fields, operation model changes, deterministic
SHA-256 hashing, startup backfill for existing operations, and automatic hash-chaining for newly created recharges and
transfers.

This phase will not add `/ledger/verify`, operation proof endpoints, UI changes, or demo tamper
simulation yet.

## Key Changes

- Add Liquibase `07*` changelogs for schema cleanup before ledger fields:
  - move raw constraint and primary-key rename changes into one formatted SQL changelog with explicit rollback;
  - rename tables to plural `REGISTERS` and `OPERATIONS`;
  - rename operation relationship columns to `SOURCE_REGISTER_ID` and `TARGET_REGISTER_ID`;
  - require `TARGET_REGISTER_ID`;
  - replace old relationship constraint names in the same structural YAML changelog.
- Add Liquibase `08*` changelogs for `OPERATIONS` ledger columns:
  `OPERATION_TYPE`, `SEQUENCE_NUMBER`, `PREVIOUS_HASH`, `PAYLOAD_HASH`, `OPERATION_HASH`.
- In the same structural changelog, add the columns first, then add not-null constraints for them with Liquibase
  `defaultNullValue` fallbacks.
- Default legacy rows to `TRANSFER`, then use a compact SQL changelog to switch rows with null source register to
  `RECHARGE`.
- Use the same compact SQL changelog to turn defaulted legacy `SEQUENCE_NUMBER` and `OPERATION_HASH` values into
  unique temporary placeholders, then add the source/type check constraint with explicit rollback.
- Add uniqueness for `SEQUENCE_NUMBER` and `OPERATION_HASH`.
- Use the OpenAPI-generated `OperationType` enum with `RECHARGE` and `TRANSFER`.
- Extend `Operation` entity with the new ledger fields.
- Update operation creation:
  - recharge sets `operationType=RECHARGE`;
  - transfer sets `operationType=TRANSFER`;
  - before saving the operation, assign next sequence, previous hash, payload hash, and operation hash.
- Add startup backfill:
  - after Liquibase, treat all non-positive sequence numbers as legacy placeholder rows and backfill all operations
    ordered by `TIMESTAMP ASC, ID ASC`;
  - use the stored `OPERATION_TYPE`;
  - fail startup on mixed legacy/ledger sequence state rather than guessing.
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
sourceRegisterId=null
targetRegisterId=Wallet
```

- Payload hash for that vector:
  `296ec6d6025acbdc12da09bf833e445d2e4028c461d11b85cc3dc341c77dbc04`
- Operation hash input, no trailing newline:

```text
chainVersion=home-budget-ledger-v1
sequenceNumber=1
previousHash=0000000000000000000000000000000000000000000000000000000000000000
payloadHash=296ec6d6025acbdc12da09bf833e445d2e4028c461d11b85cc3dc341c77dbc04
```

- Operation hash for that vector:
  `bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c`
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
  - insert typed operations with temporary negative sequence numbers and placeholder hashes;
  - run backfill service;
  - assert deterministic sequence order by timestamp then ID.
- Backfill-test invalid mixed state:
  - one operation has a legacy placeholder sequence and another has a ledger sequence;
  - service fails fast.
- Update existing API/integration assertions for the new required operation response fields.
- Run final verification with JDK 21:
  `.\mvnw.cmd clean verify`.

## Assumptions

- Phase 1 starts chaining new operations immediately.
- Existing local operation history should be preserved by deterministic startup backfill when the legacy rows match the
  old application shape: target register present, optional source register for transfers.
- Legacy rows get database-safe ledger defaults during migration; SQL makes sequence/hash placeholders unique, and
  startup backfill replaces them with the real deterministic chain.
- Migration must remain backward-compatible with pre-ledger local databases: add nullable columns first, populate safe
  defaults while adding not-null constraints, use temporary unique placeholders for legacy rows, and only then apply
  final constraints.
- Once these changelogs are released or may have been applied outside this branch, do not rewrite them; add a new fixing
  changelog instead.
- Concurrency hardening beyond unique constraints is deferred; this app is still demo/local scale.
- Ledger verification, proof APIs, frontend audit UI, browser-side verification, and demo tamper simulation are later
  phases.
