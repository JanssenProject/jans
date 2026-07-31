import {
  isTask,
  type PermissionMap,
  type Task,
} from "@/libs/demo-domain";

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
    response = await fetch(path, init);
  } catch {
    throw new Error("Connection failed");
  }
  if (!response.ok) {
    throw new Error(await responseError(response, failureMessage));
  }
  return response;
}

export async function listTasks(headers: HeadersInit): Promise<Task[]> {
  const response = await request("/api/tasks", { headers }, "Failed to fetch tasks");
  const value: unknown = await response.json();
  if (!Array.isArray(value) || !value.every(isTask)) {
    throw new Error("Task API returned invalid data");
  }
  return value;
}

export async function checkTaskPermissions(
  tasks: readonly Task[],
  headers: HeadersInit,
): Promise<PermissionMap> {
  const entries = await Promise.all(
    tasks.map(async (task) => {
      const check = async (action: "UpdateTask" | "DeleteTask") => {
        const query = new URLSearchParams({ action, taskId: task.id });
        const response = await request(
          `/api/check?${query}`,
          { headers },
          "Permission check failed",
        );
        const body = (await response.json()) as { allowed?: unknown };
        return body.allowed === true;
      };
      const [canUpdate, canDelete] = await Promise.all([
        check("UpdateTask"),
        check("DeleteTask"),
      ]);
      return [task.id, { canUpdate, canDelete }] as const;
    }),
  );
  return Object.fromEntries(entries);
}

export async function createTask(
  title: string,
  headers: HeadersInit,
): Promise<void> {
  await request(
    "/api/tasks",
    { method: "POST", headers, body: JSON.stringify({ title }) },
    "Task creation failed",
  );
}

export async function updateTask(
  task: Task,
  headers: HeadersInit,
): Promise<void> {
  await request(
    `/api/tasks/${encodeURIComponent(task.id)}`,
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
    `/api/tasks/${encodeURIComponent(task.id)}`,
    { method: "DELETE", headers },
    "Task deletion failed",
  );
}
