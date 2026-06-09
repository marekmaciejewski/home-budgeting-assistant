import { FormEvent, useEffect, useState } from "react";
import type { RegisterResponse, TransferCommand } from "../apiTypes";

export type TransferFormProps = {
  registers: RegisterResponse[];
  disabled: boolean;
  isSubmitting: boolean;
  onSubmit: (command: TransferCommand) => Promise<boolean>;
};

export function TransferForm({ registers, disabled, isSubmitting, onSubmit }: TransferFormProps) {
  const [sourceRegisterId, setSourceRegisterId] = useState("");
  const [targetRegisterId, setTargetRegisterId] = useState("");
  const [amount, setAmount] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (registers.length === 0) {
      setSourceRegisterId("");
      setTargetRegisterId("");
      return;
    }

    setSourceRegisterId((current) => current || registers[0].id);
    setTargetRegisterId((current) => (current || registers[1]?.id) ?? registers[0].id);
  }, [registers]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationError(null);

    const parsedAmount = Number.parseFloat(amount);
    if (!sourceRegisterId || !targetRegisterId || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setValidationError("Choose both registers and enter an amount greater than 0.");
      return;
    }

    if (sourceRegisterId === targetRegisterId) {
      setValidationError("Source and target register must be different.");
      return;
    }

    const succeeded = await onSubmit({ sourceRegisterId, targetRegisterId, amount: parsedAmount });
    if (succeeded) {
      setAmount("");
    }
  }

  return (
    <form className="card border-0 shadow-sm" onSubmit={handleSubmit}>
      <div className="card-header bg-white py-3">
        <h2 className="h5 mb-1">Transfer</h2>
        <div className="text-secondary small">Move funds between two registers.</div>
      </div>
      <div className="card-body vstack gap-3">
        {validationError && <div className="alert alert-warning py-2 mb-0">{validationError}</div>}

        <div className="row g-3">
          <div className="col-sm-6">
            <label className="form-label fw-semibold" htmlFor="transfer-source">
              Source
            </label>
            <select
              className="form-select"
              id="transfer-source"
              value={sourceRegisterId}
              onChange={(event) => setSourceRegisterId(event.target.value)}
              disabled={disabled}
            >
              {registers.map((register) => (
                <option key={register.id} value={register.id}>
                  {register.id}
                </option>
              ))}
            </select>
          </div>

          <div className="col-sm-6">
            <label className="form-label fw-semibold" htmlFor="transfer-target">
              Target
            </label>
            <select
              className="form-select"
              id="transfer-target"
              value={targetRegisterId}
              onChange={(event) => setTargetRegisterId(event.target.value)}
              disabled={disabled}
            >
              {registers.map((register) => (
                <option key={register.id} value={register.id}>
                  {register.id}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className="form-label fw-semibold" htmlFor="transfer-amount">
            Amount
          </label>
          <input
            className="form-control"
            id="transfer-amount"
            inputMode="decimal"
            min="0.01"
            step="0.01"
            type="number"
            placeholder="1500.00"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            disabled={disabled}
          />
        </div>

        <button className="btn btn-primary w-100" type="submit" disabled={disabled}>
          {isSubmitting ? "Transferring..." : "Transfer"}
        </button>
      </div>
    </form>
  );
}

