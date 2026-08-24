# Phase 3 Session Prompt: Frontend Auditability UI

Date: 2026-08-24

## Purpose

Use this prompt to start a separate Codex task for Phase 3 of the auditable ledger work.

Phase 1 and Phase 2 are already implemented. The next slice should make the ledger visible in the React frontend without
adding backend tamper simulation or browser-side hash recalculation yet.

## Copy Prompt

```text
Implement Phase 3 of the auditable ledger: the frontend auditability UI.

Before editing, read:

- AGENTS.md
- docs/auditable-ledger-master-plan.md
- docs/auditable-ledger-phase-2-plan.md
- docs/auditability-ui-plan.md
- docs/auditable-ledger-phase-3-session-prompt.md

Current state:

- Phase 1 and Phase 2 are in place.
- The backend exposes read-only ledger APIs:
  - GET /ledger/verify
  - GET /operations/{operationId}/proof
- There is no GET /ledger/head endpoint. Use GET /ledger/verify for both verification status and ledger head fields.
- GET /ledger/verify returns LedgerVerificationResponse with status, operationCount, verifiedThroughSequence,
  latestSequenceNumber, ledgerHeadHash, checkedAt, and nullable mismatch.
- GET /operations/{operationId}/proof returns OperationProofResponse with stored hashes, expected hashes, canonical
  payload/input, validation booleans, and valid.
- OperationResponse already includes operationType, sequenceNumber, and operationHash for list rows.
- Frontend code lives in frontend/.
- Relevant frontend files:
  - frontend/src/api.ts
  - frontend/src/apiTypes.ts
  - frontend/src/App.tsx
  - frontend/src/components/AppHeader.tsx
  - frontend/src/components/OperationsHistory.tsx
  - frontend/src/components/StatusAlerts.tsx
  - frontend/src/styles.css

Goal:

Add a first useful frontend auditability slice:

1. Add generated ledger types to frontend/src/apiTypes.ts.
2. Add api.verifyLedger() and api.getOperationProof(operationId) to frontend/src/api.ts.
3. Load ledger verification data together with registers and operations.
4. Refresh ledger verification after manual refresh, recharge, transfer, and demo reset.
5. Show a compact ledger status in AppHeader:
   - pending/loading
   - verified
   - invalid
   - unavailable
6. Add operation row sequence number and shortened operation hash metadata in OperationsHistory.
7. Add expandable proof details for one operation at a time, fetched lazily from GET /operations/{operationId}/proof.
8. Add an Audit Trail panel or section that shows current verification status, verified-through sequence, operation
   count, latest sequence number, ledger head hash, checked timestamp, and first mismatch details when invalid.

Scope limits:

- Do not add GET /ledger/head.
- Do not implement backend tamper simulation in this phase.
- Do not implement browser-side hash recalculation in this phase.
- Do not change recharge or transfer behavior except to refresh ledger status after successful mutations.
- Keep the UI subtle and utility-focused. Avoid crypto language in visible copy.
- Keep hashes shortened by default, with full hashes visible in proof details or technical detail areas.
- Keep invalid ledger state obvious, but do not hide balances.

Verification:

- Run npm.cmd run build from frontend/.
- If OpenAPI or backend code is changed unexpectedly, also run the backend checks with JDK 21 and .\mvnw.cmd clean verify.

Acceptance criteria:

- The user can see whether the ledger is verified.
- The user can inspect proof details for a specific operation.
- The user can manually refresh verification from the UI.
- Ledger mismatch states are clear and actionable.
- The normal budgeting workflow remains simple.
```

## Notes For The Implementing Task

- Prefer adding a small frontend state type for loading/verified/invalid/unavailable rather than leaking raw transport
  errors into presentation components.
- Use `latestSequenceNumber` and `ledgerHeadHash` from `LedgerVerificationResponse` for the header summary.
- Use `verifiedThroughSequence`, `operationCount`, and `mismatch` for the Audit Trail details.
- If proof loading fails for one operation, show the row-level error without replacing the whole dashboard error state.
- Keep the first implementation additive. The existing dashboard layout should remain recognizable.
