import type { CedarlingContextError, Result } from "../errors/types.js";
import type { ContextDataValue } from "../values/types.js";

/**
 * Cedar type inferred by Cedarling for a stored context-data value.
 *
 * @example
 * ```ts
 * const dataType: CedarDataType = "record";
 * ```
 */
export type CedarDataType =
  | "string"
  | "long"
  | "bool"
  | "set"
  | "record"
  | "entity"
  | "ip"
  | "decimal"
  | "datetime"
  | "duration";

/**
 * Optional lifetime applied to one stored context-data value.
 *
 * @example
 * ```ts
 * const options: ContextSetOptions = { ttlSeconds: 60 };
 * ```
 */
export interface ContextSetOptions {
  /** Positive lifetime in seconds; omitted uses configured defaults. */
  readonly ttlSeconds?: number;
}

/**
 * Detached context-data value plus Cedarling-managed metadata.
 *
 * @example
 * ```ts
 * if (entry.ok && entry.value) {
 *   console.log(entry.value.key, entry.value.accessCount);
 * }
 * ```
 */
export interface ContextDataEntry {
  /** Stored context key. */
  readonly key: string;
  /** Detached stored value. */
  readonly value: ContextDataValue;
  /** Cedarling-inferred value type. */
  readonly dataType: CedarDataType;
  /** RFC 3339 creation timestamp. */
  readonly createdAt: string;
  /** RFC 3339 expiry timestamp, when the entry has a lifetime. */
  readonly expiresAt?: string;
  /** Safe access counter reported by Cedarling. */
  readonly accessCount: number;
}

/**
 * Detached context-store capacity and metric observation.
 *
 * @example
 * ```ts
 * if (stats.ok) console.log(stats.value.entryCount);
 * ```
 */
export interface ContextDataStats {
  /** Number of active stored entries. */
  readonly entryCount: number;
  /** Configured entry capacity; zero means unlimited. */
  readonly maxEntries: number;
  /** Configured per-entry byte limit; zero means unlimited. */
  readonly maxEntrySizeBytes: number;
  /** Whether access and store metrics are enabled. */
  readonly metricsEnabled: boolean;
  /** Approximate serialized bytes used by active entries. */
  readonly totalSizeBytes: number;
  /** Average serialized bytes per active entry. */
  readonly averageEntrySizeBytes: number;
  /** Percentage of configured entry capacity currently used. */
  readonly capacityUsagePercent: number;
  /** Configured memory warning threshold percentage. */
  readonly memoryAlertThresholdPercent: number;
  /** Whether current usage exceeds the configured warning threshold. */
  readonly memoryAlertTriggered: boolean;
}

/**
 * Public per-client context-data service.
 *
 * @example
 * ```ts
 * await client.context.set("feature_enabled", true);
 * const fact = await client.context.get("feature_enabled");
 * ```
 */
export interface CedarlingContext {
  /**
   * Stores or replaces one detached context-data value.
   *
   * @param key - Non-empty key injected below `context.data`.
   * @param value - Cedar-compatible value for later `context.data` injection.
   * @param options - Optional entry lifetime.
   */
  set(
    key: string,
    value: ContextDataValue,
    options?: ContextSetOptions,
  ): Promise<Result<void, CedarlingContextError>>;

  /**
   * Reads one detached context-data value.
   *
   * @param key - Non-empty stored-data key.
   * @returns The value, `undefined` when missing or expired, or an error.
   */
  get(
    key: string,
  ): Promise<Result<ContextDataValue | undefined, CedarlingContextError>>;

  /** Reads one value with its Cedarling-managed metadata. */
  getEntry(
    key: string,
  ): Promise<Result<ContextDataEntry | undefined, CedarlingContextError>>;

  /** Removes one key and reports whether it existed. */
  delete(key: string): Promise<Result<boolean, CedarlingContextError>>;

  /** Removes every entry from this client's context store. */
  clear(): Promise<Result<void, CedarlingContextError>>;

  /** Lists current, non-expired entries as detached values. */
  entries(): Promise<
    Result<readonly ContextDataEntry[], CedarlingContextError>
  >;

  /** Observes current context-store capacity and metrics. */
  stats(): Promise<Result<ContextDataStats, CedarlingContextError>>;
}
