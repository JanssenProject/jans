import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { CedarlingClient } from '@janssenproject/cedarling';
import { getRendererCedarling } from './cedarling/init';
import {
  checkUnsignedPermissions,
  type PermissionMap,
  type Task,
} from './cedarling/exercise-unsigned';
import { checkSignedPermissions } from './cedarling/exercise-signed';
import './App.css';

const USERS = [
  { id: 'bob', label: 'Bob', note: 'owner of "Buy groceries"' },
  { id: 'alice', label: 'Alice', note: 'owner of "Schedule meeting with CEO"' },
  { id: 'charlie', label: 'Charlie', note: 'guest user' },
] as const;

type UserId = (typeof USERS)[number]['id'];
type AuthMode = 'unsigned' | 'signed-idp';
type SessionMap = Partial<Record<UserId, boolean>>;

type SessionResponse = {
  readonly authenticated: boolean;
};

function isUserId(value: unknown): value is UserId {
  return USERS.some(({ id }) => id === value);
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? `${fallback}: ${error.message}` : fallback;
}

function StatusBadge({ completed }: { readonly completed: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-default px-2.5 py-0.5 text-xs font-medium text-text-secondary">
      <span
        aria-hidden="true"
        className={`inline-block h-1.5 w-1.5 rounded-full ${completed ? 'bg-status-success' : 'bg-status-warning'}`}
      />
      {completed ? 'Completed' : 'Incomplete'}
    </span>
  );
}

