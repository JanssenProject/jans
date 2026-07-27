import {
  createCedarling,
  type CedarlingBaseOptions,
  type CedarlingClient,
} from '@janssenproject/cedarling';

const IDP_BASE_URL = 'http://localhost:9090';

interface TestScenario {
  readonly name: string;
  readonly override?: Partial<CedarlingBaseOptions>;
}

interface TestConfig {
  readonly activeScenario: string;
  readonly cedarling: CedarlingBaseOptions;
  readonly scenarios?: readonly TestScenario[];
}

let clientPromise: Promise<CedarlingClient> | undefined;

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to fetch ${url}: HTTP ${response.status}`);
  }
  const text = await response.text();
  return JSON.parse(text) as T;
}

async function initializeCedarling(): Promise<CedarlingClient> {
  const testConfig = await fetchJson<TestConfig>(
    `${IDP_BASE_URL}/config/test-config`,
  );
  const scenario = testConfig.scenarios?.find(
    ({ name }) => name === testConfig.activeScenario,
  );
  const config = Object.assign(
    {},
    testConfig.cedarling,
    scenario?.override,
  );

  console.log("Creating a cedarling instance")
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

  console.log("Cedarlinginstanceis created")
  return result.value;
}

export function getCedarling(): Promise<CedarlingClient> {
  if (!clientPromise) {
    const pending = initializeCedarling();
    clientPromise = pending;
    void pending.catch(() => {
      if (clientPromise === pending) clientPromise = undefined;
    });
  }

  return clientPromise;
}
