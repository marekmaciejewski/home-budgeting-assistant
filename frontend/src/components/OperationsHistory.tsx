import { useEffect, useRef, useState, type ReactNode } from "react";
import { api } from "../api";
import type { OperationProofResponse, OperationResponse } from "../apiTypes";
import { formatAmount, formatShortHash, formatTimestamp } from "../utils/formatters";
import { describeOperation, operationKind } from "../utils/operations";

export type OperationsHistoryProps = {
  operations: OperationResponse[];
  isLoading: boolean;
};

type ProofState =
  | { operationKey: string; kind: "loading" }
  | { operationKey: string; kind: "loaded"; proof: OperationProofResponse }
  | { operationKey: string; kind: "error"; message: string };

export function OperationsHistory({ operations, isLoading }: Readonly<OperationsHistoryProps>) {
  const [expandedOperationKey, setExpandedOperationKey] = useState<string | null>(null);
  const [proofState, setProofState] = useState<ProofState | null>(null);
  const requestVersion = useRef(0);

  useEffect(
    () => () => {
      requestVersion.current += 1;
    },
    []
  );

  async function toggleProof(operation: OperationResponse) {
    const operationKey = `${operation.id}:${operation.operationHash}`;

    if (expandedOperationKey === operationKey) {
      requestVersion.current += 1;
      setExpandedOperationKey(null);
      setProofState(null);
      return;
    }

    const currentRequest = requestVersion.current + 1;
    requestVersion.current = currentRequest;
    setExpandedOperationKey(operationKey);
    setProofState({ operationKey, kind: "loading" });

    try {
      const proof = await api.getOperationProof(operation.id);

      if (requestVersion.current === currentRequest) {
        setProofState({ operationKey, kind: "loaded", proof });
      }
    } catch (error) {
      if (requestVersion.current === currentRequest) {
        const message = error instanceof Error ? error.message : "Operation proof could not be loaded.";
        setProofState({ operationKey, kind: "error", message });
      }
    }
  }

  let content: ReactNode;

  if (isLoading) {
    content = (
      <div className="vstack gap-3" aria-label="Loading operation history">
        {[0, 1, 2].map((item) => (
          <div className="placeholder-card placeholder-card-sm" key={item} />
        ))}
      </div>
    );
  } else if (operations.length > 0) {
    content = (
      <div className="list-group list-group-flush history-list">
        {operations.map((operation) => {
          const kind = operationKind(operation);
          const operationKey = `${operation.id}:${operation.operationHash}`;
          const isExpanded = expandedOperationKey === operationKey;
          const operationProofState =
            proofState?.operationKey === operationKey ? proofState : null;

          return (
            <article className="list-group-item px-0 history-item" key={operation.id}>
              <div className="d-flex align-items-start gap-3">
                <span
                  className={`operation-icon ${kind === "Recharge" ? "operation-icon-recharge" : "operation-icon-transfer"}`}
                  aria-hidden="true"
                >
                  {kind.slice(0, 1)}
                </span>
                <div className="flex-grow-1 min-w-0">
                  <div className="d-flex justify-content-between gap-3">
                    <div className="min-w-0">
                      <div className="fw-semibold">{describeOperation(operation)}</div>
                      <div className="text-secondary small">
                        {formatTimestamp(operation.timestamp)}
                      </div>
                    </div>
                    <div className="fw-semibold text-nowrap">{formatAmount(operation.amount)}</div>
                  </div>

                  <div className="d-flex flex-wrap align-items-center gap-2 mt-2 small">
                    <span className="text-secondary">Seq #{operation.sequenceNumber}</span>
                    <span className="text-secondary" aria-hidden="true">
                      &middot;
                    </span>
                    <code className="history-hash" title={operation.operationHash}>
                      {formatShortHash(operation.operationHash)}
                    </code>
                    {operationProofState?.kind === "loaded" && (
                      <span
                        className={`badge rounded-pill ${operationProofState.proof.valid ? "bg-success-subtle text-success-emphasis" : "bg-danger-subtle text-danger-emphasis"}`}
                      >
                        {operationProofState.proof.valid ? "Proof verified" : "Proof mismatch"}
                      </span>
                    )}
                    <button
                      className="btn btn-link btn-sm p-0 ms-sm-auto proof-toggle"
                      type="button"
                      aria-expanded={isExpanded}
                      aria-controls={`operation-proof-${operation.id}`}
                      onClick={() => void toggleProof(operation)}
                    >
                      {isExpanded ? "Hide proof" : "Inspect proof"}
                    </button>
                  </div>
                </div>
              </div>

              {isExpanded && (
                <OperationProofDetails
                  operationId={operation.id}
                  proofState={operationProofState}
                />
              )}
            </article>
          );
        })}
      </div>
    );
  } else {
    content = (
      <div className="empty-state text-center p-4">
        <div className="fw-semibold mb-1">No operations yet.</div>
        <div className="text-secondary">
          Use recharge or transfer to create the first movement in this demo session.
        </div>
      </div>
    );
  }

  return (
    <section className="card border-0 shadow-sm">
      <div className="card-header bg-white d-flex justify-content-between align-items-center py-3">
        <div>
          <h2 className="h5 mb-1">Operations history</h2>
          <div className="text-secondary small">Newest balance-changing operations first.</div>
        </div>
        <span className="badge text-bg-light border">{operations.length} total</span>
      </div>
      <div className="card-body">{content}</div>
    </section>
  );
}

