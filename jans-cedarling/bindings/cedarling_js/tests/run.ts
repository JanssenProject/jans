import type QUnitApi from "qunit";

/** Test groups shared by every runtime runner. */
export type TestGroup = "unit" | "contract" | "e2e";

/** Public SDK entry loaded inside an isolated runtime fixture. */
export type IsolatedSdkEntry = Pick<
  typeof import("@janssenproject/cedarling"),
  "createCedarling"
>;

/** Controls one isolated SDK installation with an initially missing asset. */
export interface MissingWasmAssetFixture {
  /** Public package entry resolved from the isolated installation. */
  readonly sdk: IsolatedSdkEntry;

  /** Restores the dependency-owned asset without reloading the SDK module. */
  restoreWasmAsset(): Promise<void>;
}

/** Runtime-owned local HTTP server exposing synthetic policy-store data. */
export interface PolicyServerFixture {
  /** URL returning the shared tracer policy store as JSON. */
  readonly jsonUrl: string;

  /** Extensionless URL returning the shared tracer policy store as `.cjar`. */
  readonly archiveUrl: string;

  /** Number of requests received by the fixture. */
  requestCount(): number;

  /** Replaces the JSON endpoint's next response. */
  setJsonResponse(status: number, body: string): void;

  /** Resolves after at least the requested number of HTTP calls arrive. */
  waitForRequestCount(count: number): Promise<void>;
}

/** Host capabilities supplied to otherwise runtime-neutral shared tests. */
export interface RuntimeFixtures {
  /** Stable runtime label used in capability-test diagnostics. */
  readonly runtime: string;

  /**
   * Runs a callback against an isolated SDK whose generated WASM asset is
   * absent until the callback explicitly restores it.
   */
  withMissingWasmAsset(
    run: (fixture: MissingWasmAssetFixture) => Promise<void>,
  ): Promise<void>;

  /**
   * Runs a callback against an isolated SDK while the host WebAssembly API is
   * unavailable, restoring the global capability afterward.
   */
  withMissingWebAssembly(
    run: (sdk: IsolatedSdkEntry) => Promise<void>,
  ): Promise<void>;

  /** Runs a callback while a loopback synthetic policy server is available. */
  withPolicyServer(
    run: (fixture: PolicyServerFixture) => Promise<void>,
  ): Promise<void>;

  /** Reads a fresh copy of the synthetic tracer `.cjar` fixture. */
  loadTracerArchive(): Promise<Uint8Array>;
}

/** Registers one runtime-neutral set of QUnit modules and tests. */
export type TestSuite = (
  QUnit: QUnitApi,
  fixtures: RuntimeFixtures,
) => void | Promise<void>;

/** Loads a test module without evaluating it before QUnit is configured. */
export type SuiteLoader = () => Promise<{ default: TestSuite }>;

/** Runtime-neutral statistics returned to a host-specific runner. */
export interface TestRunSummary {
  /** Number of failed assertions. */
  readonly failed: number;

  /** Number of passed assertions. */
  readonly passed: number;

  /** Total assertion count. */
  readonly total: number;

  /** QUnit-reported execution time in milliseconds. */
  readonly runtime: number;
}

/** QUnit TAP reporter extension not yet represented by its type package. */
type QUnitReporterConfiguration = QUnitApi["config"] & {
  reporters: {
    tap: boolean;
  };
};

/**
 * Configures QUnit, registers one shared group, and returns its final summary.
 *
 * QUnit is injected by the host runner. This keeps suite composition identical
 * across Node.js and future browser, Deno, Bun, Electron, worker, and edge
 * runners while allowing each host to translate failures into its own exit
 * mechanism.
 *
 * @param QUnit - Host-loaded QUnit instance.
 * @param suiteLoaders - Test suite loaders to run.
 * @param fixtures - Host-provided capability fixtures.
 * @param filter - Optional QUnit name filter.
 * @returns Final QUnit statistics after all registered tests finish.
 */
export async function runTestSuites(
  QUnit: QUnitApi,
  suiteLoaders: readonly SuiteLoader[],
  fixtures: RuntimeFixtures,
  filter?: string,
): Promise<TestRunSummary> {
  QUnit.config.autostart = false;
  QUnit.config.testTimeout = 60_000;
  const reporterConfiguration = QUnit.config as QUnitReporterConfiguration;
  reporterConfiguration.reporters.tap = true;

  if (filter !== undefined) {
    QUnit.config.filter = filter;
  }

  const completion = new Promise<TestRunSummary>((resolve) => {
    QUnit.done((details) => {
      resolve({
        failed: details.failed,
        passed: details.passed,
        total: details.total,
        runtime: details.runtime,
      });
    });
  });

  for (const loadSuite of suiteLoaders) {
    try {
      const suite = await loadSuite();
      await suite.default(QUnit, fixtures);
    } catch (error: unknown) {
      // Isolate per-suite registration failures so one broken `*.test.ts`
      // cannot silently truncate the registration of every later suite.
      QUnit.onUncaughtException(error);
    }
  }

  QUnit.start();
  return completion;
}
