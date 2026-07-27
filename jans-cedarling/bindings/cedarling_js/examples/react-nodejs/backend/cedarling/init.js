import { createCedarling } from '@janssenproject/cedarling';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const TEST_CONFIG_PATH = path.resolve(__dirname, '../../../common/test-config.json');
const POLICY_STORE_PATH = path.resolve(__dirname, '../../../common/policy-store.json');

function deepMerge(base, override) {
  const result = { ...base };
  for (const key of Object.keys(override || {})) {
    const baseVal = result[key];
    const overrideVal = override[key];
    if (
      baseVal &&
      overrideVal &&
      typeof baseVal === 'object' &&
      !Array.isArray(baseVal) &&
      typeof overrideVal === 'object' &&
      !Array.isArray(overrideVal)
    ) {
      result[key] = deepMerge(baseVal, overrideVal);
    } else {
      result[key] = overrideVal;
    }
  }
  return result;
}

function readTestConfig() {
  const raw = JSON.parse(fs.readFileSync(TEST_CONFIG_PATH, 'utf8'));
  const scenario = raw.scenarios.find((s) => s.name === raw.activeScenario);
  if (!scenario) {
    throw new Error(
      `test-config.json: scenario "${raw.activeScenario}" not found. ` +
      `Available: ${raw.scenarios.map((s) => s.name).join(', ')}`,
    );
  }
  const merged = deepMerge(raw.cedarling, scenario.override);
  return { config: merged, scenario, features: raw.features };
}

export async function init() {
  const { config, scenario, features } = readTestConfig();
  const policyStore = JSON.parse(fs.readFileSync(POLICY_STORE_PATH, 'utf8'));

  console.log(`[cedarling] initializing with scenario="${scenario.name}" exercises=[${scenario.exercises.join(', ')}]`);
  console.log(`[cedarling] jwt.dangerouslyDisableSignatureValidation=${config.jwt.dangerouslyDisableSignatureValidation}`);
  console.log(`[cedarling] logging.type=${config.logging.type}`);
  if (config.logging.level) console.log(`[cedarling] logging.level=${config.logging.level}`);
  if (config.authorization?.dangerouslyDisableSchemaValidation) {
    console.log(`[cedarling] authorization.dangerouslyDisableSchemaValidation=true (known bug with authorizeMultiIssuer)`);
  }
  console.log(`[cedarling] contextStore.maxEntries=${config.contextStore.maxEntries}`);
  console.log(`[cedarling] tokenCache.capacity=${config.tokenCache.capacity}`);
  console.log(`[cedarling] issuerLoading.mode=${config.issuerLoading.mode}`);

  const result = await createCedarling({
    applicationName: config.applicationName,
    jwt: config.jwt,
    logging: config.logging,
    authorization: config.authorization,
    contextStore: config.contextStore,
    tokenCache: config.tokenCache,
    issuerLoading: config.issuerLoading,
    http: config.http,
    policyStore: {
      type: 'inline',
      document: policyStore,
    },
  });

  if (!result.ok) {
    console.error('[cedarling] INIT FAILED:', result.error);
    throw result.error;
  }

  const cedarling = result.value;
  console.log('[cedarling] initialized successfully');

  if (features.enableExercisesAtStartup) {
    console.log(`[cedarling] running startup exercises for scenario="${scenario.name}"`);
    await runStartupExercises(cedarling, scenario.exercises, scenario.expectedFailures);
  }

  return cedarling;
}

async function runStartupExercises(cedarling, exercises, expectedFailures = []) {
  for (const name of exercises) {
    try {
      switch (name) {
        case 'context': {
          const { exerciseContext } = await import('./exercise-context.js');
          await exerciseContext(cedarling);
          break;
        }
        case 'logs': {
          const { exerciseLogs } = await import('./exercise-logs.js');
          await exerciseLogs(cedarling);
          break;
        }
        case 'issuers': {
          const { exerciseIssuers } = await import('./exercise-issuers.js');
          await exerciseIssuers(cedarling);
          break;
        }
        case 'lifecycle': {
          const { exerciseLifecycle } = await import('./exercise-lifecycle.js');
          await exerciseLifecycle(cedarling);
          break;
        }
        case 'authorizeUnsigned':
        case 'authorizeMultiIssuer':
          break;
        default:
          console.warn(`[cedarling] unknown exercise "${name}"`);
      }
    } catch (err) {
      const isExpected = expectedFailures?.includes(name);
      const prefix = isExpected ? 'EXPECTED FAILURE' : 'UNEXPECTED ERROR';
      console.error(`[cedarling] ${prefix} in exercise "${name}":`, err);
    }
  }
}

const testConfig = readTestConfig();
export { testConfig };
