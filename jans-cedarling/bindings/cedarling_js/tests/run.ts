import type QUnitApi from "qunit";

export type TestGroup = "unit" | "contract" | "portable";
type TestSuite = (QUnit: QUnitApi) => void | Promise<void>;
export type SuiteLoader = () => Promise<{ default: TestSuite }>;

export async function runTestSuites(
  QUnit: QUnitApi,
  suiteLoaders: readonly SuiteLoader[],
  filter?: string,
): Promise<{ readonly failed: number }> {
  QUnit.config.autostart = false;
  QUnit.config.testTimeout = 60_000;
  (QUnit.config as QUnitApi["config"] & { reporters: { tap: boolean } }).reporters.tap = true;
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
