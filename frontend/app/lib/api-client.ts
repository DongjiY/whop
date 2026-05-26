export type ApiError = {
  code: string;
  message: string;
};

export class ApiRequestError extends Error {
  code: string;
  status: number;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "https://whop-production.up.railway.app";

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    let error: ApiError | undefined;
    try {
      error = (await response.json()) as ApiError;
    } catch {
      error = undefined;
    }
    throw new ApiRequestError(
      response.status,
      error?.code ?? "UNKNOWN_ERROR",
      error?.message ?? "Request failed",
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