function OperationProofDetails({
  operationId,
  proofState
}: Readonly<{ operationId: number; proofState: ProofState | null }>) {
  return (
    <div className="operation-proof mt-3 ms-sm-5" id={`operation-proof-${operationId}`}>
      {(!proofState || proofState.kind === "loading") && (
        <div className="d-flex align-items-center gap-2 text-secondary small p-3">
          <span className="spinner-border spinner-border-sm" aria-hidden="true" />
          <span>Loading operation proof...</span>
        </div>
      )}

      {proofState?.kind === "error" && (
        <div className="alert alert-warning small mb-0" role="alert">
          <div className="fw-semibold">Proof unavailable for this operation.</div>
          <div className="mt-1">{proofState.message}</div>
        </div>
      )}

      {proofState?.kind === "loaded" && <LoadedOperationProof proof={proofState.proof} />}
    </div>
  );
}

function LoadedOperationProof({ proof }: Readonly<{ proof: OperationProofResponse }>) {
  return (
    <div className={`border rounded-3 p-3 ${proof.valid ? "border-success-subtle" : "border-danger-subtle"}`}>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div className="fw-semibold">Operation proof</div>
        <span
          className={`badge rounded-pill ${proof.valid ? "bg-success-subtle text-success-emphasis" : "bg-danger-subtle text-danger-emphasis"}`}
        >
          {proof.valid ? "Valid" : "Mismatch detected"}
        </span>
      </div>

      <dl className="row small proof-details mb-3">
        <dt className="col-sm-4">Sequence</dt>
        <dd className="col-sm-8">#{proof.sequenceNumber}</dd>

        <dt className="col-sm-4">Previous hash</dt>
        <dd className="col-sm-8">
          <code className="technical-value">{proof.previousHash}</code>
        </dd>

        <dt className="col-sm-4">Payload hash</dt>
        <dd className="col-sm-8">
          <code className="technical-value">{proof.payloadHash}</code>
        </dd>

        <dt className="col-sm-4">Operation hash</dt>
        <dd className="col-sm-8">
          <code className="technical-value">{proof.operationHash}</code>
        </dd>
      </dl>

      <div className="d-flex flex-wrap gap-2 mb-3" aria-label="Operation proof checks">
        <ProofCheck label="Chain position" valid={proof.previousHashValid} />
        <ProofCheck label="Payload" valid={proof.payloadHashValid} />
        <ProofCheck label="Operation hash" valid={proof.operationHashValid} />
      </div>

      <details className="technical-details">
        <summary className="small fw-semibold">Technical calculation details</summary>
        <div className="mt-3">
          <HashComparison
            label="Expected previous hash"
            value={proof.expectedPreviousHash}
          />
          <HashComparison label="Expected payload hash" value={proof.expectedPayloadHash} />
          <HashComparison label="Expected operation hash" value={proof.expectedOperationHash} />

          <div className="small fw-semibold mt-3 mb-1">Canonical payload</div>
          <pre className="technical-block mb-3">{proof.canonicalPayload}</pre>

          <div className="small fw-semibold mb-1">Canonical operation hash input</div>
          <pre className="technical-block mb-0">
            {proof.canonicalOperationHashInput ?? "Unavailable because the previous operation is missing."}
          </pre>
        </div>
      </details>
    </div>
  );
}

function ProofCheck({ label, valid }: Readonly<{ label: string; valid: boolean }>) {
  return (
    <span
      className={`badge rounded-pill ${valid ? "bg-success-subtle text-success-emphasis" : "bg-danger-subtle text-danger-emphasis"}`}
    >
      {label}: {valid ? "valid" : "mismatch"}
    </span>
  );
}

function HashComparison({ label, value }: Readonly<{ label: string; value: string | null }>) {
  return (
    <div className="mb-2">
      <div className="small fw-semibold mb-1">{label}</div>
      <code className="technical-value d-block">{value ?? "Unavailable"}</code>
    </div>
  );
}
