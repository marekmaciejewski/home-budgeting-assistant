import type {
  OperationResponse,
  ProblemDetail,
  RechargeCommand,
  RegisterResponse,
  TransferCommand
} from "./apiTypes";

const fallbackApiBaseUrl = "http://localhost:8080";
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || fallbackApiBaseUrl).replace(/\/$/, "");

export class ApiError extends Error {
  readonly status?: number;
  readonly problem?: ProblemDetail;

  constructor(message: string, status?: number, problem?: ProblemDetail) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
  }
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.title === "string" &&
    typeof candidate.status === "number" &&
    typeof candidate.detail === "string" &&
    typeof candidate.instance === "string"
  );
}

async function readResponseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json") || contentType.includes("application/problem+json")) {
    return response.json();
  }

  const text = await response.text();
  return text.length > 0 ? text : undefined;
}

function formatProblem(problem: ProblemDetail): string {
  const fieldErrors = problem.errors?.map((error) => `${error.field}: ${error.message}`).join("; ");
  return fieldErrors ? `${problem.detail} (${fieldErrors})` : problem.detail;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;

  try {
    const headers = new Headers(init.headers);
    headers.set("Accept", "application/json");

    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers
    });
  } catch (error) {
    const detail = error instanceof Error ? error.message : "unknown network error";
    throw new ApiError(
      `Could not reach the backend (${detail}). If this is the first request after inactivity, Render may still be waking up. If it keeps failing from GitHub Pages, verify the Pages origin is allowed by backend CORS.`
    );
  }

  const body = await readResponseBody(response);

  if (!response.ok) {
    if (isProblemDetail(body)) {
      throw new ApiError(formatProblem(body), response.status, body);
    }

    throw new ApiError(`Request failed with HTTP ${response.status}.`, response.status);
  }

  return body as T;
}

function jsonRequest<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
}

export const api = {
  baseUrl: apiBaseUrl,
  getRegisters: () => request<RegisterResponse[]>("/registers"),
  getOperations: () => request<OperationResponse[]>("/operations"),
  createRecharge: (command: RechargeCommand) =>
    jsonRequest<OperationResponse>("/operations/recharges", command),
  createTransfer: (command: TransferCommand) =>
    jsonRequest<OperationResponse>("/operations/transfers", command),
  resetDemo: () => jsonRequest<RegisterResponse[]>("/demo/reset")
};
