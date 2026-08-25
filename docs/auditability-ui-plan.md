# Auditability UI Plan

Date: 2026-07-14

## Goal

Make ledger auditability visible in the React frontend without making the app feel like a crypto dashboard.

The UI should communicate confidence, explain problems clearly, and keep everyday budgeting tasks fast.

## Product Language

Prefer:

- Ledger verified
- Audit trail
- Tamper-evident history
- Operation proof
- Ledger head

Avoid:

- Mining
- Wallet
- Token
- Crypto
- Blockchain unless explaining the inspiration in docs

The tone should feel like an accountant's seal: calm, precise, and useful.

## Main Surfaces

### Header Ledger Status

Add a compact ledger status near the existing API connection controls in `AppHeader`.

Examples:

```text
Ledger verified - #128 - a91f...23c0
Verification pending
Ledger mismatch detected
Verification unavailable
```

Behavior:

- Click or button action opens the Audit Trail view/panel.
- Show sequence number and short hash only when available.
- Keep the status small so it supports the dashboard rather than competing with balances.

Suggested states:

| state         | meaning                                      | UI treatment                    |
|---------------|----------------------------------------------|---------------------------------|
| `verified`    | Full chain verified.                         | Subtle success badge/check.     |
| `pending`     | Verification is loading or has not been run. | Neutral spinner/text.           |
| `invalid`     | Chain verification failed.                   | Clear warning/error badge.      |
| `unavailable` | Backend did not return ledger data.          | Neutral warning, no panic copy. |

### Operations History

Enhance `OperationsHistory` rows with audit status.

Default row:

```text
#42 Transfer Wallet -> Food expenses  1500.00  Verified
```

Compact technical metadata:

```text
Seq #42 - a91f...23c0
```

Expand row details to show proof data:

```text
Sequence:        42
Previous hash:   7b12...f91a
Payload hash:    f03e...aa44
Operation hash:  a91f...23c0
Chain position:  Valid
```

Only show long hashes after expansion. Listing rows should remain scannable.

### Audit Trail View

Add a dedicated Audit Trail panel or tab after the backend verification API exists.

Core content:

```text
Audit Trail
Verified through operation #128
Ledger head: a91f...23c0
Operations verified: 128 / 128
Last checked: 2026-07-14 14:32
```

Actions:

- Verify ledger
- Copy ledger head
- Export audit proof
- Show technical details
- Simulate corruption, when showcase tamper simulation is available

Mismatch content:

```text
Ledger mismatch detected at operation #37
Expected previous hash: 91af...7710
Stored previous hash:   b20d...88ca
```

When invalid, keep balances readable while making the integrity failure clear in the header and Audit Trail. Disable
balance-changing actions until repair or reset; do not add warning styling to every balance card.

### Showcase Tamper Simulation

If the backend advertises tamper simulation, surface it as a clearly labeled showcase tool inside the Audit Trail view.

Suggested control:

```text
Simulate corruption
```

Behavior:

- Show only when `GET /capabilities` indicates tamper simulation is available.
- Offer the four fixed modes: amount, previous hash, payload hash, and operation hash.
- Require a confirmation before running it.
- After the request succeeds, automatically run ledger verification again.
- Show a copyable receipt containing the affected operation, sequence, field, previous value, and new value.
- Keep `Reset demo` nearby as the recovery path.
- Do not show this action as part of normal recharge or transfer workflows.

Suggested success copy:

```text
Mismatch simulated at operation #37. Verification now reports the first broken link.
```

Persistent confirmation warning:

```text
Permanent unless repaired manually. New operations will be blocked.
```

## Browser-Side Verification

Browser-side verification is an optional later phase.

When implemented, the frontend should:

- fetch the ledger operations or proof material,
- rebuild canonical payloads,
- recompute SHA-256 hashes using the Web Crypto API,
- compare the browser result with backend-provided hashes.

This enables stronger copy:

```text
Verified in this browser
```

Do this after backend verification is stable. Otherwise, two moving targets will make debugging unpleasant.

## API Data Needed

Phase 2 currently exposes the ledger data through:

- `GET /ledger/verify`, which returns verification status, mismatch diagnostics, latest sequence number, and
  `ledgerHeadHash`.
- `GET /operations/{operationId}/proof`, which returns detailed proof material for one operation.

There is no separate `GET /ledger/head` endpoint. Use `GET /ledger/verify` whenever the UI needs ledger status or head
data.

Minimum list data on each operation:

- `operationType`
- `sequenceNumber`
- `operationHash`

Ledger status data:

- status,
- operation count,
- verified through sequence,
- current ledger head hash,
- first mismatch summary if invalid,
- checked timestamp.

Operation proof data:

- sequence number,
- previous hash,
- payload hash,
- operation hash,
- recalculated values or status,
- previous/next operation identifiers when useful.

## Current Frontend Fit

The existing frontend has natural integration points:

- `App.tsx` already fetches registers and operations together.
- `AppHeader` already shows backend/demo state and contains action controls.
- `OperationsHistory` already maps operation rows.
- `api.ts` has a small request helper that can add ledger calls cleanly.
- `apiTypes.ts` already re-exports generated OpenAPI schema types.

The first UI implementation can be additive and should avoid changing the recharge/transfer workflows.

## UX Guardrails

- Do not block normal reading of balances when verification is pending.
- Make invalid ledger state obvious but not theatrical.
- Keep hashes shortened by default, with full values available in proof details.
- Prefer clear text labels over crypto jargon.
- Use structural states rather than color alone.
- If ledger verification fails, show the first mismatch and a plain explanation.

## Implementation Order

1. Add API client calls for ledger verification and operation proof.
2. Load ledger verification data with initial dashboard data.
3. Refresh ledger verification after recharge, transfer, manual refresh, and demo reset.
4. Add header status from `LedgerVerificationResponse`.
5. Add operation row sequence/hash metadata from `OperationResponse`.
6. Add expandable proof details from `OperationProofResponse`.
7. Add an Audit Trail panel using `LedgerVerificationResponse` for status, head hash, and mismatch details.
8. Add capability-gated corruption controls with fixed mode and target-operation selectors, plus an exact repair receipt.
9. Add browser-side verification and copy/export actions in a later slice.

## Acceptance Criteria

The UI is ready when:

- The user can see whether the ledger is verified.
- The user can inspect proof details for a specific operation.
- The user can run verification from the UI.
- The user can target the latest or a historical operation when simulating a bounded mismatch.
- Ledger mismatch states are clear and actionable.
- The default budgeting workflow still feels simple.
