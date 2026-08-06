import type { CedarlingEngine } from "../../dist/engine/engine.js";

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
