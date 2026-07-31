import { useCallback, useEffect, useState, type FormEvent } from "react";
import type { CedarlingClient } from "@janssenproject/cedarling";

import { initCedarling } from "./cedarling/init";
import { checkPermissions } from "./cedarling/permissions";
import { isUserId, USERS, type PermissionMap, type Task, type UserId } from "./model";
import * as taskApi from "./task-api";
import {
  clearSignedSession,
  completeLogin,
  getSignedSession,
  startLogin,
  type SignedSession,
} from "./oidc";

function message(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

function StatusBadge({ completed }: { completed: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-line px-2.5 py-0.5 text-xs font-medium text-ink-secondary">
      <span
        aria-hidden="true"
        className={`inline-block h-1.5 w-1.5 rounded-full ${completed ? "bg-success" : "bg-warning"}`}
      />
      {completed ? "Completed" : "Incomplete"}
    </span>
  );
}

export default function App() {
  const initialSession = getSignedSession();
  const [client, setClient] = useState<CedarlingClient | null>(null);
  const [signedSession, setSignedSession] = useState<SignedSession | null>(initialSession);
  const [signedMode, setSignedMode] = useState(Boolean(initialSession));
  const [currentUser, setCurrentUser] = useState<UserId>(
    initialSession && isUserId(initialSession.userId)
      ? initialSession.userId
      : "bob",
  );
  const [tasks, setTasks] = useState<Task[]>([]);
  const [permissions, setPermissions] = useState<PermissionMap>({});
  const [newTitle, setNewTitle] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [checkingPermissions, setCheckingPermissions] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.all([initCedarling(), completeLogin()])
      .then(([cedarling, completedSession]) => {
        if (!active) return;
        setClient(cedarling);
        const session = completedSession ?? getSignedSession();
        if (session && isUserId(session.userId)) {
          setSignedSession(session);
          setSignedMode(true);
          setCurrentUser(session.userId);
        }
      })
      .catch((error: unknown) => {
        if (active) setErrorMessage(message(error, "Initialization failed"));
      });
    return () => {
      active = false;
    };
  }, []);

  const requestHeaders = useCallback(
    (json = false): HeadersInit => {
      const headers: Record<string, string> = { "x-user-id": currentUser };
      if (json) headers["content-type"] = "application/json";
      if (signedMode && signedSession) {
        headers.authorization = `Bearer ${signedSession.userinfoToken}`;
      }
      return headers;
    },
    [currentUser, signedMode, signedSession],
  );

  const fetchTasks = useCallback(async () => {
    try {
      setTasks(await taskApi.listTasks(requestHeaders()));
      setErrorMessage("");
    } catch (error) {
      setTasks([]);
      setErrorMessage(message(error, "Failed to fetch tasks"));
    }
  }, [requestHeaders]);

  useEffect(() => {
    void fetchTasks();
  }, [fetchTasks]);

  useEffect(() => {
    if (!client || tasks.length === 0) {
      setPermissions({});
      return;
    }
    // Cedarling calculates a browser-side preview; button state is never the
    // backend's authorization boundary.
    let active = true;
    setCheckingPermissions(true);
    checkPermissions(
      client,
      currentUser,
      tasks,
      signedMode ? signedSession?.userinfoToken : undefined,
    )
      .then((value) => {
        if (active) setPermissions(value);
      })
      .catch(() => {
        if (active) setPermissions({});
      })
      .finally(() => {
        if (active) setCheckingPermissions(false);
      });
    return () => {
      active = false;
    };
  }, [client, currentUser, signedMode, signedSession, tasks]);

  async function createTask(event: FormEvent) {
    event.preventDefault();
    if (!newTitle.trim()) return;
    try {
      await taskApi.createTask(newTitle, requestHeaders(true));
      setNewTitle("");
      await fetchTasks();
    } catch (error) {
      setErrorMessage(message(error, "Task creation failed"));
    }
  }

  async function updateTask(task: Task) {
    try {
      await taskApi.updateTask(task, requestHeaders(true));
      await fetchTasks();
    } catch (error) {
      setErrorMessage(message(error, "Task update failed"));
    }
  }

  async function deleteTask(task: Task) {
    try {
      await taskApi.deleteTask(task, requestHeaders());
      await fetchTasks();
    } catch (error) {
      setErrorMessage(message(error, "Task deletion failed"));
    }
  }

  async function login() {
    setErrorMessage("");
    try {
      await startLogin(currentUser);
    } catch (error) {
      setErrorMessage(message(error, "OIDC login failed"));
    }
  }

  function logout() {
    clearSignedSession();
    setSignedSession(null);
    setSignedMode(false);
  }

  const userLabel = USERS.find((user) => user.id === currentUser)?.label ?? currentUser;

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="mb-7 flex flex-wrap items-center justify-between gap-5 border-b border-line pb-5">
        <div>
          <p className="demo-kicker">Cedarling JavaScript SDK example</p>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-ink">TaskApp</h1>
            <span className="demo-runtime-badge">React + Express</span>
          </div>
          <p className="mt-1 text-sm text-ink-muted">
            Compare signed OIDC identity with an explicit unsigned application identity.
          </p>
        </div>
        <label className="flex items-center gap-2 text-sm text-ink-muted">
          <span className="font-semibold text-ink-secondary">User</span>
          <select
            value={currentUser}
            disabled={signedMode}
            onChange={(event) => setCurrentUser(event.target.value as UserId)}
            className="rounded-md border border-input bg-surface px-3 py-1.5 text-ink-secondary"
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
          The browser uses Cedarling to preview action availability. The Express API constructs
          trusted resource attributes and independently enforces every task operation.
        </p>
      </section>

      <section className="demo-card mb-7 p-5">
        <h2 className="mb-3 text-lg font-semibold text-ink">Identity source</h2>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => setSignedMode(false)}
            className="demo-button demo-button--secondary"
          >
            Use explicit unsigned identity
          </button>
          {signedSession ? (
            <>
              <button
                type="button"
                onClick={() => setSignedMode(true)}
                className="demo-button demo-button--primary"
              >
                Use signed UserInfo
              </button>
              <button type="button" onClick={logout} className="demo-button demo-button--secondary">
                Log out
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={() => void login()}
              className="demo-button demo-button--primary"
            >
              Sign in as {userLabel} with OIDC + PKCE
            </button>
          )}
        </div>
        <p className="mt-3 text-xs text-ink-muted">
          Active mode: {signedMode ? "signed RS256 UserInfo" : "application-asserted user"}.
          Signed tokens live only in this browser tab session.
        </p>
      </section>

      {errorMessage && (
        <div role="alert" className="mb-6 rounded border border-danger bg-danger-subtle p-3 text-sm text-danger">
          {errorMessage}
        </div>
      )}

      <section className="demo-card mb-7 p-5">
        <h2 className="mb-4 text-lg font-semibold text-ink">New task</h2>
        <form onSubmit={(event) => void createTask(event)} className="flex gap-3">
          <input
            aria-label="Task title"
            maxLength={120}
            required
            value={newTitle}
            onChange={(event) => setNewTitle(event.target.value)}
            placeholder="Enter task title"
            className="flex-1 rounded-md border border-input bg-surface px-4 py-2 text-sm"
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
        {tasks.length === 0 && <p className="p-8 text-center text-ink-disabled">No tasks available.</p>}
        {checkingPermissions && <p className="p-2 text-center text-xs text-ink-muted">Checking permissions...</p>}
      </section>
    </main>
  );
}
