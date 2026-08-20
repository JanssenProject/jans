import { errorCode, type CedarlingOperation } from "../errors/types.js";
import { createSdkError } from "../errors/errors.js";
import type { ContextDataValue } from "../values/types.js";
import { snapshotCedarValue } from "../values/snapshot.js";
import { CEDAR_DATA_TYPE_SET, JS_SAFE_U64_MAX, LIMITS } from "../helpers/constants.js";
import type { CedarDataType, ContextDataEntry, ContextDataStats } from "../context/types.js";

export interface GeneratedClientBoundary {
  isIssuerLoadedById(id: string): unknown;

  isIssuerLoadedByIss(iss: string): unknown;

  pushDataContext(
    key: string,
    value: ContextDataValue,
    ttlSeconds?: bigint,
  ): unknown;

  getDataContext(key: string): unknown;

  getDataContextEntry(key: string): unknown;

  removeDataContext(key: string): unknown;

  clearDataContext(): unknown;

  listDataContext(): unknown;

  getDataContextStats(): unknown;

  getLogIds(): unknown;

  getLogById(id: string): unknown;

  getLogsByRequestId(requestId: string): unknown;

  getLogsByRequestIdAndTag(requestId: string, tag: string): unknown;

  getLogsByTag(tag: string): unknown;

  popLogs(): unknown;

  authorizeUnsigned(request: string): Promise<unknown>;

  authorizeMultiIssuer(request: string): Promise<unknown>;

  shutDown(): Promise<unknown>;

  dispose(): void;
}

export interface GeneratedResultBoundary {
  jsonString(): unknown;

  dispose(): void;
}

interface GeneratedFieldBoundary {
  field(name: string): unknown;
  dispose(): void;
}

interface GeneratedDataEntryBoundary extends GeneratedFieldBoundary {
  value(): unknown;
}

function isObjectLike(value: unknown): value is object {
  return (
    (typeof value === "object" && value !== null) ||
    typeof value === "function"
  );
}

export function hasGeneratedModuleOutput(value: unknown): boolean {
  if (!isObjectLike(value)) {
    return false;
  }

  try {
    return Reflect.has(value, "memory");
  } catch {
    return false;
  }
}

function readMethod(
  value: object,
  name: PropertyKey,
): ((...arguments_: unknown[]) => unknown) | undefined {
  try {
    const method = Reflect.get(value, name) as unknown;
    return typeof method === "function"
      ? (method as (...arguments_: unknown[]) => unknown)
      : undefined;
  } catch {
    return undefined;
  }
}

function invokeGeneratedMethod(
  value: object,
  name: PropertyKey,
  operation: CedarlingOperation,
  ...arguments_: readonly unknown[]
): unknown {
  const method = readMethod(value, name);
  if (method === undefined) {
    throw createSdkError(errorCode.generatedProtocolError, operation);
  }
  return Reflect.apply(method, value, arguments_);
}

function adaptGeneratedFields(
  value: unknown,
): GeneratedFieldBoundary | undefined {
  if (!isObjectLike(value)) {
    return undefined;
  }
  const dispose = readMethod(value, "free");
  if (dispose === undefined) {
    return undefined;
  }
  return {
    field(name: string): unknown {
      return Reflect.get(value, name);
    },
    dispose(): void {
      dispose.call(value);
    },
  };
}

function adaptGeneratedDataEntry(
  value: unknown,
): GeneratedDataEntryBoundary | undefined {
  const fields = adaptGeneratedFields(value);
  if (!isObjectLike(value)) {
    return undefined;
  }
  const getValue = readMethod(value, "value");
  if (fields === undefined || getValue === undefined) {
    return undefined;
  }
  return {
    ...fields,
    value(): unknown {
      return getValue.call(value);
    },
  };
}

function safeCounter(value: unknown): number | undefined {
  if (typeof value === "bigint") {
    return value >= BigInt(LIMITS.unsignedInteger.minimum) &&
      value <= BigInt(JS_SAFE_U64_MAX)
      ? Number(value)
      : undefined;
  }
  return typeof value === "number" &&
    Number.isSafeInteger(value) &&
    value >= LIMITS.unsignedInteger.minimum
    ? value
    : undefined;
}

