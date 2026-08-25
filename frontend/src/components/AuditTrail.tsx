import { useState } from "react";
import type {
  OperationResponse,
  TamperLedgerMode,
  TamperLedgerResponse
} from "../apiTypes";
import type { LedgerVerificationState } from "../types/ui";
import { formatTimestamp } from "../utils/formatters";

export type AuditTrailProps = {
  ledgerState: LedgerVerificationState;
  operations: OperationResponse[];
  tamperSimulationAvailable: boolean;
  ephemeralStorage: boolean | null;
  isTampering: boolean;
  isResetting: boolean;
  isBusy: boolean;
  tamperReceipt: TamperLedgerResponse | null;
  onVerify: () => void;
  onTamper: (mode: TamperLedgerMode, sequenceNumber?: number) => void;
  onReset: () => void;
};

const TAMPER_MODES: Record<
  TamperLedgerMode,
  { label: string; description: string; outcome: string }
> = {
  AMOUNT: {
    label: "Amount",
    description: "Changes the stored financial value by one cent.",
    outcome: "Reports a payload mismatch; payload and operation proof checks fail."
  },
  PREVIOUS_HASH: {
    label: "Previous hash",
    description: "Changes the stored link to the preceding ledger entry.",
    outcome: "Reports a previous-hash mismatch."
  },
  PAYLOAD_HASH: {
    label: "Payload hash",
    description: "Changes audit metadata without changing balances or visible operation data.",
    outcome: "Reports a payload-hash mismatch."
  },
  OPERATION_HASH: {
    label: "Operation hash",
    description: "Changes the stored seal for the selected ledger entry.",
    outcome: "Reports an operation-hash mismatch and breaks the next entry's link."
  }
};

