import { afterEach, describe, expect, test, vi } from "vitest";

import { createTask, listTasks } from "./task-api";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("task API", () => {
  test("accepts only the known task response shape", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify([
            {
              id: "task-1",
              title: "Buy groceries",
              completed: false,
              owner: "bob",
            },
          ]),
          { status: 200 },
        ),
      ),
    );

    await expect(listTasks({ "x-user-id": "bob" })).resolves.toHaveLength(1);
  });

  test("rejects task data with an unknown owner", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify([
            { id: "task-1", title: "Forged", completed: false, owner: "mallory" },
          ]),
          { status: 200 },
        ),
      ),
    );

    await expect(listTasks({ "x-user-id": "bob" })).rejects.toThrow(
      "Backend returned invalid tasks",
    );
  });

  test("surfaces the backend error for a failed mutation", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: "Forbidden by policy" }), { status: 403 }),
      ),
    );

    await expect(createTask("Denied", {})).rejects.toThrow("Forbidden by policy");
  });
});
