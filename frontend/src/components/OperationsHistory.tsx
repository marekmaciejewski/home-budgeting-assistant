import type { OperationResponse } from "../apiTypes";
import type { ReactNode } from "react";
import { formatAmount, formatTimestamp } from "../utils/formatters";
import { describeOperation, operationKind } from "../utils/operations";

export type OperationsHistoryProps = {
  operations: OperationResponse[];
  isLoading: boolean;
};

export function OperationsHistory({ operations, isLoading }: Readonly<OperationsHistoryProps>) {
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
                    <div>
                      <div className="fw-semibold">{describeOperation(operation)}</div>
                      <div className="text-secondary small">{formatTimestamp(operation.timestamp)}</div>
                    </div>
                    <div className="fw-semibold text-nowrap">{formatAmount(operation.amount)}</div>
                  </div>
                </div>
              </div>
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
