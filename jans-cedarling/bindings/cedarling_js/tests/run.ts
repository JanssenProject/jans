import type QUnitApi from "qunit";
import {
  type CedarlingClient,
  type CedarlingError,
  type CedarlingErrorCode,
  type CedarlingOperation,
  type CedarlingOptions,
  createCedarling,
  type Result,
} from "@janssenproject/cedarling";

export type TestGroup = "unit" | "contract" | "portable";
export type TestSuite = (QUnit: QUnitApi) => void | Promise<void>;
export type SuiteLoader = () => Promise<{ default: TestSuite }>;

export async function withCedarling(
  assert: Assert,
  options: CedarlingOptions,
  work: (client: CedarlingClient) => Promise<void>,
): Promise<void> {
  const created = await createCedarling(options);
  assert.true(created.ok, "the real WASM client initializes");
  if (!created.ok) return;
  try {
    await work(created.value);
  } finally {
    assert.true((await created.value.shutDown()).ok, "the client shuts down");
  }
}

export function assertCedarlingError<T>(
  assert: Assert,
  result: Result<T>,
  expected: {
    readonly code: CedarlingErrorCode;
    readonly operation: CedarlingOperation;
    readonly path?: readonly (string | number)[];
  },
  inspect?: (error: CedarlingError) => void,
): void {
  assert.false(result.ok, expected.operation + " returns an error");
  if (result.ok) return;
  assert.strictEqual(result.error.code, expected.code);
  assert.strictEqual(result.error.operation, expected.operation);
  if (expected.path !== undefined) {
    assert.deepEqual(result.error.path, expected.path);
  }
  inspect?.(result.error);
}

export async function runTestSuites(
  QUnit: QUnitApi,
  suiteLoaders: readonly SuiteLoader[],
  filter?: string,
): Promise<{ readonly failed: number }> {
  QUnit.config.autostart = false;
  QUnit.config.testTimeout = 60_000;
  (QUnit.config as QUnitApi["config"] & {
    reporters: { tap: boolean };
  }).reporters.tap = true;
  if (filter !== undefined) QUnit.config.filter = filter;
  const completion = new Promise<{ readonly failed: number }>((resolve) => {
    QUnit.done((details) => resolve({ failed: details.failed }));
  });
  for (const loadSuite of suiteLoaders) {
    try {
      await (await loadSuite()).default(QUnit);
    } catch (error: unknown) {
      QUnit.onUncaughtException(error);
    }
  }
  QUnit.start();
  return completion;
}
