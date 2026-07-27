import { ipcMain, shell } from 'electron';
import { createServer } from 'node:http';
import type { CedarEntity } from '@janssenproject/cedarling';
import { authorizeAction, type AuthorizeResponse } from './cedarling/authorize';
import { loadPolicyStore, loadTestConfig } from './cedarling/init';
import * as tasks from './tasks';

const OIDC_ISSUER = 'http://localhost:9090';
const CALLBACK_PORT = 9180;
const REDIRECT_URI = `http://127.0.0.1:${CALLBACK_PORT}/callback`;
const OIDC_TIMEOUT_MS = 120_000;
const USERS = new Set(['bob', 'alice', 'charlie']);

type OidcModule = typeof import('openid-client');
type OidcConfiguration = Awaited<
  ReturnType<OidcModule['dynamicClientRegistration']>
>;

type UserContext = {
  readonly userId: string;
  readonly signed: boolean;
};

let configurationPromise: Promise<OidcConfiguration> | undefined;
let loginInProgress = false;
const signedSessions = new Map<string, string>();

function validateUser(userId: string): void {
  if (!USERS.has(userId)) throw new Error('Unknown example user');
}

async function getOidcClient(): Promise<OidcConfiguration> {
  if (!configurationPromise) {
    const pending = (async () => {
      const oidc = await import('openid-client');
      return oidc.dynamicClientRegistration(
        new URL(OIDC_ISSUER),
        {
          client_name: 'Cedarling JS for Electron',
          application_type: 'native',
          redirect_uris: [REDIRECT_URI],
          response_types: ['code'],
          grant_types: ['authorization_code'],
          token_endpoint_auth_method: 'none',
        },
        oidc.None(),
        { execute: [oidc.allowInsecureRequests] },
      );
    })();
    configurationPromise = pending;
    void pending.catch(() => {
      if (configurationPromise === pending) configurationPromise = undefined;
    });
  }
  return configurationPromise;
}

function taskResource(task: tasks.Task): CedarEntity {
  return {
    type: 'TaskApp::Task',
    id: task.id,
    attributes: {
      owner: task.owner,
      title: task.title,
      completed: task.completed,
    },
  };
}

async function authorizeForUser(
  action: string,
  context: UserContext,
  resource: CedarEntity,
): Promise<AuthorizeResponse> {
  validateUser(context.userId);
  const token = context.signed ? signedSessions.get(context.userId) : undefined;
  if (context.signed && !token) return { allowed: false };
  return authorizeAction(action, context.userId, resource, token);
}

async function requireAuthorization(
  action: string,
  context: UserContext,
  resource: CedarEntity,
): Promise<void> {
  const result = await authorizeForUser(action, context, resource);
  if (!result.allowed) throw new Error('Forbidden');
}

ipcMain.handle('tasks:list', async (_event, context: UserContext) => {
  const listedTasks = tasks.getAll();
  await Promise.all(
    listedTasks.map((task) =>
      requireAuthorization('ViewTask', context, taskResource(task)),
    ),
  );
  return listedTasks;
});

ipcMain.handle(
  'tasks:create',
  async (_event, data: UserContext & { readonly title: string }) => {
    const title = data.title.trim();
    if (!title) throw new Error('Task title is required');
    await requireAuthorization('CreateTask', data, {
      type: 'TaskApp::Task',
      id: 'new-task',
      attributes: { owner: data.userId, title, completed: false },
    });
    return tasks.create(title, data.userId);
  },
);

ipcMain.handle(
  'tasks:update',
  async (
    _event,
    data: UserContext & {
      readonly id: string;
      readonly completed?: boolean;
      readonly title?: string;
    },
  ) => {
    const task = tasks.findById(data.id);
    if (!task) throw new Error('Task not found');
    await requireAuthorization('UpdateTask', data, taskResource(task));
    return tasks.update(data.id, {
      completed: data.completed,
      title: data.title,
    });
  },
);

ipcMain.handle(
  'tasks:delete',
  async (_event, data: UserContext & { readonly id: string }) => {
    const task = tasks.findById(data.id);
    if (!task) throw new Error('Task not found');
    await requireAuthorization('DeleteTask', data, taskResource(task));
    return { removed: tasks.remove(data.id) };
  },
);