export default function App() {
  const [cedarling, setCedarling] = useState<CedarlingClient | null>(null);
  const [currentUser, setCurrentUser] = useState<UserId>('bob');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [newTitle, setNewTitle] = useState('');
  const [permissions, setPermissions] = useState<PermissionMap>({});
  const [sessions, setSessions] = useState<SessionMap>({});
  const [error, setError] = useState('');
  const [busyUser, setBusyUser] = useState<UserId | null>(null);
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [authMode, setAuthMode] = useState<AuthMode>(() => {
    const stored = localStorage.getItem('cedarling.authMode');
    return stored === 'signed-idp' ? stored : 'unsigned';
  });
  const [running, setRunning] = useState(false);

  const signed = authMode === 'signed-idp';
  const authenticated = sessions[currentUser] === true;

  useEffect(() => {
    const stored = localStorage.getItem('cedarling.theme');
    const prefersDark = window.matchMedia(
      '(prefers-color-scheme: dark)',
    ).matches;
    const initial =
      stored === 'dark' || (!stored && prefersDark) ? 'dark' : 'light';
    setTheme(initial);
    document.documentElement.classList.toggle('dark', initial === 'dark');
  }, []);

  useEffect(() => {
    localStorage.setItem('cedarling.authMode', authMode);
  }, [authMode]);

  useEffect(() => {
    let active = true;
    void getRendererCedarling()
      .then((client) => {
        if (active) setCedarling(client);
      })
      .catch((cause: unknown) => {
        if (active)
          setError(
            errorMessage(cause, 'Renderer Cedarling initialization failed'),
          );
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    void Promise.all(
      USERS.map(async ({ id }) => {
        const session =
          await window.electron.ipcRenderer.invoke<SessionResponse>(
            'oidc:session',
            id,
          );
        return [id, session.authenticated] as const;
      }),
    )
      .then((entries) => {
        if (active) setSessions(Object.fromEntries(entries));
      })
      .catch((cause: unknown) => {
        if (active)
          setError(errorMessage(cause, 'Could not read OIDC sessions'));
      });
    return () => {
      active = false;
    };
  }, []);

  const fetchTasks = useCallback(async () => {
    if (signed && !authenticated) {
      setTasks([]);
      setPermissions({});
      setError('');
      return;
    }
    try {
      const data = await window.electron.ipcRenderer.invoke<Task[]>(
        'tasks:list',
        { userId: currentUser, signed },
      );
      setTasks(Array.isArray(data) ? data : []);
      setError('');
    } catch (cause: unknown) {
      setTasks([]);
      setError(errorMessage(cause, 'Failed to load tasks'));
    }
  }, [authenticated, currentUser, signed]);

  useEffect(() => {
    void fetchTasks();
  }, [fetchTasks]);

  useEffect(() => {
    if (tasks.length === 0 || (!signed && !cedarling)) {
      setPermissions({});
      setRunning(false);
      return undefined;
    }

    let active = true;
    setRunning(true);
    const pending = signed
      ? checkSignedPermissions(currentUser, tasks)
      : checkUnsignedPermissions(
          cedarling as CedarlingClient,
          currentUser,
          tasks,
        );
    void pending
      .then((result) => {
        if (active) setPermissions(result);
      })
      .catch((cause: unknown) => {
        if (active) setError(errorMessage(cause, 'Permission check failed'));
      })
      .finally(() => {
        if (active) setRunning(false);
      });
    return () => {
      active = false;
    };
  }, [cedarling, currentUser, signed, tasks]);

  function toggleTheme() {
    const next = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    document.documentElement.classList.toggle('dark', next === 'dark');
    localStorage.setItem('cedarling.theme', next);
  }

  async function handleCreateTask(event: FormEvent) {
    event.preventDefault();
    const title = newTitle.trim();
    if (!title) return;
    try {
      await window.electron.ipcRenderer.invoke('tasks:create', {
        userId: currentUser,
        signed,
        title,
      });
      setNewTitle('');
      await fetchTasks();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Failed to create task'));
    }
  }

  async function handleToggleCompleted(task: Task) {
    try {
      await window.electron.ipcRenderer.invoke('tasks:update', {
        userId: currentUser,
        signed,
        id: task.id,
        completed: !task.completed,
      });
      await fetchTasks();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Failed to update task'));
    }
  }

  async function handleDeleteTask(taskId: string) {
    try {
      await window.electron.ipcRenderer.invoke('tasks:delete', {
        userId: currentUser,
        signed,
        id: taskId,
      });
      await fetchTasks();
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'Failed to delete task'));
    }
  }

  async function handleOidcLogin(userId: UserId) {
    setBusyUser(userId);
    try {
      const session = await window.electron.ipcRenderer.invoke<SessionResponse>(
        'oidc:login',
        userId,
      );
      setSessions((current) => ({
        ...current,
        [userId]: session.authenticated,
      }));
      setCurrentUser(userId);
      setAuthMode('signed-idp');
      setError('');
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'OIDC login failed'));
    } finally {
      setBusyUser(null);
    }
  }

  async function handleOidcLogout(userId: UserId) {
    setBusyUser(userId);
    try {
      await window.electron.ipcRenderer.invoke<SessionResponse>(
        'oidc:logout',
        userId,
      );
      setSessions((current) => ({ ...current, [userId]: false }));
      setError('');
    } catch (cause: unknown) {
      setError(errorMessage(cause, 'OIDC logout failed'));
    } finally {
      setBusyUser(null);
    }
  }

  const currentUserLabel =
    USERS.find(({ id }) => id === currentUser)?.label ?? currentUser;

  return (
    <main className="mx-auto max-w-5xl px-6 py-8 lg:px-10">
      <header className="mb-7 flex items-center justify-between border-b border-default pb-5">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold text-text-primary">TaskApp</h1>
          <span className="rounded-full bg-primary-light px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-text-primary-on-light">
            Electron desktop
          </span>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={toggleTheme}
            className="rounded-md border border-default bg-surface px-3 py-1.5 text-xs font-medium text-text-muted transition hover:bg-surface-hover"
          >
            {theme === 'light' ? 'Dark' : 'Light'}
          </button>
          <label htmlFor="user-select" className="text-sm text-text-muted">
            User:
          </label>
          <select
            id="user-select"
            value={currentUser}
            onChange={(event) => {
              if (isUserId(event.target.value))
                setCurrentUser(event.target.value);
            }}
            className="cursor-pointer rounded-md border border-input bg-surface px-3 py-1.5 text-sm text-text-secondary outline-none transition focus:border-border-primary focus:ring-1 focus:ring-ring-primary"
          >
            {USERS.map((user) => (
              <option key={user.id} value={user.id}>
                {user.label} ({user.note})
              </option>
            ))}
          </select>
        </div>
      </header>

      <section className="mb-7 grid gap-3 rounded-lg border border-default bg-surface-muted p-5 md:grid-cols-[1fr_auto] md:items-center">
        <p className="text-sm leading-relaxed text-text-muted">
          Cedarling runs in both Electron processes: the renderer previews
          direct unsigned decisions, while the main process owns signed tokens
          and enforces every task operation.
        </p>
        <div className="flex gap-2 text-[10px] font-semibold uppercase tracking-wider">
          <span className="rounded bg-primary-light px-2 py-1 text-text-primary-on-light">
            Renderer: WebAssembly
          </span>
          <span className="rounded bg-primary-light px-2 py-1 text-text-primary-on-light">
            Main: Node.js
          </span>
        </div>
      </section>

      <section className="mb-7 rounded-lg border border-default bg-surface p-5">
        <h2 className="mb-3 text-base font-semibold text-text-primary">
          Authorization Settings
        </h2>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-4">
            <label className="flex cursor-pointer items-center gap-2 text-sm text-text-secondary">
              <input
                type="radio"
                name="auth-mode"
                value="unsigned"
                checked={!signed}
                onChange={() => setAuthMode('unsigned')}
                className="text-primary focus:ring-ring-primary"
              />
              <span>Unsigned (Asserted)</span>
            </label>
            <label className="flex cursor-pointer items-center gap-2 text-sm text-text-secondary">
              <input
                type="radio"
                name="auth-mode"
                value="signed-idp"
                checked={signed}
                onChange={() => setAuthMode('signed-idp')}
                className="text-primary focus:ring-ring-primary"
              />
              <span>Signed (Local OIDC IdP)</span>
            </label>
          </div>
          {signed && (
            <div className="text-xs">
              {authenticated ? (
                <span className="font-medium text-status-success">
                  Main-process session ready for {currentUserLabel}
                </span>
              ) : (
                <span className="font-medium text-status-warning">
                  Sign in as {currentUserLabel} to use signed mode
                </span>
              )}
            </div>
          )}
        </div>

        {signed && (
          <div className="mt-4 border-t border-default pt-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-text-muted">
              Authenticate via local OIDC provider (port 9090)
            </div>
            <div className="flex flex-wrap gap-2">
              {USERS.map((user) => {
                const hasSession = sessions[user.id] === true;
                const busy = busyUser === user.id;
                return hasSession ? (
                  <button
                    key={user.id}
                    type="button"
                    disabled={busy}
                    onClick={() => void handleOidcLogout(user.id)}
                    className="rounded border border-default bg-surface-muted px-2.5 py-1 text-xs font-medium text-text-muted transition hover:bg-surface-hover disabled:opacity-50"
                  >
                    {busy ? 'Working…' : `Logout ${user.label}`}
                  </button>
                ) : (
                  <button
                    key={user.id}
                    type="button"
                    disabled={busyUser !== null}
                    onClick={() => void handleOidcLogin(user.id)}
                    className="rounded bg-primary px-2.5 py-1 text-xs font-medium text-text-on-primary transition hover:bg-primary-hover disabled:opacity-50"
                  >
                    {busy ? 'Waiting for browser…' : `Login as ${user.label}`}
                  </button>
                );
              })}
            </div>
            <p className="mt-2 text-xs text-text-disabled">
              The default development IdP accepts the username with any
              password. Tokens remain in Electron&apos;s main process and are
              never exposed to the page.
            </p>
          </div>
        )}
      </section>

      {error && (
        <div
          className="mb-6 rounded-md border border-border-danger bg-danger-subtle px-4 py-3 text-sm text-text-danger"
          role="alert"
        >
          {error}
        </div>
      )}

      <section className="mb-7 rounded-lg border border-default bg-surface p-5">
        <h2 className="mb-4 text-base font-semibold text-text-primary">
          New Task
        </h2>
        <form
          onSubmit={(event) => void handleCreateTask(event)}
          className="flex gap-3"
        >
          <input
            type="text"
            aria-label="Task title"
            placeholder="Enter task title..."
            value={newTitle}
            onChange={(event) => setNewTitle(event.target.value)}
            disabled={signed && !authenticated}
            className="flex-1 rounded-md border border-input bg-surface px-4 py-2 text-sm text-text-secondary outline-none placeholder-placeholder-muted transition focus:border-border-primary focus:ring-1 focus:ring-ring-primary disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={signed && !authenticated}
            className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-text-on-primary transition hover:bg-primary-hover focus:outline-none focus:ring-2 focus:ring-ring-primary focus:ring-offset-2 focus:ring-offset-app disabled:opacity-50"
          >
            Add Task
          </button>
        </form>
      </section>

      <div className="overflow-hidden rounded-lg border border-default bg-surface">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-default bg-surface-muted">
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">
                Task
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">
                Status
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">
                Owner
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-muted">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {tasks.length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-12 text-center text-sm text-text-disabled"
                >
                  {signed && !authenticated
                    ? 'Sign in to load tasks.'
                    : 'No tasks found.'}
                </td>
              </tr>
            ) : (
              tasks.map((task) => {
                const permission = permissions[task.id] ?? {
                  canUpdate: false,
                  canDelete: false,
                };
                const isOwner = task.owner === currentUser;
                return (
                  <tr
                    key={task.id}
                    className="border-b border-light transition last:border-b-0 hover:bg-surface-hover"
                  >
                    <td className="px-4 py-3.5 font-medium text-text-primary">
                      <span
                        className={
                          task.completed
                            ? 'text-text-disabled line-through'
                            : ''
                        }
                      >
                        {task.title}
                      </span>
                    </td>
                    <td className="px-4 py-3.5">
                      <StatusBadge completed={task.completed} />
                    </td>
                    <td className="px-4 py-3.5 text-text-muted">
                      {task.owner}
                      {isOwner && (
                        <span className="ml-2 rounded bg-primary-light px-1.5 py-0.5 text-[10px] font-medium text-text-primary-on-light">
                          you
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-3">
                        {permission.canUpdate ? (
                          <button
                            type="button"
                            title={
                              task.completed
                                ? 'Mark incomplete'
                                : 'Mark complete'
                            }
                            onClick={() => void handleToggleCompleted(task)}
                            className={`rounded px-2.5 py-1 text-xs font-medium transition ${task.completed ? 'border border-input text-text-muted hover:bg-surface-hover' : 'bg-primary text-text-on-primary hover:bg-primary-hover'}`}
                          >
                            {task.completed ? 'Undo' : 'Complete'}
                          </button>
                        ) : (
                          <span className="text-xs text-text-disabled">
                            {running ? '...' : 'update denied'}
                          </span>
                        )}
                        {permission.canDelete ? (
                          <button
                            type="button"
                            onClick={() => void handleDeleteTask(task.id)}
                            className="rounded border border-border-danger-bold px-2.5 py-1 text-xs font-medium text-text-danger transition hover:bg-danger-light"
                          >
                            Delete
                          </button>
                        ) : (
                          <span className="text-xs text-text-disabled">
                            {running ? '...' : 'delete denied'}
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
          {tasks.length > 0 && (
            <tfoot>
              <tr className="bg-surface-muted">
                <td
                  colSpan={3}
                  className="px-4 py-3 text-xs font-medium text-text-muted"
                >
                  {tasks.length} task{tasks.length === 1 ? '' : 's'} total
                </td>
                <td className="px-4 py-3 text-right text-xs text-text-disabled">
                  {currentUserLabel}
                </td>
              </tr>
            </tfoot>
          )}
        </table>
      </div>
    </main>
  );
}
