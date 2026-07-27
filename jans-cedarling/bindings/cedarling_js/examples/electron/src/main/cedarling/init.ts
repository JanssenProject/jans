import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
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

export interface TestConfig {
  readonly activeScenario: string;
  readonly cedarling: CedarlingBaseOptions;
  readonly scenarios?: readonly TestScenario[];
}

function findCommonDirectory(): string {
  const packagedCandidate = path.join(process.resourcesPath, 'common');
  if (existsSync(packagedCandidate)) return packagedCandidate;

  let directory = __dirname;
  for (let depth = 0; depth < 10; depth += 1) {
    const candidate = path.join(directory, 'common');
    if (existsSync(candidate)) return candidate;
    const parent = path.dirname(directory);
    if (parent === directory) break;
    directory = parent;
  }

  throw new Error(
    'The shared Cedarling fixtures are unavailable. Start from the examples tree or package the common resources.',
  );
}

const commonDirectory = findCommonDirectory();

function readJson<T>(filename: string): T {
  return JSON.parse(
    readFileSync(path.join(commonDirectory, filename), 'utf8'),
  ) as T;
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

export function loadPolicyStore(): PolicyStoreDocument {
  return readJson<PolicyStoreDocument>('policy-store.json');
}

export function loadTestConfig(): TestConfig {
  return readJson<TestConfig>('test-config.json');
}

function activeConfiguration(testConfig: TestConfig): CedarlingBaseOptions {
  const scenario = testConfig.scenarios?.find(
    ({ name }) => name === testConfig.activeScenario,
  );
  if (!scenario) {
    throw new Error(
      `Unknown Cedarling fixture scenario: ${testConfig.activeScenario}`,
    );
  }
  return deepMerge(testConfig.cedarling, scenario.override ?? {});
}

let clientPromise: Promise<CedarlingClient> | undefined;

export async function resetCedarling(): Promise<void> {
  const current = clientPromise;
  clientPromise = undefined;
  if (!current) return;
  const client = await current;
  await client.close();
}

async function initializeCedarling(): Promise<CedarlingClient> {
  const testConfig = loadTestConfig();
  const config = activeConfiguration(testConfig);
  const result = await createCedarling({
    ...config,
    policyStore: { type: 'inline', document: loadPolicyStore() },
  });

  if (!result.ok) throw result.error;
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
