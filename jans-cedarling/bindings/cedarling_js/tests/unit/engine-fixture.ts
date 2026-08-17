import type { CedarlingEngine } from "../../dist/engine/engine.js";
import { createGeneratedEngine } from "../../dist/engine/generated.js";

interface RawGeneratedClientFixture {
  authorize_unsigned(request: string): Promise<unknown>;
  authorize_multi_issuer(request: string): Promise<unknown>;
  shut_down(): Promise<unknown>;
  free(): void;
  is_trusted_issuer_loaded_by_name?(id: string): unknown;
  is_trusted_issuer_loaded_by_iss?(iss: string): unknown;
  push_data_ctx?(key: string, value: unknown, ttlSeconds?: bigint): unknown;
  get_data_ctx?(key: string): unknown;
  get_data_entry_ctx?(key: string): unknown;
  remove_data_ctx?(key: string): unknown;
  clear_data_ctx?(): unknown;
  list_data_ctx?(): unknown;
  get_stats_ctx?(): unknown;
  get_log_ids?(): unknown;
  get_log_by_id?(id: string): unknown;
  get_logs_by_request_id?(requestId: string): unknown;
  get_logs_by_request_id_and_tag?(requestId: string, tag: string): unknown;
  get_logs_by_tag?(tag: string): unknown;
  pop_logs?(): unknown;
}

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
  overrides: Partial<RawGeneratedClientFixture> = {},
): RawGeneratedClientFixture {
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
  overrides: Partial<RawGeneratedClientFixture> = {},
): CedarlingEngine {
  const engine = createGeneratedEngine(
    createGeneratedClientFixture(overrides),
  );
  if (engine === undefined) {
    throw new Error("The generated client fixture is incompatible.");
  }
  return engine;
}
