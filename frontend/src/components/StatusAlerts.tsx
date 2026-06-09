import { useEffect, useRef, useState } from "react";

export type StatusAlertsProps = {
  isInitialLoading: boolean;
  showColdStartHint: boolean;
  errorMessage: string | null;
  feedbackMessage: string | null;
  isRenderBackend: boolean;
};

type StatusPrompt = {
  key: string;
  role: "alert" | "status";
  variant: "danger" | "primary" | "success" | "warning";
  title: string;
  message: string;
  autoDismissMs?: number;
  showSpinner?: boolean;
};

export function StatusAlerts({
  isInitialLoading,
  showColdStartHint,
  errorMessage,
  feedbackMessage,
  isRenderBackend
}: StatusAlertsProps) {
  const [dismissedPromptKey, setDismissedPromptKey] = useState<string | null>(null);
  const previousPromptKey = useRef<string | null>(null);

  const prompt = getStatusPrompt({
    isInitialLoading,
    showColdStartHint,
    errorMessage,
    feedbackMessage,
    isRenderBackend
  });

  useEffect(() => {
    const nextPromptKey = prompt?.key ?? null;

    if (nextPromptKey !== previousPromptKey.current) {
      previousPromptKey.current = nextPromptKey;
      setDismissedPromptKey(null);
    }
  }, [prompt?.key]);

  useEffect(() => {
    if (!prompt?.autoDismissMs || dismissedPromptKey === prompt.key) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setDismissedPromptKey(prompt.key);
    }, prompt.autoDismissMs);

    return () => window.clearTimeout(timer);
  }, [dismissedPromptKey, prompt?.autoDismissMs, prompt?.key]);

  if (!prompt || dismissedPromptKey === prompt.key) {
    return null;
  }

  return (
    <section className="status-prompt-shell" aria-atomic="true" aria-live="polite">
      <div className={`alert alert-${prompt.variant} alert-dismissible status-prompt shadow-sm mb-0`} role={prompt.role}>
        <div className="d-flex align-items-start gap-3 pe-4">
          {prompt.showSpinner && (
            <div className="spinner-border spinner-border-sm status-prompt-spinner" aria-hidden="true" />
          )}
          <div>
            <div className="fw-semibold">{prompt.title}</div>
            <div>{prompt.message}</div>
          </div>
        </div>
        <button
          type="button"
          className="btn-close"
          aria-label="Dismiss status message"
          onClick={() => setDismissedPromptKey(prompt.key)}
        />
      </div>
    </section>
  );
}

function getStatusPrompt({
  isInitialLoading,
  showColdStartHint,
  errorMessage,
  feedbackMessage,
  isRenderBackend
}: StatusAlertsProps): StatusPrompt | null {
  if (errorMessage) {
    return {
      key: `error:${errorMessage}`,
      role: "alert",
      variant: "danger",
      title: "Request failed.",
      message: errorMessage
    };
  }

  if (showColdStartHint) {
    return {
      key: `cold-start:${isRenderBackend ? "render" : "local"}`,
      role: "status",
      variant: "warning",
      title: isRenderBackend ? "Backend may be waking up." : "Backend has not responded yet.",
      message: isRenderBackend
        ? "Render Free cold starts can take around a minute. This UI keeps waiting instead of timing out early."
        : "Check that the local backend is running on http://localhost:8080.",
      autoDismissMs: 8000
    };
  }

  if (feedbackMessage) {
    return {
      key: `feedback:${feedbackMessage}`,
      role: "status",
      variant: "success",
      title: "Demo updated.",
      message: feedbackMessage,
      autoDismissMs: 4500
    };
  }

  if (isInitialLoading) {
    return {
      key: `loading:${isRenderBackend ? "render" : "local"}`,
      role: "status",
      variant: "primary",
      title: "Loading demo data.",
      message: `Fetching registers and operation history${isRenderBackend ? " from Render. First response can be slow after idle time." : "."}`,
      autoDismissMs: 4500,
      showSpinner: true
    };
  }

  return null;
}
