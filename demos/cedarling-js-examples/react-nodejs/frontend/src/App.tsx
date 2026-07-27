import { useEffect, useState } from 'react';
import { initCedarling } from './cedarling/init.ts';
import { checkUnsignedPermissions } from './cedarling/exercise-unsigned.ts';
import { checkSignedPermissions } from './cedarling/exercise-signed.ts';

const BACKEND_URL = (import.meta as any).env?.VITE_BACKEND_URL || 'http://localhost:8080';

const USERS = [
  { id: 'bob', label: 'Bob', note: 'owner of "Buy groceries"' },
  { id: 'alice', label: 'Alice', note: 'owner of "Schedule meeting with CEO"' },
  { id: 'charlie', label: 'Charlie', note: 'guest user' },
];

type Task = {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
};

type PermMap = Record<string, { canUpdate: boolean; canDelete: boolean }>;

function StatusBadge({ completed }: { completed: boolean }) {
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
  const [cedarling, setCedarling] = useState<any>(null);
  const [currentUser, setCurrentUser] = useState('bob');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [newTitle, setNewTitle] = useState('');
  const [permissions, setPermissions] = useState<PermMap>({});
  const [errorMessage, setErrorMessage] = useState('');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [authMode, setAuthMode] = useState<'unsigned' | 'signed-idp'>(() => {
    const match = document.cookie.match(/(?:^|;\s*)authMode=([^;]*)/);
    return match && (match[1] === 'unsigned' || match[1] === 'signed-idp') ? match[1] : 'unsigned';
  });
  const [running, setRunning] = useState(false);

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
    document.cookie = `authMode=${authMode}; path=/; max-age=86400; SameSite=Lax`;
  }, [authMode]);

  useEffect(() => {
    const hash = window.location.hash;
    if (hash) {
      const params = new URLSearchParams(hash.substring(1));
      const idToken = params.get('id_token');
      const accessToken = params.get('access_token');
      if (idToken) {
        try {
          const payloadBase64 = idToken.split('.')[1];
          const payload = JSON.parse(atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/')));
          const username = payload.sub || 'bob';

          const handleTokenSave = async () => {
            let tokenToStore = idToken;
            if (accessToken) {
              try {
                const meRes = await fetch('http://localhost:9090/me', {
                  headers: { 'Authorization': `Bearer ${accessToken}` },
                });
                if (meRes.ok) {
                  const userinfoToken = await meRes.text();
                  if (userinfoToken && userinfoToken.split('.').length === 3) {
                    tokenToStore = userinfoToken;
                  }
                }
              } catch (meErr) {
                console.error('Failed to fetch Userinfo JWT, falling back to ID Token:', meErr);
              }
            }
            localStorage.setItem(`token_${username}`, tokenToStore);
            setAuthMode('signed-idp');
            setCurrentUser(username);
            setErrorMessage('');
            window.history.replaceState(null, '', window.location.pathname);
          };
          handleTokenSave();
        } catch (err) {
          console.error('Failed to parse ID Token from hash:', err);
          setErrorMessage('Failed to parse OIDC token.');
        }
      }
    }
  }, []);

  useEffect(() => {
    async function registerClient() {
      if (localStorage.getItem('registered_client_id')) return;
      try {
        const discRes = await fetch('http://localhost:9090/.well-known/openid-configuration');
        if (!discRes.ok) throw new Error('Failed to fetch OIDC discovery document');
        const discovery = await discRes.json();
        const regEndpoint = discovery.registration_endpoint;
        if (!regEndpoint) throw new Error('OIDC provider does not support dynamic client registration');

        const regRes = await fetch(regEndpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            client_name: 'TaskApp React Client',
            application_type: 'native',
            redirect_uris: ['http://localhost:3000/callback'],
            response_types: ['id_token token', 'code'],
            grant_types: ['implicit', 'authorization_code'],
            token_endpoint_auth_method: 'none',
            'urn:custom:client:allowed-cors-origins': ['http://localhost:3000'],
          }),
        });

        if (!regRes.ok) {
          const errMsg = await regRes.text();
          throw new Error(`Registration failed: ${errMsg}`);
        }

        const clientData = await regRes.json();
        if (clientData.client_id) {
          localStorage.setItem('registered_client_id', clientData.client_id);
          console.log('Successfully dynamically registered client:', clientData.client_id);
        }
      } catch (err) {
        console.error('Dynamic Client Registration failed:', err);
      }
    }
    registerClient();
  }, []);

  useEffect(() => {
    initCedarling()
      .then(setCedarling)
      .catch((err) => {
        console.error('Failed to initialize Cedarling:', err);
        setErrorMessage('Failed to initialize Cedarling');
      });
  }, []);

  function getActiveToken(username: string) {
    if (authMode === 'signed-idp') {
      return localStorage.getItem(`token_${username}`) || '';
    }
    return '';
  }

  function getRequestHeaders() {
    const headers: Record<string, string> = { 'x-user-id': currentUser };
    const token = getActiveToken(currentUser);
    if (authMode !== 'unsigned' && token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }

  async function fetchTasks() {
    try {
      const res = await fetch(`${BACKEND_URL}/tasks`, { headers: getRequestHeaders() });
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        setErrorMessage(`${errData.error || 'Failed to fetch tasks'}`);
        setTasks([]);
        return;
      }
      const data = await res.json();
      setTasks(Array.isArray(data) ? data : []);
      setErrorMessage('');
    } catch {
      setErrorMessage('Failed to connect to backend.');
    }
  }

  useEffect(() => {
    fetchTasks();
  }, [currentUser, authMode]);

  useEffect(() => {
    if (!cedarling || tasks.length === 0) return;
    setRunning(true);

    async function checkPermissions() {
      const token = getActiveToken(currentUser);
      let perms: PermMap;

      if (authMode === 'unsigned') {
        perms = await checkUnsignedPermissions(cedarling, currentUser, tasks);
      } else {
        if (!token) {
          perms = Object.fromEntries(tasks.map((t) => [t.id, { canUpdate: false, canDelete: false }]));
        } else {
          perms = await checkSignedPermissions(cedarling, currentUser, tasks, token);
        }
      }

      setPermissions(perms);
      setRunning(false);
    }
    checkPermissions();
  }, [cedarling, tasks, currentUser, authMode]);

  async function handleCreateTask(e: React.FormEvent) {
    e.preventDefault();
    if (!newTitle.trim()) return;
    try {
      const res = await fetch(`${BACKEND_URL}/tasks`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getRequestHeaders() },
        body: JSON.stringify({ title: newTitle }),
      });
      if (res.ok) {
        setNewTitle('');
        fetchTasks();
      } else {
        const err = await res.json();
        setErrorMessage(err.error || 'Failed to create task');
      }
    } catch {
      setErrorMessage('Connection error');
    }
  }

  async function handleToggleCompleted(task: Task) {
    try {
      const res = await fetch(`${BACKEND_URL}/tasks/${task.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getRequestHeaders() },
        body: JSON.stringify({ completed: !task.completed }),
      });
      if (res.ok) {
        fetchTasks();
      } else {
        const err = await res.json();
        setErrorMessage(`Failed to update: ${err.error}`);
      }
    } catch {
      setErrorMessage('Connection error');
    }
  }

  async function handleDeleteTask(taskId: string) {
    try {
      const res = await fetch(`${BACKEND_URL}/tasks/${taskId}`, {
        method: 'DELETE',
        headers: getRequestHeaders(),
      });
      if (res.ok) {
        fetchTasks();
      } else {
        const err = await res.json();
        setErrorMessage(`Failed to delete: ${err.error}`);
      }
    } catch {
      setErrorMessage('Connection error');
    }
  }

  function handleOidcLogin(username: string) {
    const clientId = localStorage.getItem('registered_client_id');
    if (!clientId) {
      setErrorMessage('Dynamic Client Registration in progress or failed. Please refresh the page.');
      return;
    }
    const authUrl = `http://localhost:9090/auth` +
      `?client_id=${clientId}` +
      `&response_type=id_token token` +
      `&scope=openid profile role` +
      `&resource=http://localhost:9090` +
      `&redirect_uri=http://localhost:3000/callback` +
      `&nonce=${Math.random().toString(36).substring(2)}` +
      `&state=${username}`;
    window.location.href = authUrl;
  }

  function handleOidcLogout(username: string) {
    localStorage.removeItem(`token_${username}`);
    fetchTasks();
  }

  const currentUserLabel = USERS.find((u) => u.id === currentUser)?.label ?? currentUser;

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="mb-8 flex items-center justify-between border-b border-default pb-5">
        <h1 className="text-2xl font-bold text-text-primary">TaskApp</h1>
        <div className="flex items-center gap-3">
          <button
            onClick={toggleTheme}
            className="rounded-md border border-default bg-surface px-3 py-1.5 text-xs font-medium text-text-muted transition hover:bg-surface-hover"
          >
            {theme === 'light' ? 'Dark' : 'Light'}
          </button>
          <label htmlFor="user-select" className="text-sm text-text-muted">User:</label>
          <select
            id="user-select"
            value={currentUser}
            onChange={(e) => setCurrentUser(e.target.value)}
            className="cursor-pointer rounded-md border border-input bg-surface px-3 py-1.5 text-sm text-text-secondary outline-none transition focus:border-border-primary focus:ring-1 focus:ring-ring-primary"
          >
            {USERS.map((u) => (
              <option key={u.id} value={u.id}>{u.label} ({u.note})</option>
            ))}
          </select>
        </div>
      </header>

      <div className="mb-8 rounded-lg border border-default bg-surface-muted p-5">
        <p className="text-sm leading-relaxed text-text-muted">
          Cedarling enforces fine-grained authorization. All users can view and create tasks, but only the task owner can update or delete their own tasks.
        </p>
      </div>

      <div className="mb-8 rounded-lg border border-default bg-surface p-5">
        <h2 className="mb-3 text-base font-semibold text-text-primary">Authorization Settings</h2>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-4">
            <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
              <input type="radio" name="auth-mode" value="unsigned" checked={authMode === 'unsigned'} onChange={() => setAuthMode('unsigned')} className="text-primary focus:ring-primary" />
              <span>Unsigned (Asserted)</span>
            </label>
            <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
              <input type="radio" name="auth-mode" value="signed-idp" checked={authMode === 'signed-idp'} onChange={() => setAuthMode('signed-idp')} className="text-primary focus:ring-primary" />
              <span>Signed (Local OIDC IdP)</span>
            </label>
          </div>
          {authMode !== 'unsigned' && (
            <div className="text-xs">
              {getActiveToken(currentUser) ? (
                <span className="text-status-success font-medium">Token Loaded for {currentUserLabel}</span>
              ) : (
                <span className="text-status-warning font-medium">No Token Loaded for {currentUserLabel}</span>
              )}
            </div>
          )}
        </div>

        {authMode === 'signed-idp' && (
          <div className="mt-4 border-t border-default pt-4">
            <div className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">Authenticate via Local OIDC Provider (Port 9090)</div>
            <div className="flex flex-wrap gap-2">
              {USERS.map((u) => {
                const hasToken = !!localStorage.getItem(`token_${u.id}`);
                return (
                  <div key={u.id} className="flex items-center gap-1.5">
                    {hasToken ? (
                      <button onClick={() => handleOidcLogout(u.id)} className="rounded bg-surface-muted border border-default px-2.5 py-1 text-xs font-medium text-text-muted hover:bg-surface-hover transition">Logout {u.label}</button>
                    ) : (
                      <button onClick={() => handleOidcLogin(u.id)} className="rounded bg-primary px-2.5 py-1 text-xs font-medium text-white hover:bg-primary-hover transition">Login as {u.label}</button>
                    )}
                  </div>
                );
              })}
            </div>
            <p className="mt-2 text-xs text-text-disabled">Sign in with the username and any password.</p>
          </div>
        )}
      </div>

      {errorMessage && (
        <div className="mb-6 rounded-md border border-border-danger bg-danger-subtle px-4 py-3 text-sm text-text-danger">{errorMessage}</div>
      )}

      <div className="mb-8 rounded-lg border border-default bg-surface p-5">
        <h2 className="mb-4 text-base font-semibold text-text-primary">New Task</h2>
        <form onSubmit={handleCreateTask} className="flex gap-3">
          <input type="text" placeholder="Enter task title..." value={newTitle} onChange={(e) => setNewTitle(e.target.value)} className="flex-1 rounded-md border border-input bg-surface px-4 py-2 text-sm text-text-secondary outline-none placeholder-placeholder-muted transition focus:border-border-primary focus:ring-1 focus:ring-ring-primary" />
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
              <tr>
                <td colSpan={4} className="px-4 py-12 text-center text-sm text-text-disabled">No tasks found.</td>
              </tr>
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
                          <button onClick={() => handleToggleCompleted(task)} className={`rounded px-2.5 py-1 text-xs font-medium transition ${task.completed ? 'border border-input text-text-muted hover:bg-surface-hover' : 'bg-primary text-white hover:bg-primary-hover'}`} title={task.completed ? 'Mark incomplete' : 'Mark complete'}>{task.completed ? 'Undo' : 'Complete'}</button>
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
                <td className="px-4 py-3 text-right text-xs text-text-disabled">{currentUserLabel}</td>
              </tr>
            </tfoot>
          )}
        </table>
      </div>
    </div>
  );
}
