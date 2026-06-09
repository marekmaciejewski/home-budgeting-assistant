import type { MouseEventHandler } from "react";

export type AppHeaderProps = {
  apiBaseUrl: string;
  isRenderBackend: boolean;
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
  isRefreshing,
  isResetting,
  isInitialLoading,
  isBusy,
  onRefresh,
  onReset
}: AppHeaderProps) {
  const backendLabel = isRenderBackend ? "Render demo backend" : "Local backend";

  return (
    <header className="bg-white border-bottom">
      <nav className="navbar navbar-expand-lg bg-white">
        <div className="container py-2">
          <a className="navbar-brand d-flex align-items-center gap-2" href={import.meta.env.BASE_URL}>
            <span className="brand-badge">HB</span>
            <span>
              <span className="fw-semibold d-block lh-sm">Home Budgeting Assistant</span>
              <span className="text-secondary small">portfolio demo</span>
            </span>
          </a>
          <a
            className="btn btn-outline-secondary btn-sm"
            href={`${apiBaseUrl}/swagger-ui/index.html`}
            target="_blank"
            rel="noreferrer"
          >
            Swagger UI
          </a>
        </div>
      </nav>

      <div className="container pb-4">
        <div className="row align-items-end g-4 py-3">
          <div className="col-lg-8">
            <span className="badge rounded-pill text-bg-primary-subtle text-primary-emphasis mb-3">
              {backendLabel}
            </span>
            <h1 className="display-5 fw-semibold mb-3">Budget registers, demo operations, clear state.</h1>
            <p className="lead text-secondary mb-0">
              Recharge household registers, transfer funds between them, and inspect the operation
              history backed by the existing Spring WebFlux API.
            </p>
          </div>

          <div className="col-lg-4">
            <div className="card border-0 bg-light">
              <div className="card-body">
                <div className="text-secondary small text-uppercase fw-semibold mb-1">API base</div>
                <div className="api-base small fw-semibold mb-3">{apiBaseUrl}</div>
                <div className="d-flex gap-2">
                  <button
                    className="btn btn-primary flex-fill"
                    type="button"
                    onClick={onRefresh}
                    disabled={isInitialLoading || isRefreshing || isBusy}
                  >
                    {isRefreshing ? "Refreshing..." : "Refresh"}
                  </button>
                  <button
                    className="btn btn-outline-danger flex-fill"
                    type="button"
                    onClick={onReset}
                    disabled={isInitialLoading || isBusy}
                  >
                    {isResetting ? "Resetting..." : "Reset demo"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