export function AuditTrail({
  ledgerState,
  operations,
  tamperSimulationAvailable,
  ephemeralStorage,
  isTampering,
  isResetting,
  isBusy,
  tamperReceipt,
  onVerify,
  onTamper,
  onReset
}: Readonly<AuditTrailProps>) {
  const [tamperMode, setTamperMode] = useState<TamperLedgerMode>("PAYLOAD_HASH");
  const [targetSequenceNumber, setTargetSequenceNumber] = useState("");
  const [copyStatus, setCopyStatus] = useState<string | null>(null);
  const verification =
    ledgerState.kind === "verified" || ledgerState.kind === "invalid"
      ? ledgerState.data
      : null;
  const modeDetails = TAMPER_MODES[tamperMode];
  const hasOperations = (verification?.operationCount ?? 0) > 0;
  const operationsBySequence = [...operations].sort(
    (left, right) => right.sequenceNumber - left.sequenceNumber
  );
  const [latestOperation, ...historicalOperations] = operationsBySequence;
  const selectedHistoricalOperation = historicalOperations.find(
    (operation) => operation.sequenceNumber === Number(targetSequenceNumber)
  );
  const effectiveTargetSequenceNumber = selectedHistoricalOperation ? targetSequenceNumber : "";
  const selectedOperation = selectedHistoricalOperation ?? latestOperation;
  const canTamper =
    tamperSimulationAvailable && ledgerState.kind === "verified" && hasOperations && !isBusy;

  function confirmTamper() {
    const recoveryWarning =
      ephemeralStorage === true
        ? "Reset Demo restores the ledger afterward."
        : "Permanent unless repaired manually. New operations will be blocked.";
    const targetLabel = selectedOperation
      ? `ledger sequence #${selectedOperation.sequenceNumber}`
      : "the latest ledger operation";
    if (
      globalThis.confirm(
        `Simulate ${modeDetails.label.toLowerCase()} corruption at ${targetLabel}? ${recoveryWarning}`
      )
    ) {
      setCopyStatus(null);
      onTamper(tamperMode, selectedHistoricalOperation?.sequenceNumber);
    }
  }

  async function copyRepairDetails() {
    if (!tamperReceipt) {
      return;
    }

    try {
      await globalThis.navigator.clipboard.writeText(formatRepairDetails(tamperReceipt));
      setCopyStatus("Repair details copied.");
    } catch {
      setCopyStatus("Could not copy repair details.");
    }
  }

  return (
    <section className="card border-0 shadow-sm audit-trail" id="audit-trail">
      <div className="card-header bg-white d-flex flex-wrap justify-content-between align-items-center gap-3 py-3">
        <div>
          <h2 className="h5 mb-1">Audit Trail</h2>
          <div className="text-secondary small">Tamper-evident checks for operation history.</div>
        </div>
        <button
          className="btn btn-outline-primary btn-sm"
          type="button"
          onClick={onVerify}
          disabled={ledgerState.kind === "pending"}
        >
          {ledgerState.kind === "pending" ? "Verifying..." : "Verify ledger"}
        </button>
      </div>

      <div className="card-body" aria-live="polite">
        {ledgerState.kind === "pending" && <PendingVerification />}
        {ledgerState.kind === "unavailable" && <UnavailableVerification />}
        {verification && <VerificationDetails ledgerState={ledgerState} />}
        {tamperSimulationAvailable && (
          <div className="tamper-simulation border-top mt-4 pt-4">
            <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
              <div>
                <h3 className="h6 mb-1">Corruption simulation</h3>
                <p className="text-secondary small mb-0">
                  Deliberately change one fixed ledger field and inspect the reported mismatch.
                </p>
              </div>
              {ephemeralStorage === true && (
                <button
                  className="btn btn-outline-danger btn-sm"
                  type="button"
                  onClick={onReset}
                  disabled={isBusy}
                >
                  {isResetting ? "Resetting..." : "Reset Demo"}
                </button>
              )}
            </div>

            <div className="row g-3 align-items-end mt-1">
              <div className="col-md-6">
                <label className="form-label fw-semibold" htmlFor="tamper-mode">
                  Corruption mode
                </label>
                <select
                  className="form-select"
                  id="tamper-mode"
                  value={tamperMode}
                  onChange={(event) => setTamperMode(event.target.value as TamperLedgerMode)}
                  disabled={isBusy || ledgerState.kind === "invalid"}
                >
                  {Object.entries(TAMPER_MODES).map(([value, details]) => (
                    <option value={value} key={value}>
                      {details.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-md-6">
                <label className="form-label fw-semibold" htmlFor="tamper-target">
                  Target operation
                </label>
                <select
                  className="form-select"
                  id="tamper-target"
                  value={effectiveTargetSequenceNumber}
                  onChange={(event) => setTargetSequenceNumber(event.target.value)}
                  disabled={isBusy || ledgerState.kind === "invalid" || !hasOperations}
                >
                  <option value="">
                    {latestOperation
                      ? `Latest operation (#${latestOperation.sequenceNumber})`
                      : "Latest operation"}
                  </option>
                  {historicalOperations.map((operation) => (
                    <option value={operation.sequenceNumber} key={operation.id}>
                      Sequence #{operation.sequenceNumber} - {operation.operationType}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-12 d-grid d-sm-flex justify-content-sm-end">
                <button
                  className="btn btn-outline-danger"
                  type="button"
                  onClick={confirmTamper}
                  disabled={!canTamper}
                >
                  {isTampering ? "Simulating..." : "Simulate corruption"}
                </button>
              </div>
            </div>

            <div className="small mt-3">
              <div>{modeDetails.description}</div>
              <div className="text-secondary">{modeDetails.outcome}</div>
            </div>

            {ephemeralStorage === false && (
              <div className="text-danger-emphasis small fw-semibold mt-3">
                Permanent unless repaired manually. New operations will be blocked.
              </div>
            )}
            {!hasOperations && ledgerState.kind === "verified" && (
              <div className="text-secondary small mt-3">
                Create an operation before simulating corruption.
              </div>
            )}
          </div>
        )}

        {tamperReceipt && (
          <TamperReceipt
            receipt={tamperReceipt}
            copyStatus={copyStatus}
            onCopy={() => void copyRepairDetails()}
          />
        )}
      </div>
    </section>
  );
}

function TamperReceipt({
  receipt,
  copyStatus,
  onCopy
}: Readonly<{
  receipt: TamperLedgerResponse;
  copyStatus: string | null;
  onCopy: () => void;
}>) {
  return (
    <div className="tamper-receipt border rounded-3 p-3 mt-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
          <div className="fw-semibold">Tamper receipt</div>
          <div className="text-secondary small">
            Keep these exact values if the operation may need manual repair.
          </div>
        </div>
        <button className="btn btn-outline-secondary btn-sm" type="button" onClick={onCopy}>
          Copy repair details
        </button>
      </div>

      <dl className="row small mb-0">
        <dt className="col-sm-4">Operation</dt>
        <dd className="col-sm-8">
          ID {receipt.operationId}, sequence #{receipt.sequenceNumber}
        </dd>

        <dt className="col-sm-4">Mode</dt>
        <dd className="col-sm-8">{TAMPER_MODES[receipt.mode].label}</dd>

        <dt className="col-sm-4">Field</dt>
        <dd className="col-sm-8">
          <code>{receipt.field}</code>
        </dd>

        <dt className="col-sm-4">Previous value</dt>
        <dd className="col-sm-8">
          <code className="technical-value d-block">{receipt.previousValue}</code>
        </dd>

        <dt className="col-sm-4">New value</dt>
        <dd className="col-sm-8 mb-0">
          <code className="technical-value d-block">{receipt.newValue}</code>
        </dd>
      </dl>

      {copyStatus && <output className="small text-secondary d-block mt-3">{copyStatus}</output>}
    </div>
  );
}

function formatRepairDetails(receipt: TamperLedgerResponse): string {
  return [
    `Operation ID: ${receipt.operationId}`,
    `Sequence number: ${receipt.sequenceNumber}`,
    `Mode: ${receipt.mode}`,
    `Field: ${receipt.field}`,
    `Previous value: ${receipt.previousValue}`,
    `New value: ${receipt.newValue}`
  ].join("\n");
}

function PendingVerification() {
  return (
    <div className="d-flex align-items-center gap-3 text-secondary">
      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
      <span>Checking the complete operation chain.</span>
    </div>
  );
}

function UnavailableVerification() {
  return (
    <output className="alert alert-warning d-block mb-0">
      <span className="fw-semibold d-block">Verification unavailable</span>
      <span className="small d-block mt-1">
        The ledger endpoint did not return a result. Budget balances remain available; retry the
        check when the backend is reachable.
      </span>
    </output>
  );
}

function VerificationDetails({ ledgerState }: Readonly<{ ledgerState: LedgerVerificationState }>) {
  if (ledgerState.kind !== "verified" && ledgerState.kind !== "invalid") {
    return null;
  }

  const { data } = ledgerState;
  const isVerified = ledgerState.kind === "verified";
  let verificationMessage =
    "The operation history is not fully verified. Review the first mismatch below and run verification again after it is corrected.";
  if (isVerified) {
    verificationMessage =
      data.operationCount === 0
        ? "The empty ledger is ready for its first operation."
        : `All ${data.operationCount} operations passed the stored chain checks.`;
  }

  return (
    <>
      <output className={`alert d-block ${isVerified ? "alert-success" : "alert-danger"}`}>
        <span className="fw-semibold d-block">
          {isVerified ? "Ledger verified" : "Ledger mismatch detected"}
        </span>
        <span className="small d-block mt-1">{verificationMessage}</span>
      </output>

      <dl className="row audit-details mb-0">
        <dt className="col-sm-5">Verified through sequence</dt>
        <dd className="col-sm-7">#{data.verifiedThroughSequence}</dd>

        <dt className="col-sm-5">Operation count</dt>
        <dd className="col-sm-7">{data.operationCount}</dd>

        <dt className="col-sm-5">Latest sequence</dt>
        <dd className="col-sm-7">#{data.latestSequenceNumber}</dd>

        <dt className="col-sm-5">Checked</dt>
        <dd className="col-sm-7">{formatTimestamp(data.checkedAt)}</dd>

        <dt className="col-12">Ledger head hash</dt>
        <dd className="col-12 mb-0">
          <code className="technical-value d-block p-2 mt-1">{data.ledgerHeadHash}</code>
        </dd>
      </dl>

      {data.mismatch && (
        <div className="audit-mismatch border border-danger-subtle rounded-3 p-3 mt-3">
          <div className="fw-semibold text-danger-emphasis mb-2">First mismatch</div>
          <dl className="row small mb-0">
            <dt className="col-sm-5">Sequence</dt>
            <dd className="col-sm-7">#{data.mismatch.sequenceNumber}</dd>

            <dt className="col-sm-5">Operation ID</dt>
            <dd className="col-sm-7">{data.mismatch.operationId ?? "No stored operation"}</dd>

            <dt className="col-sm-5">Reason</dt>
            <dd className="col-sm-7 mb-0">{data.mismatch.reason}</dd>
          </dl>
        </div>
      )}
    </>
  );
}
