import { request } from "./api-client";

export type TaskOwner = {
  id: string;
  username: string;
};

export type Task = {
  id: string;
  title: string;
  description: string;
  budgetAmount: number;
  budgetCurrency: "USD";
  status: "OPEN" | "ASSIGNED";
  createdAt: string;
  owner: TaskOwner;
  acceptedOffer: {
    id: string;
    amount: number;
    currency: "USD";
    seller: TaskOwner;
  } | null;
};

export type CreateTaskPayload = {
  title: string;
  description: string;
  budgetAmount: number;
  budgetCurrency: "USD";
};

export function listTasks(): Promise<Task[]> {
  return request<Task[]>("/api/tasks", {
    method: "GET",
  });
}

export function getTask(id: string): Promise<Task> {
  return request<Task>(`/api/tasks/${id}`, {
    method: "GET",
  });
}

export function createTask(payload: CreateTaskPayload): Promise<Task> {
  return request<Task>("/api/tasks", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
