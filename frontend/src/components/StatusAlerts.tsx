export type StatusAlertsProps = {
  isInitialLoading: boolean;
  showColdStartHint: boolean;
  errorMessage: string | null;
  feedbackMessage: string | null;
  isRenderBackend: boolean;
};

export function StatusAlerts({
  isInitialLoading,
  showColdStartHint,
  errorMessage,
  feedbackMessage,
  isRenderBackend
}: StatusAlertsProps) {
  if (!isInitialLoading && !showColdStartHint && !errorMessage && !feedbackMessage) {
    return null;
  }

  return (
    <section className="d-grid gap-2 mb-4" aria-live="polite">
      {isInitialLoading && (
        <div className="alert alert-primary d-flex align-items-start gap-3 mb-0" role="status">
          <div className="spinner-border spinner-border-sm mt-1" aria-hidden="true" />
          <div>
            <div className="fw-semibold">Loading demo data.</div>
            <div>
              Fetching registers and operation history
              {isRenderBackend ? " from Render. First response can be slow after idle time." : "."}
            </div>
          </div>
        </div>
      )}

      {showColdStartHint && (
        <div className="alert alert-warning mb-0" role="status">
          <div className="fw-semibold">
            {isRenderBackend ? "Backend may be waking up." : "Backend has not responded yet."}
          </div>
          <div>
            {isRenderBackend
              ? "Render Free cold starts can take around a minute. This UI keeps waiting instead of timing out early."
              : "Check that the local backend is running on http://localhost:8080."}
          </div>
        </div>
      )}

      {errorMessage && (
        <div className="alert alert-danger mb-0" role="alert">
          <div className="fw-semibold">Request failed.</div>
          <div>{errorMessage}</div>
        </div>
      )}

      {feedbackMessage && !errorMessage && (
        <div className="alert alert-success mb-0" role="status">
          <div className="fw-semibold">Demo updated.</div>
          <div>{feedbackMessage}</div>
        </div>
      )}
    </section>
  );
}
