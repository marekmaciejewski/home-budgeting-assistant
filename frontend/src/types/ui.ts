import type { LedgerVerificationResponse } from "../apiTypes";

export type SubmitAction = "recharge" | "transfer" | "tamper" | "reset" | null;

export type LedgerVerificationState =
  | { kind: "pending" }
  | { kind: "verified"; data: LedgerVerificationResponse }
  | { kind: "invalid"; data: LedgerVerificationResponse }
  | { kind: "unavailable" };
