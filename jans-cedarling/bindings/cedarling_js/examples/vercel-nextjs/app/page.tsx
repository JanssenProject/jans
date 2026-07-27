'use client';

import { useEffect, useState, useCallback } from 'react';

const USERS = [
  { id: 'bob', label: 'Bob', note: 'owner of "Buy groceries"' },
  { id: 'alice', label: 'Alice', note: 'owner of "Schedule meeting with CEO"' },
  { id: 'charlie', label: 'Charlie', note: 'guest user' },
];

type Task = { id: string; title: string; completed: boolean; owner: string };
type PermMap = Record<string, { canUpdate: boolean; canDelete: boolean }>;
type OidcSession =
  | { authenticated: false }
  | { authenticated: true; userId: string };

function StatusBadge({ completed }: { completed: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-default px-2.5 py-0.5 text-xs font-medium text-text-secondary">
      <span aria-hidden="true" className={`inline-block h-1.5 w-1.5 rounded-full ${completed ? 'bg-status-success' : 'bg-status-warning'}`} />
      {completed ? 'Completed' : 'Incomplete'}
    </span>
  );
}

export default function App() {
  const [currentUser, setCurrentUser] = useState('bob');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [newTitle, setNewTitle] = useState('');
  const [permissions, setPermissions] = useState<PermMap>({});
  const [errorMessage, setErrorMessage] = useState('');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [authMode, setAuthMode] = useState<'unsigned' | 'signed-idp'>('unsigned');
  const [authModeReady, setAuthModeReady] = useState(false);
  const [oidcSession, setOidcSession] = useState<OidcSession>({ authenticated: false });
  const [running, setRunning] = useState(false);

  const refreshSession = useCallback(async (): Promise<OidcSession> => {
    try {
      const response = await fetch('/api/oidc/session', { cache: 'no-store' });
      const session = (await response.json()) as OidcSession;
      const normalized =
        session.authenticated && typeof session.userId === 'string'
          ? session
          : { authenticated: false as const };
      setOidcSession(normalized);
      if (normalized.authenticated) setCurrentUser(normalized.userId);
      return normalized;
    } catch {
      const empty = { authenticated: false as const };
      setOidcSession(empty);
      return empty;
    }
  }, []);

  useEffect(() => {
    const m = document.cookie.match(/(?:^|;\s*)authMode=([^;]*)/);
    setAuthMode(m && m[1] === 'signed-idp' ? 'signed-idp' : 'unsigned');
    const query = new URLSearchParams(window.location.search);
    if (query.has('oidc_error')) {
      setErrorMessage('OIDC authentication failed. Please try again.');
      window.history.replaceState(null, '', window.location.pathname);
    }
    void refreshSession().finally(() => setAuthModeReady(true));
  }, [refreshSession]);

  useEffect(() => {
    const stored = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initial = stored === 'dark' || (!stored && prefersDark) ? 'dark' : 'light';
    setTheme(initial);
    document.documentElement.classList.toggle('dark', initial === 'dark');
  }, []);

  function toggleTheme() {
    const next = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    document.documentElement.classList.toggle('dark', next === 'dark');
    localStorage.setItem('theme', next);
  }

  useEffect(() => {
    if (!authModeReady) return;
    document.cookie = `authMode=${authMode}; path=/; max-age=86400; SameSite=Lax`;
  }, [authMode, authModeReady]);


  const getRequestHeaders = useCallback((): Record<string, string> => {
    return authMode === 'unsigned' ? { 'x-user-id': currentUser } : {};
  }, [currentUser, authMode]);

  const fetchTasks = useCallback(async () => {
    if (!authModeReady) return;
    try {
      const res = await fetch('/api/tasks', { headers: getRequestHeaders() });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setErrorMessage(err.error || 'Failed to fetch tasks');
        setTasks([]); return;
      }
      setTasks(await res.json());
      setErrorMessage('');
    } catch { setErrorMessage('Failed to connect to backend.'); }
  }, [authModeReady, getRequestHeaders]);

  useEffect(() => { fetchTasks(); }, [fetchTasks]);

  async function checkPermission(action: string, task: Task): Promise<boolean> {
    const params = new URLSearchParams({
      action, taskId: task.id,
      owner: task.owner, title: task.title, completed: String(task.completed),
    });
    const res = await fetch(`/api/check?${params}`, { headers: getRequestHeaders() });
    if (!res.ok) return false;
    const data = await res.json();
    return data.allowed === true;
  }

  useEffect(() => {
    if (!authModeReady || tasks.length === 0) return;
    setRunning(true);
    (async () => {
      const pm: PermMap = {};
      for (const task of tasks) {
        const [canUpdate, canDelete] = await Promise.all([
          checkPermission('UpdateTask', task),
          checkPermission('DeleteTask', task),
        ]);
        pm[task.id] = { canUpdate, canDelete };
      }
      setPermissions(pm);
      setRunning(false);
    })();
  }, [tasks, currentUser, authMode]);

  async function handleCreateTask(e: React.FormEvent) {
    e.preventDefault();
    if (!newTitle.trim()) return;
    try {
      const res = await fetch('/api/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getRequestHeaders() },
        body: JSON.stringify({ title: newTitle }),
      });
      if (res.ok) { setNewTitle(''); fetchTasks(); }
      else { const err = await res.json(); setErrorMessage(err.error || 'Failed to create task'); }
    } catch { setErrorMessage('Connection error'); }
  }

  async function handleToggleCompleted(task: Task) {
    try {
      const res = await fetch(`/api/tasks/${task.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getRequestHeaders() },
        body: JSON.stringify({ completed: !task.completed }),
      });
      if (res.ok) fetchTasks();
      else { const err = await res.json(); setErrorMessage(err.error || 'Failed to update'); }
    } catch { setErrorMessage('Connection error'); }
  }

  async function handleDeleteTask(taskId: string) {
    try {
      const res = await fetch(`/api/tasks/${taskId}`, { method: 'DELETE', headers: getRequestHeaders() });
      if (res.ok) fetchTasks();
      else { const err = await res.json(); setErrorMessage(err.error || 'Failed to delete'); }
    } catch { setErrorMessage('Connection error'); }
  }

  function handleOidcLogin(username: string) {
    window.location.assign(`/api/oidc/start?user=${encodeURIComponent(username)}`);
  }

  function handleOidcLogout() {
    window.location.assign('/api/oidc/logout');
  }

  const userLabel = USERS.find((u) => u.id === currentUser)?.label ?? currentUser;

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="mb-8 flex items-center justify-between border-b border-default pb-5">
        <h1 className="text-2xl font-bold text-text-primary">TaskApp</h1>
        <div className="flex items-center gap-3">
          <button onClick={toggleTheme} className="rounded-md border border-default bg-surface px-3 py-1.5 text-xs font-medium text-text-muted transition hover:bg-surface-hover">
            {theme === 'light' ? 'Dark' : 'Light'}
          </button>
          <label htmlFor="user-select" className="text-sm text-text-muted">User:</label>
          <select id="user-select" value={currentUser} onChange={(e) => setCurrentUser(e.target.value)}
            disabled={authMode === 'signed-idp' && oidcSession.authenticated}
            className="cursor-pointer rounded-md border border-input bg-surface px-3 py-1.5 text-sm text-text-secondary outline-none transition focus:border-border-primary focus:ring-1 focus:ring-ring-primary">
            {USERS.map((u) => <option key={u.id} value={u.id}>{u.label} ({u.note})</option>)}
          </select>
        </div>
      </header>

      <div className="mb-8 rounded-lg border border-default bg-surface-muted p-5">
        <p className="text-sm leading-relaxed text-text-muted">Cedarling enforces fine-grained authorization. All users can view and create tasks, but only the task owner can update or delete their own tasks.</p>
      </div>

      <div className="mb-8 rounded-lg border border-default bg-surface p-5">
        <h2 className="mb-3 text-base font-semibold text-text-primary">Authorization Settings</h2>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-4">
            {(['unsigned', 'signed-idp'] as const).map((mode) => (
              <label key={mode} className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                <input type="radio" name="auth-mode" value={mode} checked={authMode === mode} onChange={() => setAuthMode(mode)} className="text-primary focus:ring-primary" />
                <span>{mode === 'unsigned' ? 'Unsigned (Asserted)' : 'Signed (Local OIDC IdP)'}</span>
              </label>
            ))}
          </div>
          {authMode !== 'unsigned' && (
            <div className="text-xs">
              {oidcSession.authenticated ? (
                <span className="text-status-success font-medium">
                  Authenticated as {USERS.find((u) => u.id === oidcSession.userId)?.label ?? oidcSession.userId}
                </span>
              ) : (
                <span className="text-status-warning font-medium">
                  No authenticated OIDC session
                </span>
              )}
            </div>
          )}
        </div>
        {authMode === 'signed-idp' && (
          <div className="mt-4 border-t border-default pt-4">
            <div className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">Authenticate via Local OIDC Provider (Port 9090)</div>
            <div className="flex flex-wrap gap-2">
              {USERS.map((u) => (
                <div key={u.id} className="flex items-center gap-1.5">
                  {oidcSession.authenticated && oidcSession.userId === u.id ? (
                    <button onClick={handleOidcLogout} className="rounded bg-surface-muted border border-default px-2.5 py-1 text-xs font-medium text-text-muted hover:bg-surface-hover transition">Logout {u.label}</button>
                  ) : (
                    <button onClick={() => handleOidcLogin(u.id)} className="rounded bg-primary px-2.5 py-1 text-xs font-medium text-white hover:bg-primary-hover transition">Login as {u.label}</button>
                  )}
                </div>
              ))}
            </div>
            <p className="mt-2 text-xs text-text-disabled">
              Authorization Code + PKCE. Tokens remain in HttpOnly cookies. Sign in with the username and any password.
            </p>
          </div>
        )}
      </div>

      {errorMessage && (
        <div className="mb-6 rounded-md border border-border-danger bg-danger-subtle px-4 py-3 text-sm text-text-danger">{errorMessage}</div>
      )}

      <div className="mb-8 rounded-lg border border-default bg-surface p-5">
        <h2 className="mb-4 text-base font-semibold text-text-primary">New Task</h2>
        <form onSubmit={handleCreateTask} className="flex gap-3">
          <input type="text" placeholder="Enter task title..." value={newTitle} onChange={(e) => setNewTitle(e.target.value)}
            className="flex-1 rounded-md border border-input bg-surface px-4 py-2 text-sm text-text-secondary outline-none placeholder-placeholder-muted transition focus:border-border-primary focus:ring-1 focus:ring-ring-primary" />
          <button type="submit" className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-white transition hover:bg-primary-hover focus:outline-none focus:ring-2 focus:ring-ring-primary focus:ring-offset-2 focus:ring-offset-app">Add Task</button>
        </form>
      </div>

      <div className="overflow-hidden rounded-lg border border-default">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-default bg-surface-muted">
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Task</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">Owner</th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-text-muted">Actions</th>
            </tr>
          </thead>
          <tbody>
            {tasks.length === 0 ? (
              <tr><td colSpan={4} className="px-4 py-12 text-center text-sm text-text-disabled">No tasks found.</td></tr>
            ) : (
              tasks.map((task) => {
                const perm = permissions[task.id] ?? { canUpdate: false, canDelete: false };
                const isOwner = task.owner === currentUser;
                return (
                  <tr key={task.id} className="border-b border-light transition last:border-b-0 hover:bg-surface-hover">
                    <td className="px-4 py-3.5 font-medium text-text-primary">
                      <span className={task.completed ? 'text-text-disabled line-through' : ''}>{task.title}</span>
                    </td>
                    <td className="px-4 py-3.5"><StatusBadge completed={task.completed} /></td>
                    <td className="px-4 py-3.5 text-text-muted">
                      {task.owner}
                      {isOwner && <span className="ml-2 rounded bg-primary-light px-1.5 py-0.5 text-[10px] font-medium text-text-primary-on-light">you</span>}
                    </td>
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-3">
                        {perm.canUpdate ? (
                          <button onClick={() => handleToggleCompleted(task)}
                            className={`rounded px-2.5 py-1 text-xs font-medium transition ${task.completed ? 'border border-input text-text-muted hover:bg-surface-hover' : 'bg-primary text-white hover:bg-primary-hover'}`}>
                            {task.completed ? 'Undo' : 'Complete'}
                          </button>
                        ) : (
                          <span className="text-xs text-text-disabled">{running ? '...' : 'update denied'}</span>
                        )}
                        {perm.canDelete ? (
                          <button onClick={() => handleDeleteTask(task.id)} className="rounded border border-border-danger-bold px-2.5 py-1 text-xs font-medium text-text-danger transition hover:bg-danger-light">Delete</button>
                        ) : (
                          <span className="text-xs text-text-disabled">{running ? '...' : 'delete denied'}</span>
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
                <td colSpan={3} className="px-4 py-3 text-xs font-medium text-text-muted">{tasks.length} task{tasks.length !== 1 ? 's' : ''} total</td>
                <td className="px-4 py-3 text-right text-xs text-text-disabled">{userLabel}</td>
              </tr>
            </tfoot>
          )}
        </table>
      </div>
    </div>
  );
}
