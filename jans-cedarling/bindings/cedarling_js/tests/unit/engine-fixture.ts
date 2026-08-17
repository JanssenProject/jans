import type { CedarlingEngine } from "../../dist/engine/engine.js";
import { createGeneratedEngine } from "../../dist/engine/generated.js";

/**
 * Creates a complete private engine test double with explicit safe defaults.
 *
 * Focused unit tests override only the operations whose behavior they observe;
 * newly added engine services therefore have one default rather than being
 * repeated across unrelated authorization and lifecycle tests.
 */
export function createTestEngine(
  overrides: Partial<CedarlingEngine> = {},
): CedarlingEngine {
  return {
    async isIssuerLoaded() {
      return false;
    },
    async setContext() {},
    async getContext() {
      return undefined;
    },
    async getContextEntry() {
      return undefined;
    },
    async deleteContext() {
      return false;
    },
    async clearContext() {},
    async contextEntries() {
      return [];
    },
    async contextStats() {
      return {
        entryCount: 0,
        maxEntries: 0,
        maxEntrySizeBytes: 0,
        metricsEnabled: false,
        totalSizeBytes: 0,
        averageEntrySizeBytes: 0,
        capacityUsagePercent: 0,
        memoryAlertThresholdPercent: 0,
        memoryAlertTriggered: false,
      };
    },
    async logIds() {
      return [];
    },
    async findLogs() {
      return [];
    },
    async drainLogs() {
      return [];
    },
    async authorizeUnsigned() {
      throw new Error("unsigned authorization is outside this test");
    },
    async authorizeMultiIssuer() {
      throw new Error("multi-issuer authorization is outside this test");
    },
    async shutDown() {},
    ...overrides,
  };
}

export function createGeneratedClientFixture(
  overrides: Readonly<Record<string, unknown>> = {},
): object {
  const fixture = {
    async authorize_unsigned() {
      throw new Error("authorization is outside this test");
    },
    async authorize_multi_issuer() {
      throw new Error("authorization is outside this test");
    },
    async shut_down() {},
    free() {},
  };
  Object.defineProperties(fixture, Object.getOwnPropertyDescriptors(overrides));
  return fixture;
}

export function createGeneratedEngineFixture(
  overrides: Readonly<Record<string, unknown>> = {},
): CedarlingEngine {
  const engine = createGeneratedEngine(
    createGeneratedClientFixture(overrides),
  );
  if (engine === undefined) {
    throw new Error("The generated client fixture is incompatible.");
  }
  return engine;
}
