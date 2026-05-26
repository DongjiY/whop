import { ApiRequestError, request } from "./api-client";
export { ApiRequestError } from "./api-client";

export type MeResponse = {
  id: string;
  username: string;
  createdAt: string;
};

export type AuthPayload = {
  username: string;
  password: string;
};

export function signup(payload: AuthPayload): Promise<MeResponse> {
  return request<MeResponse>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function login(payload: AuthPayload): Promise<MeResponse> {
  return request<MeResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function logout(): Promise<void> {
  return request<void>("/api/auth/logout", {
    method: "POST",
  });
}

export async function getMe(): Promise<MeResponse | null> {
  try {
    return await request<MeResponse>("/api/auth/me", {
      method: "GET",
    });
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 401) {
      return null;
    }
    throw error;
  }
}
