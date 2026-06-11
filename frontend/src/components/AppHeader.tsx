import type { MouseEventHandler } from "react";

export type AppHeaderProps = {
  apiBaseUrl: string;
  isRenderBackend: boolean;
  isEphemeralDemo: boolean;
  canResetDemo: boolean;
  isRefreshing: boolean;
  isResetting: boolean;
  isInitialLoading: boolean;
  isBusy: boolean;
  onRefresh: MouseEventHandler<HTMLButtonElement>;
  onReset: MouseEventHandler<HTMLButtonElement>;
};

export function AppHeader({
  apiBaseUrl,
  isRenderBackend,
  isEphemeralDemo,
  canResetDemo,
  isRefreshing,
  isResetting,
  isInitialLoading,
  isBusy,
  onRefresh,
  onReset
}: Readonly<AppHeaderProps>) {
  const backendLabel = isRenderBackend ? "Render demo backend" : "Local backend";
  const storageLabel = isEphemeralDemo ? "Ephemeral" : "Persistent";
  const storageBadgeClass = isEphemeralDemo ? "text-bg-warning" : "text-bg-success";
  const backendDescription = isEphemeralDemo
    ? "Hosted demo state is resettable and may restart after Render idle time."
    : "Local mode is treated as file-backed storage; reset is unavailable here.";

  return (
    <header className="bg-white border-bottom">
      <nav className="navbar navbar-expand-lg bg-white">
        <div className="container py-1">
          <a className="navbar-brand d-flex align-items-center gap-2" href={import.meta.env.BASE_URL}>
            <span className="brand-badge">HB</span>
            <span>
              <span className="fw-semibold d-block lh-sm">Home Budgeting Assistant</span>
              <span className="text-secondary small">portfolio demo</span>
            </span>
          </a>
        </div>
      </nav>

      <div className="container pb-3">
        <div className="row align-items-start g-3 py-2">
          <div className="col-lg-7">
            <h1 className="fs-2 fw-semibold mb-2">Track household money across budget registers.</h1>
            <p className="text-secondary mb-0">
              Registers behave like lightweight accounts: recharge a balance, move funds between
              household buckets, and review the resulting operation history.
            </p>
          </div>

          <div className="col-lg-5">
            <div className="card border-0 bg-light api-connection-card">
              <div className="card-body p-3">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                  <div className="d-flex flex-wrap align-items-center gap-2">
                    <div className="text-secondary small text-uppercase fw-semibold">API connection</div>
                    <div className="fw-semibold">{backendLabel}</div>
                  </div>
                  <span className={`badge rounded-pill ${storageBadgeClass}`}>{storageLabel}</span>
                </div>

                <div className="d-flex flex-wrap align-items-baseline gap-2 mb-1">
                  <div className="text-secondary small text-uppercase fw-semibold">Base URL</div>
                  <div className="api-base small fw-semibold">{apiBaseUrl}</div>
                </div>
                <p className="text-secondary small mb-2">{backendDescription}</p>

                <div className="d-flex flex-wrap gap-2">
                  <button
                    className="btn btn-primary api-action"
                    type="button"
                    onClick={onRefresh}
                    disabled={isInitialLoading || isRefreshing || isBusy}
                  >
                    {isRefreshing ? "Refreshing..." : "Refresh"}
                  </button>
                  {canResetDemo && (
                    <button
                      className="btn btn-outline-danger api-action"
                      type="button"
                      onClick={onReset}
                      disabled={isInitialLoading || isBusy}
                    >
                      {isResetting ? "Resetting..." : "Reset demo"}
                    </button>
                  )}
                  <a
                    className="btn btn-outline-secondary api-action"
                    href={`${apiBaseUrl}/swagger-ui/index.html`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Swagger UI
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