/** Converts one generated wrapper and always releases it exactly once. */
export function withGeneratedWrapper<T>(
  wrapper: { dispose(): void },
  operation: CedarlingOperation,
  convert: () => T,
): T {
  let outcome: { readonly value: T } | { readonly error: unknown };
  try {
    outcome = { value: convert() };
  } catch (error: unknown) {
    outcome = { error };
  }
  try {
    wrapper.dispose();
  } catch (error: unknown) {
    if ("value" in outcome) {
      throw createSdkError(errorCode.generatedProtocolError, operation, {
        rawCause: error,
      });
    }
  }
  if ("error" in outcome) throw outcome.error;
  return outcome.value;
}

function requiredCounter(
  fields: GeneratedFieldBoundary,
  name: string,
  operation: CedarlingOperation,
): number {
  const value = safeCounter(fields.field(name));
  if (value === undefined) {
    throw createSdkError(errorCode.resultConversionFailed, operation);
  }
  return value;
}

function requiredBoolean(
  fields: GeneratedFieldBoundary,
  name: string,
  operation: CedarlingOperation,
): boolean {
  const value = fields.field(name);
  if (typeof value !== "boolean") {
    throw createSdkError(errorCode.resultConversionFailed, operation);
  }
  return value;
}

function requiredFiniteNumber(
  fields: GeneratedFieldBoundary,
  name: string,
  operation: CedarlingOperation,
): number {
  const value = fields.field(name);
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw createSdkError(errorCode.resultConversionFailed, operation);
  }
  return value;
}

export function copyGeneratedDataEntry(
  value: unknown,
  operation: "context.getEntry" | "context.entries",
): ContextDataEntry {
  const entry = adaptGeneratedDataEntry(value);
  if (entry === undefined) {
    throw createSdkError(errorCode.generatedProtocolError, operation);
  }
  return withGeneratedWrapper(entry, operation, () => {
    const key = entry.field("key");
    const dataType = entry.field("data_type");
    const createdAt = entry.field("created_at");
    const expiresAt = entry.field("expires_at");
    if (
      typeof key !== "string" ||
      key.length === 0 ||
      typeof dataType !== "string" ||
      !CEDAR_DATA_TYPE_SET.has(dataType) ||
      typeof createdAt !== "string" ||
      (expiresAt !== undefined &&
        expiresAt !== null &&
        typeof expiresAt !== "string")
    ) {
      throw createSdkError(errorCode.generatedProtocolError, operation);
    }
    const accessCount = requiredCounter(entry, "access_count", operation);

    let contextValue: ContextDataValue;
    try {
      contextValue = snapshotCedarValue(entry.value(), operation);
    } catch (error: unknown) {
      throw createSdkError(errorCode.resultConversionFailed, operation, {
        rawCause: error,
      });
    }
    return {
      key,
      value: contextValue,
      dataType: dataType as CedarDataType,
      createdAt,
      ...(expiresAt === undefined || expiresAt === null
        ? {}
        : { expiresAt }),
      accessCount,
    };
  });
}

export function copyGeneratedDataStats(value: unknown): ContextDataStats {
  const operation = "context.stats";
  const stats = adaptGeneratedFields(value);
  if (stats === undefined) {
    throw createSdkError(errorCode.generatedProtocolError, operation);
  }
  return withGeneratedWrapper(stats, operation, () => ({
    entryCount: requiredCounter(stats, "entry_count", operation),
    maxEntries: requiredCounter(stats, "max_entries", operation),
    maxEntrySizeBytes: requiredCounter(
      stats,
      "max_entry_size",
      operation,
    ),
    metricsEnabled: requiredBoolean(stats, "metrics_enabled", operation),
    totalSizeBytes: requiredCounter(
      stats,
      "total_size_bytes",
      operation,
    ),
    averageEntrySizeBytes: requiredCounter(
      stats,
      "avg_entry_size_bytes",
      operation,
    ),
    capacityUsagePercent: requiredFiniteNumber(
      stats,
      "capacity_usage_percent",
      operation,
    ),
    memoryAlertThresholdPercent: requiredFiniteNumber(
      stats,
      "memory_alert_threshold",
      operation,
    ),
    memoryAlertTriggered: requiredBoolean(
      stats,
      "memory_alert_triggered",
      operation,
    ),
  }));
}

