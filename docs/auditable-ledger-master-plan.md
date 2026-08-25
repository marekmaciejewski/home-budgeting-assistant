# Auditable Ledger Master Plan

Date: 2026-07-14

## Goal

Add blockchain-inspired auditability to the home budgeting assistant without turning the project into a distributed
blockchain system.

The useful property for this app is a tamper-evident operation history: once a recharge or transfer is recorded, later
changes to the amount, timestamp, source register, target register, or operation ordering should be detectable.

## Scope

This feature should start as an in-repo auditable ledger:

- Keep the existing Spring WebFlux application and R2DBC persistence.
- Keep registers and operations as the user-facing concepts.
- Add a hash chain over the operation history.
- Treat operation history as the source of truth for auditability.
- Keep `REGISTERS.BALANCE` as a read-optimized projection for now.
- Avoid proof-of-work, mining, tokens, peer nodes, and distributed consensus.

Use "auditable ledger" or "tamper-evident ledger" in product and code-facing language. Avoid presenting the feature as
a cryptocurrency-style blockchain.

## Codex Collaboration Note

The Codex app's `[Use plan mode]` button is a good fit for this feature. Use it at phase boundaries so the visible
checklist stays negotiated before implementation moves forward.

Recommended use:

- Enter Plan Mode before starting each phase.
- Confirm the phase objective and acceptance criteria.
- Keep implementation tasks visible while work is in progress.
- Leave Plan Mode when it is time to make the agreed code changes.

This keeps the work collaborative instead of making the implementation feel like a black box.

## Current System Fit

The app already has the right foundation:

- Operations are first-class records in `OPERATIONS`.
- Recharges and transfers are append-like business events.
- Register balances are changed through service methods rather than directly by controllers.
- The OpenAPI contract is the source of truth for external API shape.
- The frontend already has an operations history surface where audit status can be shown.

The main shift is conceptual: operations become ledger entries, and balances become a projection of that ledger.

## Data Model Plan

Extend operation records with ledger metadata:

| field | purpose |
|-------|---------|
| `OPERATION_TYPE` | Distinguishes `RECHARGE` from `TRANSFER`, and leaves room for later correction operations. |
| `SEQUENCE_NUMBER` | Monotonic ledger position. This is the chain order, independent from database ID semantics. |
| `PREVIOUS_HASH` | Hash of the previous ledger operation. The first operation uses a known genesis value. |
| `PAYLOAD_HASH` | Hash of the canonical operation payload. |
| `OPERATION_HASH` | Hash that links sequence number, previous hash, and payload hash. |

Recommended genesis hash:

```text
0000000000000000000000000000000000000000000000000000000000000000
```

Do not include the database-generated operation ID in the hash input. The sequence number is the ledger identity.

## Canonical Payload

Hashing must be deterministic. The same operation data must produce the same payload hash across JVMs and environments.

Canonical payload version 1 should include:

```text
ledgerVersion=1
operationType=RECHARGE|TRANSFER
timestamp=<Instant in UTC ISO-8601 form>
amount=<BigDecimal with scale 2 and plain string format>
sourceRegisterId=<value or null>
targetRegisterId=<value or null>
```

Rules:

- Normalize timestamps to `Instant`.
- Normalize money to scale 2 before hashing.
- Use the literal `null` string for missing source or target register IDs.
- Use UTF-8 bytes.
- Use lowercase hexadecimal SHA-256 output.
- Keep the canonical format versioned so future changes can be introduced deliberately.

Operation hash formula:

```text
operation_hash = SHA-256("home-budget-ledger-v1" + sequence_number + previous_hash + payload_hash)
```

The exact separator format should be explicit in code and covered by tests.

## Backend Implementation Phases

### Phase 1: Ledger Fields

- Update `src/main/resources/openapi/home-budget-api.yaml` first for response/API changes.
- Add Liquibase changelogs: first clean up operation/register table and relationship names, then add ledger columns to
  `OPERATIONS`.
- Use the OpenAPI-generated `OperationType` model.
- Extend the R2DBC `Operation` entity.
- Add generated DTO exposure for minimal ledger fields.

Suggested minimal operation response additions:

- `operationType`
- `sequenceNumber`
- `operationHash`

Keep full technical proof details behind a dedicated proof endpoint.

### Phase 2: Hashing

- Add a `LedgerHasher`.
- Add a canonical payload builder.
- Add unit tests with fixed timestamps and expected hashes.
- Keep this layer independent from Spring where practical.

### Phase 3: Append Flow

- Update recharge and transfer creation so ledger metadata is assigned inside the same transaction as balance changes.
- Determine the current ledger head.
- Assign `sequenceNumber = currentHead.sequenceNumber + 1`.
- Set `previousHash = currentHead.operationHash`, or the genesis hash when the ledger is empty.
- Compute payload and operation hashes.
- Persist the operation.

Concurrency needs deliberate handling. The simplest first implementation can serialize append operations in the service
or rely on a database uniqueness constraint plus retry. The important invariant is that two operations cannot receive
the same sequence number.

### Phase 4: Verification

Add a verification service that reads operations in ledger order and checks:

- sequence numbers are contiguous,
- the first previous hash is the genesis hash,
- each operation's previous hash matches the prior operation hash,
- each payload hash matches the recalculated payload hash,
- each operation hash matches the recalculated operation hash.

