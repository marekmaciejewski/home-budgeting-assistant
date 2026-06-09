import { formatAmount } from "../utils/formatters";

export type SummaryCardsProps = {
  totalBalance: number;
  registerCount: number;
  operationCount: number;
  isLoading: boolean;
};

export function SummaryCards({
  totalBalance,
  registerCount,
  operationCount,
  isLoading
}: SummaryCardsProps) {
  return (
    <section className="row g-3 mb-4" aria-label="Register summary">
      <div className="col-md-4">
        <article className="card metric-card h-100 border-0 shadow-sm">
          <div className="card-body">
            <div className="text-secondary small text-uppercase fw-semibold mb-2">Total balance</div>
            <div className="metric-value">{isLoading ? "..." : formatAmount(totalBalance)}</div>
            <div className="text-secondary">Across {registerCount} active registers</div>
          </div>
        </article>
      </div>
      <div className="col-md-4">
        <article className="card metric-card h-100 border-0 shadow-sm">
          <div className="card-body">
            <div className="text-secondary small text-uppercase fw-semibold mb-2">Operations</div>
            <div className="metric-value">{isLoading ? "..." : operationCount}</div>
            <div className="text-secondary">
              {operationCount === 1 ? "recorded movement" : "recorded movements"}
            </div>
          </div>
        </article>
      </div>
      <div className="col-md-4">
        <article className="card metric-card h-100 border-0 shadow-sm">
          <div className="card-body">
            <div className="text-secondary small text-uppercase fw-semibold mb-2">Demo state</div>
            <div className="metric-value">Ephemeral</div>
            <div className="text-secondary">Reset restores seeded balances.</div>
          </div>
        </article>
      </div>
    </section>
  );
}
