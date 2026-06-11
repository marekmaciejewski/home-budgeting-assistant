import { useCallback, useEffect, useMemo, useState } from "react";
import { api, ApiError } from "./api";
import type { OperationResponse, RechargeCommand, RegisterResponse, TransferCommand } from "./apiTypes";
import { AppHeader } from "./components/AppHeader";
import { OperationsHistory } from "./components/OperationsHistory";
import { RechargeForm } from "./components/RechargeForm";
import { RegisterDashboard } from "./components/RegisterDashboard";
import { StatusAlerts } from "./components/StatusAlerts";
import { TransferForm } from "./components/TransferForm";
import type { SubmitAction } from "./types/ui";
import { formatAmount } from "./utils/formatters";
import { toSortedOperations } from "./utils/operations";

function messageFromError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  return error instanceof Error ? error.message : "Unexpected frontend error.";
}

async function fetchDemoData(): Promise<{
  registers: RegisterResponse[];
  operations: OperationResponse[];
}> {
  const [nextRegisters, nextOperations] = await Promise.all([
    api.getRegisters(),
    api.getOperations()
  ]);

  return {
    registers: nextRegisters,
    operations: toSortedOperations(nextOperations)
  };
}

export default function App() {
  const [registers, setRegisters] = useState<RegisterResponse[]>([]);
  const [operations, setOperations] = useState<OperationResponse[]>([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showColdStartHint, setShowColdStartHint] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [submitAction, setSubmitAction] = useState<SubmitAction>(null);

  const isRenderBackend = api.baseUrl.includes("onrender.com");
  // TODO: Replace this URL-based assumption with backend-provided runtime metadata.
  const isEphemeralDemo = isRenderBackend;
  const canResetDemo = isEphemeralDemo;

  const loadDemoData = useCallback(async () => {
    setErrorMessage(null);
    setShowColdStartHint(false);
    setIsRefreshing(true);

    const coldStartTimer = globalThis.setTimeout(() => {
      setShowColdStartHint(true);
    }, 7000);

    try {
      const demoData = await fetchDemoData();
      setRegisters(demoData.registers);
      setOperations(demoData.operations);
    } catch (error) {
      setErrorMessage(messageFromError(error));
    } finally {
      globalThis.clearTimeout(coldStartTimer);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    let isCurrent = true;

    const coldStartTimer = globalThis.setTimeout(() => {
      if (isCurrent) {
        setShowColdStartHint(true);
      }
    }, 7000);

    async function loadInitialDemoData() {
      try {
        const demoData = await fetchDemoData();

        if (!isCurrent) {
          return;
        }

        setRegisters(demoData.registers);
        setOperations(demoData.operations);
      } catch (error) {
        if (isCurrent) {
          setErrorMessage(messageFromError(error));
        }
      } finally {
        globalThis.clearTimeout(coldStartTimer);

        if (isCurrent) {
          setIsInitialLoading(false);
        }
      }
    }

    void loadInitialDemoData();

    return () => {
      isCurrent = false;
      globalThis.clearTimeout(coldStartTimer);
    };
  }, []);

  const totalBalance = useMemo(
    () => registers.reduce((total, register) => total + register.balance, 0),
    [registers]
  );

  const canSubmitForms =
    registers.length > 0 && !isInitialLoading && !isRefreshing && submitAction === null;

  async function refreshAfterMutation(message: string) {
    setFeedbackMessage(message);
    await loadDemoData();
  }

  async function handleRecharge(command: RechargeCommand): Promise<boolean> {
    setErrorMessage(null);
    setFeedbackMessage(null);
    setSubmitAction("recharge");

    try {
      await api.createRecharge(command);
      await refreshAfterMutation(
        `Recharged ${command.registerId} by ${formatAmount(command.amount)}.`
      );
      return true;
    } catch (error) {
      setErrorMessage(messageFromError(error));
      return false;
    } finally {
      setSubmitAction(null);
    }
  }

  async function handleTransfer(command: TransferCommand): Promise<boolean> {
    setErrorMessage(null);
    setFeedbackMessage(null);
    setSubmitAction("transfer");

    try {
      await api.createTransfer(command);
      await refreshAfterMutation(
        `Moved ${formatAmount(command.amount)} from ${command.sourceRegisterId} to ${command.targetRegisterId}.`
      );
      return true;
    } catch (error) {
      setErrorMessage(messageFromError(error));
      return false;
    } finally {
      setSubmitAction(null);
    }
  }

  async function handleReset() {
    if (!canResetDemo) {
      setErrorMessage("Reset is available only for the hosted ephemeral demo.");
      return;
    }

    setErrorMessage(null);
    setFeedbackMessage(null);
    setSubmitAction("reset");
    setShowColdStartHint(false);

    const coldStartTimer = globalThis.setTimeout(() => {
      setShowColdStartHint(true);
    }, 7000);

    try {
      const restoredRegisters = await api.resetDemo();
      const nextOperations = await api.getOperations();
      setRegisters(restoredRegisters);
      setOperations(toSortedOperations(nextOperations));
      setFeedbackMessage("Demo state reset to the seeded register balances.");
    } catch (error) {
      setErrorMessage(messageFromError(error));
    } finally {
      globalThis.clearTimeout(coldStartTimer);
      setSubmitAction(null);
    }
  }

  return (
    <div className="app-shell bg-body-tertiary min-vh-100">
      <AppHeader
        apiBaseUrl={api.baseUrl}
        isRenderBackend={isRenderBackend}
        isEphemeralDemo={isEphemeralDemo}
        canResetDemo={canResetDemo}
        isRefreshing={isRefreshing}
        isResetting={submitAction === "reset"}
        isInitialLoading={isInitialLoading}
        isBusy={submitAction !== null}
        onRefresh={() => {
          setFeedbackMessage(null);
          void loadDemoData();
        }}
        onReset={() => void handleReset()}
      />

      <main className="container py-4">
        <StatusAlerts
          isInitialLoading={isInitialLoading}
          showColdStartHint={showColdStartHint}
          errorMessage={errorMessage}
          feedbackMessage={feedbackMessage}
          isRenderBackend={isRenderBackend}
        />

        <div className="row g-4 align-items-start">
          <div className="col-lg-7 col-xl-8">
            <RegisterDashboard
              registers={registers}
              totalBalance={totalBalance}
              isLoading={isInitialLoading}
            />
            <div className="mt-4">
              <OperationsHistory operations={operations} isLoading={isInitialLoading} />
            </div>
          </div>

          <aside className="col-lg-5 col-xl-4">
            <div className="actions-stack d-grid gap-4">
              <RechargeForm
                registers={registers}
                disabled={!canSubmitForms}
                isSubmitting={submitAction === "recharge"}
                onSubmit={handleRecharge}
              />
              <TransferForm
                registers={registers}
                disabled={!canSubmitForms}
                isSubmitting={submitAction === "transfer"}
                onSubmit={handleTransfer}
              />
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}
