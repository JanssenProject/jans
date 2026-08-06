import { isTask, type Task } from "./model";

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL ?? "http://localhost:8080";

async function responseError(response: Response, fallback: string): Promise<string> {
  const body = (await response.json().catch(() => null)) as { error?: unknown } | null;
  return typeof body?.error === "string" ? body.error : fallback;
}

async function request(
  path: string,
  init: RequestInit,
  failureMessage: string,
): Promise<Response> {
  let response: Response;
  try {
    response = await fetch(`${BACKEND_URL}${path}`, init);
  } catch {
    throw new Error("Backend connection failed");
  }
  if (!response.ok) {
    throw new Error(await responseError(response, failureMessage));
  }
  return response;
}

export async function listTasks(headers: HeadersInit): Promise<Task[]> {
  const response = await request("/tasks", { headers }, "Failed to fetch tasks");
  const value: unknown = await response.json();
  if (!Array.isArray(value) || !value.every(isTask)) {
    throw new Error("Backend returned invalid tasks");
  }
  return value;
}

export async function createTask(
  title: string,
  headers: HeadersInit,
): Promise<void> {
  await request(
    "/tasks",
    { method: "POST", headers, body: JSON.stringify({ title }) },
    "Task creation failed",
  );
}

export async function updateTask(
  task: Task,
  headers: HeadersInit,
): Promise<void> {
  await request(
    `/tasks/${encodeURIComponent(task.id)}`,
    {
      method: "PUT",
      headers,
      body: JSON.stringify({ completed: !task.completed }),
    },
    "Task update failed",
  );
}

export async function deleteTask(
  task: Task,
  headers: HeadersInit,
): Promise<void> {
  await request(
    `/tasks/${encodeURIComponent(task.id)}`,
    { method: "DELETE", headers },
    "Task deletion failed",
  );
}
