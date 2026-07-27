import {
  createCedarling,
  type CedarlingBaseOptions,
  type CedarlingClient,
  type PolicyStoreDocument,
} from '@janssenproject/cedarling';

interface TestScenario {
  readonly name: string;
  readonly override?: Partial<CedarlingBaseOptions>;
}

interface TestConfig {
  readonly activeScenario: string;
  readonly cedarling: CedarlingBaseOptions;
  readonly scenarios?: readonly TestScenario[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function deepMerge<T>(base: T, override: unknown): T {
  if (!isRecord(base) || !isRecord(override)) return override as T;
  const merged: Record<string, unknown> = { ...base };
  for (const [key, value] of Object.entries(override)) {
    merged[key] =
      isRecord(merged[key]) && isRecord(value)
        ? deepMerge(merged[key], value)
        : value;
  }
  return merged as T;
}

async function initializeRendererCedarling(): Promise<CedarlingClient> {
  const [policyStore, testConfig] = await Promise.all([
    window.electron.ipcRenderer.invoke<PolicyStoreDocument>(
      'config:policy-store',
    ),
    window.electron.ipcRenderer.invoke<TestConfig>('config:test-config'),
  ]);
  const scenario = testConfig.scenarios?.find(
    ({ name }) => name === testConfig.activeScenario,
  );
  if (!scenario) {
    throw new Error(
      `Unknown Cedarling fixture scenario: ${testConfig.activeScenario}`,
    );
  }
  const config = deepMerge(testConfig.cedarling, scenario.override ?? {});
  const created = await createCedarling({
    ...config,
    policyStore: { type: 'inline', document: policyStore },
  });
  if (!created.ok) throw created.error;
  return created.value;
}

let clientPromise: Promise<CedarlingClient> | undefined;

export function getRendererCedarling(): Promise<CedarlingClient> {
  if (!clientPromise) {
    const pending = initializeRendererCedarling();
    clientPromise = pending;
    void pending.catch(() => {
      if (clientPromise === pending) clientPromise = undefined;
    });
  }
  return clientPromise;
}