export function adaptGeneratedClient(value: unknown): GeneratedClientBoundary | undefined {
  if (!isObjectLike(value)) {
    return undefined;
  }

  const authorizeUnsigned = readMethod(value, "authorize_unsigned");
  const authorizeMultiIssuer = readMethod(
    value,
    "authorize_multi_issuer",
  );
  const shutDown = readMethod(value, "shut_down");
  const dispose = readMethod(value, "free");
  if (
    authorizeUnsigned === undefined ||
    authorizeMultiIssuer === undefined ||
    shutDown === undefined ||
    dispose === undefined
  ) {
    return undefined;
  }

  const invoke = (
    name: PropertyKey,
    operation: CedarlingOperation,
    ...arguments_: readonly unknown[]
  ) => invokeGeneratedMethod(value, name, operation, ...arguments_);

  return {
    isIssuerLoadedById: (id: string) =>
      invoke(
        "is_trusted_issuer_loaded_by_name",
        "issuers.isLoaded",
        id,
      ),
    isIssuerLoadedByIss: (iss: string) =>
      invoke(
        "is_trusted_issuer_loaded_by_iss",
        "issuers.isLoaded",
        iss,
      ),
    pushDataContext: (
      key: string,
      contextValue: ContextDataValue,
      ttlSeconds?: bigint,
    ) =>
      invoke(
        "push_data_ctx",
        "context.set",
        key,
        contextValue,
        ttlSeconds,
      ),
    getDataContext: (key: string) =>
      invoke("get_data_ctx", "context.get", key),
    getDataContextEntry: (key: string) =>
      invoke(
        "get_data_entry_ctx",
        "context.getEntry",
        key,
      ),
    removeDataContext: (key: string) =>
      invoke(
        "remove_data_ctx",
        "context.delete",
        key,
      ),
    clearDataContext: () =>
      invoke("clear_data_ctx", "context.clear"),
    listDataContext: () =>
      invoke("list_data_ctx", "context.entries"),
    getDataContextStats: () =>
      invoke("get_stats_ctx", "context.stats"),
    getLogIds: () =>
      invoke("get_log_ids", "logs.ids"),
    getLogById: (id: string) =>
      invoke("get_log_by_id", "logs.find", id),
    getLogsByRequestId: (requestId: string) =>
      invoke(
        "get_logs_by_request_id",
        "logs.find",
        requestId,
      ),
    getLogsByRequestIdAndTag: (requestId: string, tag: string) =>
      invoke(
        "get_logs_by_request_id_and_tag",
        "logs.find",
        requestId,
        tag,
      ),
    getLogsByTag: (tag: string) =>
      invoke("get_logs_by_tag", "logs.find", tag),
    popLogs: () =>
      invoke("pop_logs", "logs.drain"),
    async authorizeUnsigned(request: string): Promise<unknown> {
      return authorizeUnsigned.call(value, request);
    },
    async authorizeMultiIssuer(request: string): Promise<unknown> {
      return authorizeMultiIssuer.call(value, request);
    },
    async shutDown(): Promise<unknown> {
      return shutDown.call(value);
    },
    dispose(): void {
      dispose.call(value);
    },
  };
}

export function disposeUnadaptedGeneratedClient(value: unknown): void {
  if (!isObjectLike(value)) {
    return;
  }

  const dispose = readMethod(value, "free");
  if (dispose === undefined) {
    return;
  }

  try {
    dispose.call(value);
  } catch {
    // The caller already reports the incompatible generated protocol.
  }
}

export function adaptGeneratedResult(
  value: unknown,
  operation: "authorizeUnsigned" | "authorizeMultiIssuer",
): GeneratedResultBoundary | undefined {
  if (!isObjectLike(value)) {
    return undefined;
  }

  const jsonString = readMethod(value, "json_string");
  const dispose = readMethod(value, "free");
  if (dispose === undefined) {
    return undefined;
  }

  return {
    jsonString(): unknown {
      if (jsonString === undefined) {
        throw createSdkError(
          errorCode.generatedProtocolError,
          operation,
        );
      }
      return jsonString.call(value);
    },
    dispose(): void {
      dispose.call(value);
    },
  };
}
