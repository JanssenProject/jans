"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";

import {
  MAX_TITLE_LENGTH,
  USERS,
  type PermissionMap,
  type Task,
  type UserId,
} from "@/libs/demo-domain";

import * as taskApi from "./task-api";

type OidcSession = { authenticated: false } | { authenticated: true; userId: UserId };

function StatusBadge({ completed }: { completed: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-line px-2.5 py-0.5 text-xs font-medium text-ink-secondary">
      <span aria-hidden="true" className={`inline-block h-1.5 w-1.5 rounded-full ${completed ? "bg-success" : "bg-warning"}`} />
      {completed ? "Completed" : "Incomplete"}
    </span>
  );
}

function message(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

export default function App() {
  const [currentUser, setCurrentUser] = useState<UserId>("bob");
  const [session, setSession] = useState<OidcSession>({ authenticated: false });
  const [sessionReady, setSessionReady] = useState(false);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [newTitle, setNewTitle] = useState("");
  const [permissions, setPermissions] = useState<PermissionMap>({});
  const [error, setError] = useState("");
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams(window.location.search);
    if (query.has("oidc_error")) {
      setError("OIDC authentication failed. Please try again.");
      window.history.replaceState(null, "", window.location.pathname);
    }
    fetch("/api/oidc/session", { cache: "no-store" })
      .then(async (response) => (await response.json()) as OidcSession)
      .then((value) => {
        if (value.authenticated) {
          setSession(value);
          setCurrentUser(value.userId);
        }
      })
      .catch(() => setSession({ authenticated: false }))
      .finally(() => setSessionReady(true));
  }, []);

  const requestHeaders = useCallback(
    (json = false): HeadersInit => {
      const headers: Record<string, string> = {};
      if (!session.authenticated) headers["x-user-id"] = currentUser;
      if (json) headers["content-type"] = "application/json";
      return headers;
    },
    [currentUser, session],
  );

  const fetchTasks = useCallback(async () => {
    if (!sessionReady) return;
    try {
      setTasks(await taskApi.listTasks(requestHeaders()));
      setError("");
    } catch (cause) {
      setTasks([]);
      setError(message(cause, "Failed to fetch tasks"));
    }
  }, [requestHeaders, sessionReady]);

  useEffect(() => {
    void fetchTasks();
  }, [fetchTasks]);

  useEffect(() => {
    if (!sessionReady || tasks.length === 0) return;
    // Permission responses only control button state; mutation routes enforce
    // the same Cedarling policies again on the server.
    let active = true;
    setChecking(true);
    taskApi.checkTaskPermissions(tasks, requestHeaders())
      .then((value) => {
        if (active) setPermissions(value);
      })
      .catch((cause) => {
        if (!active) return;
        setPermissions({});
        setError(message(cause, "Permission check failed"));
      })
      .finally(() => {
        if (active) setChecking(false);
      });
    return () => {
      active = false;
    };
  }, [requestHeaders, sessionReady, tasks]);

  async function createTask(event: FormEvent) {
    event.preventDefault();
    if (!newTitle.trim()) return;
    try {
      await taskApi.createTask(newTitle, requestHeaders(true));
      setNewTitle("");
      await fetchTasks();
    } catch (cause) {
      setError(message(cause, "Task creation failed"));
    }
  }

  async function updateTask(task: Task) {
    try {
      await taskApi.updateTask(task, requestHeaders(true));
      await fetchTasks();
    } catch (cause) {
      setError(message(cause, "Task update failed"));
    }
  }

  async function deleteTask(task: Task) {
    try {
      await taskApi.deleteTask(task, requestHeaders());
      await fetchTasks();
    } catch (cause) {
      setError(message(cause, "Task deletion failed"));
    }
  }

  const userLabel = USERS.find((user) => user.id === currentUser)?.label ?? currentUser;

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="mb-7 flex flex-wrap items-center justify-between gap-5 border-b border-line pb-5">
        <div>
          <p className="demo-kicker">Cedarling JavaScript SDK example</p>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-ink">TaskApp</h1>
            <span className="demo-runtime-badge">Next.js Node + Edge</span>
          </div>
          <p className="mt-1 text-sm text-ink-muted">
            Exercise the same authorization model in two Next.js server runtimes.
          </p>
        </div>
        <label className="flex items-center gap-2 text-sm text-ink-muted">
          <span className="font-semibold text-ink-secondary">User</span>
          <select
            value={currentUser}
            disabled={session.authenticated}
            onChange={(event) => setCurrentUser(event.target.value as UserId)}
            className="rounded-md border border-input bg-surface px-3 py-1.5"
          >
            {USERS.map((user) => <option key={user.id} value={user.id}>{user.label} ({user.note})</option>)}
          </select>
        </label>
      </header>

      <section className="demo-notice mb-7 p-4">
        <h2 className="font-semibold text-ink">Where authorization happens</h2>
        <p className="mt-1 text-sm leading-6 text-ink-secondary">
          The page asks a Node route for permission previews. Task routes independently enforce
          each operation, while the parallel Edge route demonstrates the SDK&apos;s Edge adapter.
        </p>
      </section>

      <section className="demo-card mb-7 p-5">
        <h2 className="mb-3 text-lg font-semibold text-ink">Identity source</h2>
        {session.authenticated ? (
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-sm text-success">Signed OIDC session for {userLabel}</span>
            <button
              type="button"
              onClick={() => window.location.assign("/api/oidc/logout")}
              className="demo-button demo-button--secondary"
            >
              Log out
            </button>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-sm text-ink-muted">Using explicit unsigned identity for {userLabel}</span>
            <button
              type="button"
              onClick={() => window.location.assign(`/api/oidc/start?user=${encodeURIComponent(currentUser)}`)}
              className="demo-button demo-button--primary"
            >
              Sign in with OIDC + PKCE
            </button>
          </div>
        )}
        <p className="mt-3 text-xs text-ink-disabled">A valid server session always takes precedence. Log out before using unsigned mode.</p>
      </section>

      {error && <div role="alert" className="mb-6 rounded border border-danger bg-danger-subtle p-3 text-sm text-danger">{error}</div>}

      <section className="demo-card mb-7 p-5">
        <h2 className="mb-4 text-lg font-semibold text-ink">New task</h2>
        <form onSubmit={(event) => void createTask(event)} className="flex gap-3">
          <input
            aria-label="Task title"
            required
            maxLength={MAX_TITLE_LENGTH}
            value={newTitle}
            onChange={(event) => setNewTitle(event.target.value)}
            placeholder="Enter task title"
            className="flex-1 rounded border border-input px-4 py-2"
          />
          <button className="demo-button demo-button--primary px-5">
            Add task
          </button>
        </form>
      </section>

      <section className="demo-card overflow-x-auto">
        <table className="w-full min-w-[720px] border-collapse text-sm">
          <thead className="border-b border-line bg-surface-muted text-left text-xs uppercase text-ink-muted">
            <tr><th className="px-4 py-3">Task</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Owner</th><th className="px-4 py-3 text-right">Actions</th></tr>
          </thead>
          <tbody>
            {tasks.map((task) => {
              const permission = permissions[task.id] ?? { canUpdate: false, canDelete: false };
              return (
                <tr key={task.id} className="border-b border-line-light last:border-0">
                  <td className="px-4 py-3 font-medium">{task.title}</td>
                  <td className="px-4 py-3"><StatusBadge completed={task.completed} /></td>
                  <td className="px-4 py-3 text-ink-muted">{task.owner}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        disabled={!permission.canUpdate}
                        onClick={() => void updateTask(task)}
                        className="demo-button demo-button--secondary py-1"
                      >
                        {task.completed ? "Undo" : "Complete"}
                      </button>
                      <button
                        disabled={!permission.canDelete}
                        onClick={() => void deleteTask(task)}
                        className="demo-button demo-button--danger py-1"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!tasks.length && <p className="p-8 text-center text-ink-disabled">No tasks found.</p>}
        {checking && <p className="p-2 text-center text-xs text-ink-muted">Checking permissions...</p>}
      </section>
    </main>
  );
}
