import type { LedgerVerificationState } from "../types/ui";
import { formatTimestamp } from "../utils/formatters";

export type AuditTrailProps = {
  ledgerState: LedgerVerificationState;
  onVerify: () => void;
};

export function AuditTrail({ ledgerState, onVerify }: Readonly<AuditTrailProps>) {
  const verification =
    ledgerState.kind === "verified" || ledgerState.kind === "invalid"
      ? ledgerState.data
      : null;

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
      </div>
    </section>
  );
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
    <div className="alert alert-warning mb-0" role="status">
      <div className="fw-semibold">Verification unavailable</div>
      <div className="small mt-1">
        The ledger endpoint did not return a result. Budget balances remain available; retry the
        check when the backend is reachable.
      </div>
    </div>
  );
}

function VerificationDetails({ ledgerState }: Readonly<{ ledgerState: LedgerVerificationState }>) {
  if (ledgerState.kind !== "verified" && ledgerState.kind !== "invalid") {
    return null;
  }

  const { data } = ledgerState;
  const isVerified = ledgerState.kind === "verified";

  return (
    <>
      <div className={`alert ${isVerified ? "alert-success" : "alert-danger"}`} role="status">
        <div className="fw-semibold">
          {isVerified ? "Ledger verified" : "Ledger mismatch detected"}
        </div>
        <div className="small mt-1">
          {isVerified
            ? data.operationCount === 0
              ? "The empty ledger is ready for its first operation."
              : `All ${data.operationCount} operations passed the stored chain checks.`
            : "The operation history is not fully verified. Review the first mismatch below and run verification again after it is corrected."}
        </div>
      </div>

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
