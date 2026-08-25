# Phase 5b Plan: Showcase Ledger Tamper Simulation

Date: 2026-08-25

## Summary

Add a production-showcase tamper simulation so the auditable ledger mismatch paths can be demonstrated without direct
database access. The feature performs one real, bounded change to `OPERATIONS`; it is fault injection, not an arbitrary
administration API.

The hosted demo has ephemeral storage and recovers through `POST /demo/reset`. Persistent deployments deliberately do
not get automatic recovery. The UI must warn: `Permanent unless repaired manually. New operations will be blocked.`

## Goal

The user can:

1. Create normal operations.
2. Choose one of four fixed corruption modes.
3. See `GET /ledger/verify` report the matching first mismatch.
4. Copy an exact tamper receipt containing the field, previous value, and new value.
5. Reset an ephemeral demo or repair persistent storage manually.

## Runtime Availability

- The endpoint is part of the normal OpenAPI contract.
- `app.features.ledger-tamper-simulation-enabled` controls whether its controller is exposed.
- The showcase-oriented default is enabled; deployments can disable it with
  `APP_LEDGER_TAMPER_SIMULATION_ENABLED=false`.
- `GET /capabilities` advertises tamper availability and an `ephemeralStorage` boolean.
- Derive `ephemeralStorage` from `spring.r2dbc.url`: URLs beginning with `r2dbc:h2:mem` are ephemeral; other URLs are
  persistent.
- Expose `POST /demo/reset` only when the custom storage condition recognizes that ephemeral prefix.
- The frontend derives reset availability from `ephemeralStorage`; do not advertise a duplicate reset boolean.
- The frontend uses those capabilities rather than deriving behavior from the backend hostname.

## API

Add:

- `GET /capabilities`
- `POST /ledger/tamper-simulations`
- `RuntimeCapabilitiesResponse`
- `TamperLedgerCommand`
- `TamperLedgerMode`
- `TamperLedgerResponse`

Request:

```text
TamperLedgerCommand
sequenceNumber: optional int64
mode: optional AMOUNT | PREVIOUS_HASH | PAYLOAD_HASH | OPERATION_HASH
```

Rules:

- Require a positive sequence number at the API-model boundary when one is supplied.
- Use the requested sequence when supplied; otherwise select the latest operation.
- Default to `PAYLOAD_HASH`.
- Return `404 Not Found` for an unknown target.
- Return `409 Conflict` for an empty or already-invalid ledger.
- Assume one active showcase user and modify exactly one field per request.

Response:

```text
TamperLedgerResponse
operationId: int64
sequenceNumber: int64
mode: TamperLedgerMode
field: string
previousValue: string
newValue: string
verificationPath: /ledger/verify
resetPath: optional /demo/reset
message: string
```

The `field`, `previousValue`, and `newValue` form a tamper receipt suitable for manual repair. Do not return executable
SQL and do not persist a server-side recovery command.

## Fixed Corruption Modes

| mode             | field            | fixed change                                  | reported outcome                                           |
|------------------|------------------|-----------------------------------------------|------------------------------------------------------------|
| `AMOUNT`         | `AMOUNT`         | Add `0.01`, or subtract at the decimal limit. | Payload mismatch; payload and operation proof checks fail. |
| `PREVIOUS_HASH`  | `PREVIOUS_HASH`  | Change the first lowercase hex digit.         | Previous-hash mismatch.                                    |
| `PAYLOAD_HASH`   | `PAYLOAD_HASH`   | Change the first lowercase hex digit.         | Payload-hash mismatch without changing business data.      |
| `OPERATION_HASH` | `OPERATION_HASH` | Change the first lowercase hex digit.         | Operation-hash mismatch and a broken next link.            |

Never accept table names, column names, SQL fragments, replacement hashes, or replacement amounts from the caller.

## Backend Behavior

- Verify that the ledger is non-empty and valid before applying the corruption.
- Use fixed update statements with an optimistic condition on the previous field value.
- Keep selection and update transactional.
- Return the exact change receipt without verifying a second time; the frontend follows the mutation with the normal
  `GET /ledger/verify` refresh.
- Reject recharge and transfer creation with `409 Conflict` while the ledger is invalid.
- Keep `POST /demo/reset` available only for inferred ephemeral storage, independent of profile name.
- Startup backfill must not repair an already initialized, corrupted persistent ledger.

## Frontend

The Audit Trail contains:

- A fixed corruption-mode selector with short outcome descriptions.
- A target selector for the latest operation or a specific historical ledger sequence.
- `PAYLOAD_HASH` selected by default.
- A confirmation before mutation.
- The persistent warning: `Permanent unless repaired manually. New operations will be blocked.`
- Ephemeral guidance that Reset Demo restores the ledger.
- Immediate verification refresh after success.
- A tamper receipt showing operation ID, sequence, mode, field, previous value, and new value.
- A `Copy repair details` action.
- Reset Demo next to the simulation controls when reset is available.

The control is disabled when the ledger is empty, invalid, unavailable, pending, or another mutation is running.

## Tests And Verification

Backend integration coverage:

- Capability responses for enabled ephemeral, enabled persistent, and disabled deployments.
- Endpoint unavailable when the feature flag is disabled.
- Empty ledger, unknown sequence, and non-positive sequence rejection.
- Explicit corruption of a historical operation while later operations remain in the chain.
- All four modes return the expected receipt and proof flags.
- Verification returns `INVALID` after corruption.
- Invalid ledgers reject new operations.
- Demo reset restores a verified empty ledger.
- Persistent responses provide manual-repair guidance without a reset path.

Verification commands:

- `npm.cmd run build` from `frontend/`.
- `./mvnw.cmd clean verify` with JDK 21.

## Acceptance Criteria

- The deployed showcase exposes selectable, bounded tamper simulation.
- Runtime availability and storage behavior come from the backend.
- Ephemeral storage is inferred from the configured R2DBC URL, and reset availability is derived from that boolean.
- Each successful response records the exact reversible database value change without providing automatic recovery.
- The selected mode produces its expected first mismatch.
- Only one simulation can be applied before repair or reset.
- Normal balance-changing operations are blocked while the ledger is invalid.
- Ephemeral reset restores the demo; persistent corruption survives until manual repair.
- No arbitrary writes or caller-provided replacement values are possible.

## Future Enhancement: Actuator Capabilities

Replace the custom `GET /capabilities` endpoint with Spring Boot Actuator `/actuator/info` and a custom
`InfoContributor` exposing the ephemeral-storage and tamper-simulation capabilities. Prefer `/actuator/info` over
`/actuator/env`: the latter is intended for environment inspection and creates unnecessary configuration-exposure risk
for a browser-facing showcase API.