ipcMain.handle(
  'authorize-signed',
  async (
    _event,
    request: {
      readonly action: string;
      readonly userId: string;
      readonly resource: CedarEntity;
    },
  ): Promise<AuthorizeResponse> =>
    authorizeForUser(
      request.action,
      { userId: request.userId, signed: true },
      request.resource,
    ),
);

ipcMain.handle('config:policy-store', () => loadPolicyStore());
ipcMain.handle('config:test-config', () => loadTestConfig());

ipcMain.handle('oidc:session', (_event, userId: string) => {
  validateUser(userId);
  return { authenticated: signedSessions.has(userId) };
});

ipcMain.handle('oidc:login', async (_event, userId: string) => {
  validateUser(userId);
  if (loginInProgress) throw new Error('Another OIDC login is already running');
  loginInProgress = true;

  try {
    const oidc = await import('openid-client');
    const config = await getOidcClient();
    const codeVerifier = oidc.randomPKCECodeVerifier();
    const codeChallenge = await oidc.calculatePKCECodeChallenge(codeVerifier);
    const state = oidc.randomState();
    const nonce = oidc.randomNonce();
    const authorizationUrl = oidc.buildAuthorizationUrl(config, {
      redirect_uri: REDIRECT_URI,
      scope: 'openid profile role',
      resource: OIDC_ISSUER,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
      nonce,
      state,
      response_type: 'code',
    });

    const token = await new Promise<string>((resolve, reject) => {
      let settled = false;
      const server = createServer(async (request, response) => {
        const callbackUrl = new URL(request.url ?? '/', REDIRECT_URI);
        if (callbackUrl.pathname !== '/callback') {
          response.writeHead(404).end('Not found');
          return;
        }

        try {
          const tokenSet = await oidc.authorizationCodeGrant(
            config,
            callbackUrl,
            {
              expectedNonce: nonce,
              expectedState: state,
              pkceCodeVerifier: codeVerifier,
            },
          );
          if (!tokenSet.id_token) throw new Error('No ID token was returned');

          let authorizationToken = tokenSet.id_token;
          if (tokenSet.access_token) {
            const userinfo = await fetch(`${OIDC_ISSUER}/me`, {
              headers: { Authorization: `Bearer ${tokenSet.access_token}` },
            });
            if (userinfo.ok) {
              const candidate = await userinfo.text();
              if (candidate.split('.').length === 3) {
                authorizationToken = candidate;
              }
            }
          }

          response.writeHead(200, {
            'Content-Security-Policy':
              "default-src 'none'; style-src 'unsafe-inline'",
            'Content-Type': 'text/html; charset=utf-8',
          });
          response.end(
            '<!doctype html><title>Signed in</title><p>Sign-in complete. Return to Cedarling JS for Electron.</p>',
          );
          if (!settled) {
            settled = true;
            resolve(authorizationToken);
          }
          server.close();
        } catch (error: unknown) {
          response.writeHead(400).end('OIDC callback validation failed');
          if (!settled) {
            settled = true;
            reject(error);
          }
          server.close();
        }
      });

      const timeout = setTimeout(() => {
        if (!settled) {
          settled = true;
          reject(new Error('OIDC login timed out'));
        }
        server.close();
      }, OIDC_TIMEOUT_MS);
      server.on('close', () => clearTimeout(timeout));
      server.on('error', (error) => {
        if (!settled) {
          settled = true;
          reject(error);
        }
      });
      server.listen(CALLBACK_PORT, '127.0.0.1', () => {
        void shell.openExternal(authorizationUrl.href).catch((error) => {
          if (!settled) {
            settled = true;
            reject(error);
          }
          server.close();
        });
      });
    });

    signedSessions.set(userId, token);
    return { authenticated: true };
  } finally {
    loginInProgress = false;
  }
});

ipcMain.handle('oidc:logout', (_event, userId: string) => {
  validateUser(userId);
  signedSessions.delete(userId);
  return { authenticated: false };
});
