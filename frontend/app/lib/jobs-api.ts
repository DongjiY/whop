import { request } from "./api-client";
import type { Task } from "./task-api";

export function listMyJobs(): Promise<Task[]> {
  return request<Task[]>("/api/jobs", {
    method: "GET",
  });
}
