import { createCedarling } from '@janssenproject/cedarling';

const BACKEND_URL = (import.meta as any).env?.VITE_BACKEND_URL || 'http://localhost:8080';
const POLICY_STORE_URL = `${BACKEND_URL}/config/policy-store`;
const TEST_CONFIG_URL = `${BACKEND_URL}/config/test-config`;

let _cedarling: any = null;

export async function initCedarling(): Promise<any> {
  if (_cedarling) return _cedarling;

  const [configRes, policyRes] = await Promise.all([
    fetch(TEST_CONFIG_URL),
    fetch(POLICY_STORE_URL),
  ]);
  if (!configRes.ok) throw new Error(`Failed to fetch test config: ${configRes.status}`);
  if (!policyRes.ok) throw new Error(`Failed to fetch policy store: ${policyRes.status}`);

  const testConfig = await configRes.json();
  const document = await policyRes.json();

  const scenario = testConfig.scenarios.find((s: any) => s.name === testConfig.activeScenario);
  if (!scenario) throw new Error(`Scenario "${testConfig.activeScenario}" not found`);

  const merge = (base: any, override: any): any => {
    const r = { ...base };
    for (const k of Object.keys(override || {})) {
      if (
        typeof r[k] === 'object' && !Array.isArray(r[k]) &&
        typeof override[k] === 'object' && !Array.isArray(override[k])
      ) {
        r[k] = merge(r[k], override[k]);
      } else {
        r[k] = override[k];
      }
    }
    return r;
  };

  const config = merge(testConfig.cedarling, scenario.override);

  console.log(`[cedarling] browser init scenario="${scenario.name}" backend="${BACKEND_URL}"`);
  console.log(`[cedarling] logging.type=${config.logging.type}`);

  const res = await createCedarling({
    applicationName: config.applicationName,
    jwt: config.jwt,
    logging: config.logging,
    authorization: config.authorization,
    contextStore: config.contextStore,
    policyStore: { type: 'inline', document },
  });

  if (!res.ok) {
    console.error('[cedarling] INIT FAILED:', res.error);
    throw res.error;
  }

  _cedarling = res.value;
  return _cedarling;
}

export function getCedarling(): any {
  return _cedarling;
}
