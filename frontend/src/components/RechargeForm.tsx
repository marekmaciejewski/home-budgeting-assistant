import { FormEvent, useEffect, useState } from "react";
import type { RechargeCommand, RegisterResponse } from "../apiTypes";

export type RechargeFormProps = {
  registers: RegisterResponse[];
  disabled: boolean;
  isSubmitting: boolean;
  onSubmit: (command: RechargeCommand) => Promise<boolean>;
};

export function RechargeForm({ registers, disabled, isSubmitting, onSubmit }: RechargeFormProps) {
  const [registerId, setRegisterId] = useState("");
  const [amount, setAmount] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (registers.length === 0) {
      setRegisterId("");
      return;
    }

    setRegisterId((current) => current || registers[0].id);
  }, [registers]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setValidationError(null);

    const parsedAmount = Number.parseFloat(amount);
    if (!registerId || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setValidationError("Choose a register and enter an amount greater than 0.");
      return;
    }

    const succeeded = await onSubmit({ registerId, amount: parsedAmount });
    if (succeeded) {
      setAmount("");
    }
  }

  return (
    <form className="card border-0 shadow-sm" onSubmit={handleSubmit}>
      <div className="card-header bg-white py-3">
        <h2 className="h5 mb-1">Recharge</h2>
        <div className="text-secondary small">Add funds to one register.</div>
      </div>
      <div className="card-body vstack gap-3">
        {validationError && <div className="alert alert-warning py-2 mb-0">{validationError}</div>}

        <div>
          <label className="form-label fw-semibold" htmlFor="recharge-register">
            Target register
          </label>
          <select
            className="form-select"
            id="recharge-register"
            value={registerId}
            onChange={(event) => setRegisterId(event.target.value)}
            disabled={disabled}
          >
            {registers.map((register) => (
              <option key={register.id} value={register.id}>
                {register.id}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="form-label fw-semibold" htmlFor="recharge-amount">
            Amount
          </label>
          <input
            className="form-control"
            id="recharge-amount"
            inputMode="decimal"
            min="0.01"
            step="0.01"
            type="number"
            placeholder="2500.00"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            disabled={disabled}
          />
        </div>

        <button className="btn btn-primary w-100" type="submit" disabled={disabled}>
          {isSubmitting ? "Recording..." : "Create recharge"}
        </button>
      </div>
    </form>
  );
}
