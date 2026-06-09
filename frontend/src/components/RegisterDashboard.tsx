import type { RegisterResponse } from "../apiTypes";
import { formatAmount } from "../utils/formatters";

export type RegisterDashboardProps = {
  registers: RegisterResponse[];
  isLoading: boolean;
};

export function RegisterDashboard({ registers, isLoading }: RegisterDashboardProps) {
  const maxBalance = Math.max(...registers.map((register) => register.balance), 1);

  return (
    <section className="card border-0 shadow-sm">
      <div className="card-header bg-white d-flex justify-content-between align-items-center py-3">
        <div>
          <h2 className="h5 mb-1">Register balances</h2>
          <div className="text-secondary small">Current active registers returned by the API.</div>
        </div>
        <span className="badge text-bg-light border">{registers.length} active</span>
      </div>
      <div className="card-body">
        {isLoading ? (
          <div className="vstack gap-3" aria-label="Loading register balances">
            {[0, 1, 2, 3].map((item) => (
              <div className="placeholder-card" key={item} />
            ))}
          </div>
        ) : registers.length > 0 ? (
          <div className="vstack gap-3">
            {registers.map((register) => {
              const percentage = Math.max((register.balance / maxBalance) * 100, 3);

              return (
                <article className="register-row border rounded-3 p-3" key={register.id}>
                  <div className="d-flex justify-content-between align-items-start gap-3 mb-2">
                    <div>
                      <h3 className="h6 mb-1">{register.id}</h3>
                      <div className="text-secondary small">Register ID</div>
                    </div>
                    <div className="fw-semibold text-nowrap">{formatAmount(register.balance)}</div>
                  </div>
                  <div className="progress balance-progress" aria-hidden="true">
                    <div className="progress-bar" style={{ width: `${percentage}%` }} />
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div className="empty-state text-center p-4">
            <div className="fw-semibold mb-1">No registers returned.</div>
            <div className="text-secondary">Reset the demo or refresh data.</div>
          </div>
        )}
      </div>
    </section>
  );
}
