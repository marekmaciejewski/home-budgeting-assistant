# Phase 2 Plan: Ledger Verification API And Operation Proof

Date: 2026-07-22

## Summary

Implement the next backend slice of the auditable ledger: read-only ledger verification, current ledger head reporting,
and per-operation proof details.

Phase 1 already added ledger fields, deterministic hashing, startup backfill, and append-time hash chaining. Phase 2
should expose and test that auditability through API endpoints, without adding frontend UI or demo tamper simulation yet.

## Key Changes

- Update `src/main/resources/openapi/home-budget-api.yaml` first.
- Add a `ledger` API tag and generated schemas for:
  - `LedgerStatus`
  - `LedgerVerificationResponse`
  - `LedgerMismatch`
  - `OperationProofResponse`
- Add read-only endpoints:
  - `GET /ledger/verify`
  - `GET /operations/{operationId}/proof`
- Add an `api.ledger` controller backed by a new verification service. The proof endpoint may live in the ledger
  controller even though its path is operation-scoped.
- Keep existing operation creation and response behavior unchanged.
- Do not add UI changes, browser-side verification, or `POST /demo/ledger/tamper` in this phase.

## API Behavior

### `GET /ledger/verify`

Return the current ledger head fields and full-chain verification diagnostics.

Fields:

- `status`: `VERIFIED` or `INVALID`
- `operationCount`: total number of operations
- `verifiedThroughSequence`: highest contiguous sequence verified before failure, or the latest sequence when verified
- `latestSequenceNumber`: latest stored sequence, or `0` when empty
- `ledgerHeadHash`: latest stored operation hash, or the genesis hash when empty
- `checkedAt`: current timestamp as `date-time`
- `mismatch`: nullable `LedgerMismatch`

This endpoint is the backend source of truth for later UI status and also provides the current ledger head. Do not add a
separate ledger head endpoint unless it gets materially different behavior.

Verification checks, in this order:

1. sequence starts at `1`;
2. sequence numbers are contiguous;
3. first previous hash equals `LedgerHasher.GENESIS_HASH`;
4. each later `previousHash` equals the previous operation's `operationHash`;
5. recalculated payload hash equals stored `payloadHash`;
6. recalculated operation hash equals stored `operationHash`.

Stop at the first mismatch and return `status=INVALID`.

Empty ledger behavior:

- `status=VERIFIED`
- `operationCount=0`
- `verifiedThroughSequence=0`
- `latestSequenceNumber=0`
- `ledgerHeadHash=LedgerHasher.GENESIS_HASH`
- `mismatch=null`

### `GET /operations/{operationId}/proof`

Return the proof material for one operation.

Fields:

- `operationId`
- `operationType`
- `timestamp`
- `amount`
- `sourceRegisterId`
- `targetRegisterId`
- `sequenceNumber`
- `previousHash`
- `payloadHash`
- `operationHash`
- `canonicalPayload`
- `canonicalOperationHashInput`
- `expectedPreviousHash`
- `expectedPayloadHash`
- `expectedOperationHash`
- `previousHashValid`
- `payloadHashValid`
- `operationHashValid`
- `valid`

Rules:

- Return `404` using the existing operation-not-found behavior when the operation ID is unknown.
- For sequence `1`, `expectedPreviousHash` is the genesis hash.
- For later sequences, `expectedPreviousHash` is the operation hash of `sequenceNumber - 1`; if the previous operation is
  missing, set `expectedPreviousHash=null`, `canonicalOperationHashInput=null`, `expectedOperationHash=null`,
  `previousHashValid=false`, `operationHashValid=false`, and `valid=false`.
- `valid` is true only when all three validation booleans are true.

## Implementation Notes

- Add repository accessors needed for verification, such as lookup by sequence number and ordered full-chain reads.
- Keep verification read-only and side-effect free.
- Derive verification status, operation count, latest sequence, and ledger head from one ordered operation scan so the
  returned diagnostics describe one database query result. After the first mismatch, preserve it and continue observing
  rows only to complete the ledger summary.
- Use `LedgerHasher` for all recalculation; do not duplicate canonicalization logic.
- Use the existing `Clock` bean for `checkedAt`.
- Keep the ledger scan accumulator immutable and separate from the verifier. Store the generated `LedgerMismatch` as
  the first detected failure and construct `LedgerVerificationResponse` directly from the completed scan; do not add
  equivalent handwritten result or mismatch types.
- Construct generated ledger verification and operation proof responses in one application-level
  `LedgerResponseFactory`. Keep hashing and validity calculation out of Spring model/bean configuration.
- Keep `Operation.sequenceNumber` nullable while an operation is transient and not yet assigned to the ledger. Treat
  sequence numbers loaded for verification as primitive `long` values because the persisted column is non-null.
- Keep endpoint response DTOs generated from OpenAPI; do not add handwritten API DTOs.
- Do not change Liquibase changelogs in this phase unless a test exposes a real schema bug. If a schema fix is needed,
  add a new changelog rather than rewriting released `07*` or `08*` changelogs.

## Test Plan

- Integration-test the verification service against the real R2DBC repository and H2 schema for:
  - empty ledger returns verified genesis state;
  - valid multi-operation chain verifies through the latest sequence;
  - sequence gap reports first gap;
  - previous-hash mismatch reports first mismatch;
  - payload tampering reports first payload mismatch;
  - operation-hash tampering reports first operation-hash mismatch.
- Integration-test API responses with `WebTestClient`:
  - `GET /ledger/verify` on empty seeded state;
  - create recharge and transfer, then `GET /ledger/verify`;
  - direct test-only database tamper, then `GET /ledger/verify` returns `INVALID`;
  - `GET /operations/{operationId}/proof` returns full proof for a valid operation;
  - proof calculation treats a missing previous operation as an invalid proof;
  - proof endpoint returns `404` for unknown operation ID.
- Update OpenAPI-generated usage and existing assertions only where required by new generated interfaces.
- Run final backend verification with JDK 21:
  `.\mvnw.cmd clean verify`.
- Optionally run frontend type/build verification after OpenAPI changes:
  `npm.cmd run build` from `frontend/`.

## Assumptions

- Phase 2 is backend/API only.
- Demo tamper simulation remains a later phase; tests may use direct test-only database updates to simulate mismatch.
- Frontend audit status, browser-side verification, and Audit Trail UI remain later phases.
- The ledger status vocabulary for this phase is only `VERIFIED` and `INVALID`; loading/unavailable states belong to the
  frontend later.
