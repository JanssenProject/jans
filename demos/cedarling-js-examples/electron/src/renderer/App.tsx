import { useCallback, useEffect, useState, type FormEvent } from "react";
import type { CedarlingClient } from "@janssenproject/cedarling";

import type { OidcSession, PermissionMap, Task, UserId } from "../shared/contracts";
import { MAX_TITLE_LENGTH, USERS } from "../shared/contracts";
import { getRendererCedarling } from "./cedarling/init";
import { checkPermissions } from "./cedarling/permissions";
import "./App.css";

function message(error: unknown, fallback: string): string {
  return error instanceof Error ? `${fallback}: ${error.message}` : fallback;
}

function StatusBadge({ completed }: { completed: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-line px-2.5 py-0.5 text-xs font-medium text-ink-secondary">
      <span aria-hidden="true" className={`inline-block h-1.5 w-1.5 rounded-full ${completed ? "bg-success" : "bg-warning"}`} />
      {completed ? "Completed" : "Incomplete"}
    </span>
  );
}

export default function App() {
  const [client, setClient] = useState<CedarlingClient | null>(null);
  const [session, setSession] = useState<OidcSession>({ authenticated: false });
  const [currentUser, setCurrentUser] = useState<UserId>("bob");
  const [tasks, setTasks] = useState<Task[]>([]);
  const [permissions, setPermissions] = useState<PermissionMap>({});
  const [newTitle, setNewTitle] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([getRendererCedarling(), window.electron.oidc.session()])
      .then(([cedarling, activeSession]) => {
        if (!active) return;
        setClient(cedarling);
        setSession(activeSession);
        if (activeSession.authenticated && activeSession.userId) setCurrentUser(activeSession.userId);
      })
      .catch((cause) => active && setError(message(cause, "Initialization failed")));
    return () => {
      active = false;
    };
  }, []);

  const fetchTasks = useCallback(async () => {
    try {
      const value = await window.electron.tasks.list({ userId: currentUser });
      setTasks(value);
      setError("");
    } catch (cause) {
      setTasks([]);
      setError(message(cause, "Failed to load tasks"));
    }
  }, [currentUser]);

  useEffect(() => {
    void fetchTasks();
  }, [fetchTasks, session]);

  useEffect(() => {
    if (!client || tasks.length === 0) return;
    let active = true;
    setChecking(true);
    checkPermissions(client, currentUser, tasks, session.authenticated)
      .then((value) => active && setPermissions(value))
      .catch((cause) => active && setError(message(cause, "Permission check failed")))
      .finally(() => active && setChecking(false));
    return () => {
      active = false;
    };
  }, [client, currentUser, session, tasks]);

  async function runTaskMutation(
    operation: () => Promise<unknown>,
    failureMessage: string,
    onSuccess?: () => void,
  ) {
    try {
      await operation();
      onSuccess?.();
      await fetchTasks();
      setError("");
    } catch (cause) {
      setError(message(cause, failureMessage));
    }
  }

  async function createTask(event: FormEvent) {
    event.preventDefault();
    if (!newTitle.trim()) return;
    await runTaskMutation(
      () => window.electron.tasks.create({ userId: currentUser, title: newTitle }),
      "Failed to create task",
      () => setNewTitle(""),
    );
  }

  async function updateTask(task: Task) {
    await runTaskMutation(
      () => window.electron.tasks.update({ userId: currentUser, id: task.id, completed: !task.completed }),
      "Failed to update task",
    );
  }

  async function deleteTask(task: Task) {
    await runTaskMutation(
      () => window.electron.tasks.delete({ userId: currentUser, id: task.id }),
      "Failed to delete task",
    );
  }

  async function login() {
    setBusy(true);
    try {
      const activeSession = await window.electron.oidc.login(currentUser);
      setSession(activeSession);
      setError("");
    } catch (cause) {
      setError(message(cause, "OIDC login failed"));
    } finally {
      setBusy(false);
    }
  }

  async function logout() {
    setBusy(true);
    try {
      setSession(await window.electron.oidc.logout());
    } finally {
      setBusy(false);
    }
  }

  const userLabel = USERS.find((user) => user.id === currentUser)?.label ?? currentUser;

  return (
    <main className="mx-auto max-w-5xl px-6 py-8 lg:px-10">
      <header className="mb-7 flex flex-wrap items-center justify-between gap-5 border-b border-line pb-5">
        <div>
          <p className="demo-kicker">Cedarling JavaScript SDK example</p>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-ink">TaskApp</h1>
            <span className="demo-runtime-badge">Electron desktop</span>
          </div>
          <p className="mt-1 text-sm text-ink-muted">
            Keep desktop credentials isolated while authorizing in both processes.
          </p>
        </div>
        <label className="flex items-center gap-2 text-sm text-ink-muted">
          <span className="font-semibold text-ink-secondary">User</span>
          <select
            value={currentUser}
            disabled={session.authenticated}
            onChange={(event) => setCurrentUser(event.target.value as UserId)}
            className="rounded border border-input bg-surface px-3 py-1.5"
          >
            {USERS.map((user) => (
              <option key={user.id} value={user.id}>{user.label} ({user.note})</option>
            ))}
          </select>
        </label>
      </header>

      <section className="demo-notice mb-7 p-4">
        <h2 className="font-semibold text-ink">Where authorization happens</h2>
        <p className="mt-1 text-sm leading-6 text-ink-secondary">
          The renderer previews unsigned Cedarling decisions. Electron main owns signed tokens,
          constructs trusted resources, and independently enforces every task operation.
        </p>
      </section>

      <section className="demo-card mb-7 p-5">
        <h2 className="mb-3 text-lg font-semibold text-ink">Identity source</h2>
        {session.authenticated ? (
          <div className="flex items-center gap-3">
            <span className="text-sm text-success">Signed main-process session for {userLabel}</span>
            <button
              type="button"
              disabled={busy}
              onClick={() => void logout()}
              className="demo-button demo-button--secondary"
            >
              Log out
            </button>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-sm text-ink-muted">Explicit unsigned identity for {userLabel}</span>
            <button
              type="button"
              disabled={busy}
              onClick={() => void login()}
              className="demo-button demo-button--primary"
            >
              {busy ? "Waiting for browser..." : "Sign in with OIDC + PKCE"}
            </button>
          </div>
        )}
      </section>

      {error && <div role="alert" className="mb-6 rounded border border-danger bg-danger-subtle p-3 text-sm text-danger">{error}</div>}

      <section className="demo-card mb-7 p-5">
        <h2 className="mb-4 text-lg font-semibold text-ink">New task</h2>
        <form onSubmit={(event) => void createTask(event)} className="flex gap-3">
          <input aria-label="Task title" required maxLength={MAX_TITLE_LENGTH} value={newTitle} onChange={(event) => setNewTitle(event.target.value)} placeholder="Enter task title" className="flex-1 rounded border border-input px-4 py-2" />
          <button className="demo-button demo-button--primary px-5">Add task</button>
        </form>
      </section>

      <section className="demo-card overflow-hidden">
        <table className="w-full border-collapse text-sm">
          <thead className="border-b border-line bg-surface-muted text-left text-xs uppercase text-ink-muted"><tr><th className="px-4 py-3">Task</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Owner</th><th className="px-4 py-3 text-right">Actions</th></tr></thead>
          <tbody>
            {tasks.map((task) => {
              const permission = permissions[task.id] ?? { canUpdate: false, canDelete: false };
              return <tr key={task.id} className="border-b border-line-light last:border-0">
                <td className="px-4 py-3 font-medium">{task.title}</td><td className="px-4 py-3"><StatusBadge completed={task.completed} /></td><td className="px-4 py-3 text-ink-muted">{task.owner}</td>
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
              </tr>;
            })}
          </tbody>
        </table>
        {!tasks.length && <p className="p-8 text-center text-ink-disabled">No tasks found.</p>}
        {checking && <p className="p-2 text-center text-xs text-ink-muted">Checking permissions...</p>}
      </section>
    </main>
  );
}
