# Phase 5b Plan: Demo Tamper Simulation

Date: 2026-08-25

## Summary

Add a demo-only tamper simulation endpoint so the auditable ledger mismatch path can be demonstrated without manual
database edits.

The feature should be treated as bounded fault injection for a portfolio demo. It is not a production administration
tool and must not expose arbitrary writes.

## Current State

- Phase 1 and Phase 2 backend work are implemented.
- The frontend auditability UI is implemented and can display verified, invalid, pending, and unavailable ledger states.
- `GET /ledger/verify` returns ledger status, mismatch diagnostics, and ledger head fields.
- `GET /operations/{operationId}/proof` returns proof material for one operation.
- `POST /demo/reset` already exists under the `demo` profile and clears operations before restoring seed registers.
- Invalid ledger states are currently created only through tests or manual database changes.

## Goal

Implement a safe demo/dev-only way to create a detectable ledger mismatch, then recover through the existing demo reset
flow.

The user should be able to:

1. Create normal operations.
2. Trigger a bounded tamper simulation.
3. See `GET /ledger/verify` return `INVALID`.
4. Inspect the mismatch in the Audit Trail UI.
5. Use `POST /demo/reset` to restore the seeded demo state.

## Scope

### Backend API

Update `src/main/resources/openapi/home-budget-api.yaml` first.

Add:

- `POST /demo/ledger/tamper`
- `TamperLedgerCommand`
- `TamperLedgerMode`
- `TamperLedgerResponse`

Suggested request:

```text
TamperLedgerCommand
operationId: optional int64
sequenceNumber: optional int64
mode: optional AMOUNT | PREVIOUS_HASH | PAYLOAD_HASH | OPERATION_HASH
```

Suggested response:

```text
TamperLedgerResponse
operationId: int64
sequenceNumber: int64
mode: AMOUNT | PREVIOUS_HASH | PAYLOAD_HASH | OPERATION_HASH
field: string
previousValue: string
newValue: string
verificationPath: /ledger/verify
resetPath: /demo/reset
message: string
```

Request rules:

- If `operationId` is provided, target that operation.
- Else if `sequenceNumber` is provided, target that sequence.
- Else choose a recent operation automatically.
- If `mode` is omitted, choose a predictable default. `PAYLOAD_HASH` is a good default because it produces a focused
  mismatch without changing visible balances.
- Reject unknown operations with the existing problem-detail style.
- Reject tampering when the ledger is empty.
- Reject ambiguous requests that provide both `operationId` and `sequenceNumber`, unless the implementation verifies
  that both identify the same operation.

### Backend Implementation

Reuse the existing demo shape:

- Put the controller under `pl.mm.homebudget.api.demo`.
- Keep it behind `@Profile("demo")`, matching `DemoController` and `DemoResetService`.
- Put orchestration in an application service such as `DemoLedgerTamperService`.
- Use repository/template access scoped to the `OPERATIONS` table only.
- Do not recompute hashes after changing the selected field.
- Keep changes transactional.

Tamper modes:

| mode | field to change | suggested change |
|------|-----------------|------------------|
| `AMOUNT` | `AMOUNT` | Add `0.01` to the stored amount. |
| `PREVIOUS_HASH` | `PREVIOUS_HASH` | Replace one character with a different lowercase hex digit. |
| `PAYLOAD_HASH` | `PAYLOAD_HASH` | Replace one character with a different lowercase hex digit. |
| `OPERATION_HASH` | `OPERATION_HASH` | Replace one character with a different lowercase hex digit. |

Do not allow arbitrary table names, column names, SQL fragments, hash values, or amount values from the request.

### Frontend

Add a small demo-only action after the backend endpoint exists.

Suggested behavior:

- Show `Simulate mismatch` inside the Audit Trail section only when the frontend is in hosted demo mode or when the
  endpoint availability is known.
- Ask for confirmation before calling the endpoint.
- After success, refresh ledger verification immediately.
- Show a short success message that includes affected sequence and field.
- Keep `Reset demo` nearby as the recovery action.
- Do not put the action in the normal recharge or transfer workflows.

### Tests

Add backend integration tests with `WebTestClient`:

- `POST /demo/ledger/tamper` is unavailable outside the `demo` profile.
- Empty ledger tamper request returns a clear client error.
- Default tamper mode corrupts a bounded field on a selected operation.
- `GET /ledger/verify` returns `INVALID` after tampering.
- Response includes operation ID, sequence number, mode, previous value, new value, `/ledger/verify`, and `/demo/reset`.
- `POST /demo/reset` clears the simulated mismatch.

Add frontend verification:

- `npm.cmd run build` from `frontend/`.
- If OpenAPI or backend code changes, run `.\mvnw.cmd clean verify` with JDK 21.

## Acceptance Criteria

- Tamper simulation is available only in demo/dev context.
- The endpoint can only modify a bounded ledger field on one operation.
- No arbitrary writes or caller-provided SQL-like field selection are possible.
- Verification reports `INVALID` after tampering.
- The Audit Trail UI can make the mismatch visible.
- `POST /demo/reset` restores the demo to a verified state.
- Tests cover enabled and disabled behavior.

## Copy Prompt

```text
Implement Phase 5b of the auditable ledger: demo tamper simulation.

Before editing, read:

- AGENTS.md
- docs/auditable-ledger-master-plan.md
- docs/auditable-ledger-phase-2-plan.md
- docs/auditability-ui-plan.md
- docs/auditable-ledger-phase-5b-demo-tamper-plan.md

Goal:

Add a demo-only `POST /demo/ledger/tamper` endpoint that deliberately corrupts one bounded ledger field on one operation,
then refresh the frontend Audit Trail flow so the mismatch can be inspected and restored with `POST /demo/reset`.

Constraints:

- Update `src/main/resources/openapi/home-budget-api.yaml` first.
- Keep generated DTOs/interfaces generated; do not edit generated sources.
- Reuse the current demo-profile style used by `DemoController` and `DemoResetService`.
- Expose this only under `@Profile("demo")` or an equally explicit demo/dev guard.
- Do not allow arbitrary SQL, arbitrary table writes, arbitrary column names, or caller-provided replacement values.
- Do not add `GET /ledger/head`.
- Do not implement browser-side hash recalculation in this phase.
- Prefer default tampering of `PAYLOAD_HASH` when no mode is provided.
- After tampering, `GET /ledger/verify` must return `INVALID`.
- `POST /demo/reset` must remain the recovery path.

Backend implementation:

1. Add OpenAPI path and schemas for `TamperLedgerCommand`, `TamperLedgerMode`, and `TamperLedgerResponse`.
2. Add a demo-profile controller method for `POST /demo/ledger/tamper`.
3. Add an application service that selects the target operation by operation ID, sequence number, or a recent default.
4. Modify exactly one bounded field without recomputing hashes.
5. Return the affected operation ID, sequence, mode, field, previous value, new value, verification path, reset path,
   and a short message.
6. Add WebTestClient integration tests for enabled, disabled, empty-ledger, invalid-after-tamper, and reset recovery.

Frontend implementation:

1. Add API/types for the tamper endpoint.
2. Add a guarded `Simulate mismatch` action inside the Audit Trail section.
3. Confirm before calling it.
4. Refresh ledger verification after success.
5. Show the affected sequence/field and keep Reset demo as the recovery action.

Verification:

- Run `npm.cmd run build` from `frontend/`.
- Run backend verification with JDK 21: `.\mvnw.cmd clean verify`.
```
