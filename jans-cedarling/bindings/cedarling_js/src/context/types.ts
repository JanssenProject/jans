import type { Result } from "../errors/types.js";
import type { ContextDataValue } from "../values/types.js";
import type { CEDAR_DATA_TYPES } from "../helpers/constants.js";

export type CedarDataType = (typeof CEDAR_DATA_TYPES)[number];

export interface ContextSetOptions {
  readonly ttlSeconds?: number;
}

/**
 * Detached context-data value plus Cedarling-managed metadata with RFC 3339
 * creation and optional expiry timestamps.
 */
export interface ContextDataEntry {
  readonly key: string;
  readonly value: ContextDataValue;
  readonly dataType: CedarDataType;
  readonly createdAt: string;
  readonly expiresAt?: string;
  readonly accessCount: number;
}

/**
 * Detached context-store capacity and metric observation. Zero max-entry count
 * and size limits mean unlimited; byte measurements are approximate serialized
 * sizes.
 */
export interface ContextDataStats {
  readonly entryCount: number;
  readonly maxEntries: number;
  readonly maxEntrySizeBytes: number;
  readonly metricsEnabled: boolean;
  readonly totalSizeBytes: number;
  readonly averageEntrySizeBytes: number;
  readonly capacityUsagePercent: number;
  readonly memoryAlertThresholdPercent: number;
  readonly memoryAlertTriggered: boolean;
}

export interface CedarlingContext {
  set(
    key: string,
    value: ContextDataValue,
    options?: ContextSetOptions,
  ): Promise<Result<void>>;

  get(
    key: string,
  ): Promise<Result<ContextDataValue | undefined>>;

  getEntry(
    key: string,
  ): Promise<Result<ContextDataEntry | undefined>>;

  delete(key: string): Promise<Result<boolean>>;
  clear(): Promise<Result<void>>;
  entries(): Promise<Result<readonly ContextDataEntry[]>>;
  stats(): Promise<Result<ContextDataStats>>;
}
