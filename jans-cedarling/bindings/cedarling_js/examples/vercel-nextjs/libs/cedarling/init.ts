import {
  createCedarling,
  type CedarlingBaseOptions,
  type CedarlingClient,
} from '@janssenproject/cedarling';

const IDP_BASE_URL = process.env.OIDC_ISSUER ?? 'http://localhost:9090';

interface TestScenario {
  readonly name: string;
  readonly override?: Partial<CedarlingBaseOptions>;
}

interface TestConfig {
  readonly activeScenario: string;
  readonly cedarling: CedarlingBaseOptions;
  readonly scenarios?: readonly TestScenario[];
}

const globalForCedarling = globalThis as typeof globalThis & {
  taskAppCedarlingClient?: Promise<CedarlingClient>;
};

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to fetch ${url}: HTTP ${response.status}`);
  }
  const text = await response.text();
  return JSON.parse(text) as T;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function deepMerge<T extends object>(
  base: T,
  override?: Partial<T>,
): T {
  const result = { ...base } as Record<string, unknown>;
  for (const [key, overrideValue] of Object.entries(override ?? {})) {
    const baseValue = result[key];
    result[key] = isRecord(baseValue) && isRecord(overrideValue)
      ? deepMerge(baseValue, overrideValue)
      : overrideValue;
  }
  return result as T;
}

export async function createCedarlingClient(
  override?: Partial<CedarlingBaseOptions>,
): Promise<CedarlingClient> {
  const testConfig = await fetchJson<TestConfig>(
    `${IDP_BASE_URL}/config/test-config`,
  );
  const scenario = testConfig.scenarios?.find(
    ({ name }) => name === testConfig.activeScenario,
  );
  const scenarioConfig = deepMerge(
    testConfig.cedarling,
    scenario?.override,
  );
  const config = deepMerge(scenarioConfig, override);

  const result = await createCedarling({
    ...config,
    policyStore: {
      type: 'url',
      url: `${IDP_BASE_URL}/config/policy-store`,
    },
  });

  if (!result.ok) {
    throw result.error;
  }

  return result.value;
}

async function initializeCedarling(): Promise<CedarlingClient> {
  return createCedarlingClient();
}

export function getCedarling(): Promise<CedarlingClient> {
  if (!globalForCedarling.taskAppCedarlingClient) {
    const pending = initializeCedarling();
    globalForCedarling.taskAppCedarlingClient = pending;
    void pending.catch(() => {
      if (globalForCedarling.taskAppCedarlingClient === pending) {
        delete globalForCedarling.taskAppCedarlingClient;
      }
    });
  }

  return globalForCedarling.taskAppCedarlingClient;
}
