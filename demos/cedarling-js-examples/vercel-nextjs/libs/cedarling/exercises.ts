import type { CedarlingClient } from '@janssenproject/cedarling';
import { createCedarlingClient } from './init';

const IDP_ISSUER = process.env.OIDC_ISSUER ?? 'http://localhost:9090';
type SdkResult<T> =
  | { readonly ok: true; readonly value: T }
  | { readonly ok: false; readonly error: unknown };

function unwrap<T>(result: SdkResult<T>, operation: string): T {
  if (!result.ok) {
    throw new Error(`Cedarling exercise failed: ${operation}`);
  }
  return result.value;
}

function exerciseResource() {
  return {
    type: 'TaskApp::Task',
    id: 'exercise-task',
    attributes: {
      owner: 'bob',
      title: 'Exercise Cedarling APIs',
      completed: false,
    },
  };
}

async function runAuthorizationExercises(
  client: CedarlingClient,
  signedToken?: string,
) {
  const unsigned = unwrap(
    await client.authorizeUnsigned({
      principal: { type: 'TaskApp::User', id: 'bob' },
      action: 'TaskApp::Action::"UpdateTask"',
      resource: exerciseResource(),
      context: { userId: 'bob' },
    }),
    'authorizeUnsigned',
  );

  let authorizeMultiIssuer: boolean | null = null;
  if (signedToken) {
    const signed = unwrap(
      await client.authorizeMultiIssuer({
        tokens: [
          { mapping: 'LocalMockIdP::Userinfo_token', payload: signedToken },
        ],
        action: 'TaskApp::Action::"UpdateTask"',
        resource: exerciseResource(),
        context: {},
      }),
      'authorizeMultiIssuer',
    );
    authorizeMultiIssuer = signed.decision;
  }

  return {
    authorizeUnsigned: unsigned.decision,
    authorizeMultiIssuer,
  };
}

async function runContextExercises(client: CedarlingClient) {
  unwrap(
    await client.context.set('exercise-user', 'bob', { ttlSeconds: 300 }),
    'context.set',
  );
  const value = unwrap(await client.context.get('exercise-user'), 'context.get');
  unwrap(await client.context.getEntry('exercise-user'), 'context.getEntry');
  unwrap(await client.context.entries(), 'context.entries');
  unwrap(await client.context.stats(), 'context.stats');
  unwrap(await client.context.delete('exercise-user'), 'context.delete');
  unwrap(await client.context.clear(), 'context.clear');
  if (value !== 'bob') throw new Error('Cedarling exercise failed: context value');

  return {
    set: true,
    get: true,
    getEntry: true,
    entries: true,
    stats: true,
    delete: true,
    clear: true,
  };
}

async function runIssuerExercises(client: CedarlingClient) {
  return {
    byId: unwrap(
      await client.issuers.isLoaded({ id: 'LocalMockIdP' }),
      'issuers.isLoaded(id)',
    ),
    byIssuer: unwrap(
      await client.issuers.isLoaded({ iss: IDP_ISSUER }),
      'issuers.isLoaded(iss)',
    ),
  };
}

async function runLogExercises(client: CedarlingClient) {
  unwrap(await client.logs.ids(), 'logs.ids');
  unwrap(await client.logs.find(), 'logs.find');
  unwrap(await client.logs.find({ tag: 'decision' }), 'logs.find(decision)');
  unwrap(await client.logs.drain(), 'logs.drain');
  return { ids: true, find: true, findDecision: true, drain: true };
}

export async function runFullSdkExercises(signedToken?: string) {
  const client = await createCedarlingClient({
    logging: { type: 'memory', level: 'trace', maxItems: 500 },
    contextStore: {
      maxEntries: 100,
      maxEntrySizeBytes: 4096,
      defaultTtlSeconds: 3600,
      metrics: true,
    },
  });
  let closed = false;
  try {
    const authorization = await runAuthorizationExercises(client, signedToken);
    const context = await runContextExercises(client);
    const issuers = await runIssuerExercises(client);
    const logs = await runLogExercises(client);
    unwrap(await client.close(), 'close');
    closed = true;
    return {
      ...authorization,
      context,
      issuers,
      logs,
      lifecycle: { close: true },
    };
  } finally {
    if (!closed) await client.close();
  }
}