Verification result should include:

- overall status,
- operation count,
- verified through sequence,
- current ledger head hash,
- first mismatch sequence and operation ID when invalid,
- human-readable reason when invalid.

### Phase 5: API

Add ledger-focused endpoints:

| method | path | purpose |
|--------|------|---------|
| `GET` | `/ledger/verify` | Verify the full chain and return current ledger head diagnostics. |
| `GET` | `/operations/{operationId}/proof` | Return technical proof details for one operation. |

Potential response schemas:

- `LedgerVerificationResponse`
- `OperationProofResponse`
- `LedgerMismatch`

### Phase 5b: Demo Tamper Simulation

Add a profile-gated tamper simulation tool so the mismatch path can be demonstrated without manual database edits.

This should be treated as demo fault injection, not a production feature or administrative backdoor.

Recommended endpoint:

| method | path | purpose |
|--------|------|---------|
| `POST` | `/demo/ledger/tamper` | Deliberately corrupt one ledger field so verification fails. Demo/dev only. |

Recommended behavior:

- Expose only under the `demo` profile or an explicit local development property.
- Prefer changing one bounded field on one operation, such as `AMOUNT`, `PREVIOUS_HASH`, or `PAYLOAD_HASH`.
- Do not recompute hashes after the change. The point is to create a detectable mismatch.
- Return the affected operation ID, sequence number, tampered field, previous value, new value, and reset guidance.
- Do not allow arbitrary SQL, arbitrary table writes, or arbitrary field names.
- After tampering, `GET /ledger/verify` should report the first mismatch.
- `POST /demo/reset` should restore the seed state and clear the simulated mismatch.

Potential request schema:

```text
TamperLedgerCommand
operationId: optional long
sequenceNumber: optional long
mode: AMOUNT | PREVIOUS_HASH | PAYLOAD_HASH | OPERATION_HASH
```

If neither operation ID nor sequence number is provided, choose a recent operation with enough surrounding chain context
to produce a useful mismatch demonstration.

See [Phase 5b demo tamper plan](auditable-ledger-phase-5b-demo-tamper-plan.md) for implementation details and a
separate-session prompt.

### Phase 6: Tests

Add focused coverage for:

- deterministic canonical payloads,
- deterministic SHA-256 output,
- first operation using genesis hash,
- second operation linking to first operation,
- tampered amount detection,
- tampered source/target register detection,
- missing operation detection through sequence gaps,
- demo tamper simulation causing verification failure,
- demo reset clearing a simulated mismatch,
- API responses for valid and invalid ledgers.

Prefer structural JSON assertions for controller and integration tests.

## Migration Strategy

This is a demo/showcase app, but a local file-backed H2 database can still contain operation rows from earlier runs.

Recommended low-risk path:

1. Clean up operation/register schema names and target-register nullability.
2. Add ledger columns.
3. In the same structural migration, add not-null constraints with `defaultNullValue` fallbacks so legacy rows receive
   database-safe ledger values.
4. Convert defaulted legacy rows to type-correct and unique temporary placeholders before uniqueness and type/source
   constraints are applied.
5. Backfill the real deterministic chain on startup by `TIMESTAMP` then `ID`.
6. Start assigning sequence and hash fields for newly appended operations.

The ledger migration should be backward-compatible with pre-ledger local databases. Existing operation rows are not
discarded: nullable ledger columns are added first, Liquibase defaults make them non-null, compact SQL fixes the
defaulted values that must be type-correct or unique, and startup backfill replaces placeholders with deterministic
hash-chain values. After these changelogs are released, compatibility fixes should be appended as new changelogs rather
than by rewriting applied ones.

For the public demo profile, `POST /demo/reset` should clear operations and restore seed registers. After this feature,
reset should also leave the ledger empty and ready for a fresh genesis-linked first operation.

## Frontend Implementation Phases

Frontend work should follow the backend/API foundation:

1. Add generated API types for ledger responses.
2. Fetch ledger verification alongside registers and operations; use its ledger head fields for UI status.
3. Show a compact ledger status in the header.
4. Add verification badges to operation rows.
5. Add expandable operation proof details.
6. Add a dedicated Audit Trail view.
7. Optionally add browser-side hash verification after backend verification is stable.

See [auditability UI plan](auditability-ui-plan.md) for frontend details.

## Separate Repository Decision Point

A separate repository starts to make sense only if the project grows beyond a local tamper-evident ledger.

Create a separate `home-budget-ledger-node` style project if the goal becomes:

- multiple ledger nodes,
- signed client transactions,
- peer replication,
- consensus,
- a ledger explorer,
- independent storage outside the budgeting API.

Until then, keep this feature inside the current repo. The current app can gain most of the practical auditability
benefit without carrying distributed-system complexity.

## Acceptance Criteria

The first complete version is done when:

- New recharge and transfer operations are hash-chained.
- The ledger head and full-chain verification diagnostics can be retrieved through the API.
- Tampering with a historical operation causes verification to fail.
- A demo/dev-only tamper simulation can create a visible mismatch without manual database access.
- Operation history shows clear audit status in the UI.
- A user can inspect the proof for an individual operation.
- Tests cover valid and invalid chains.
- README and implementation notes describe the feature honestly as tamper-evident, not distributed.
