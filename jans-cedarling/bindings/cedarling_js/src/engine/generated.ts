/**
 * Host-independent generated protocol for the `cedarling_wasm` package.
 *
 * This module owns snake-case methods, JSON transport, result conversion, and
 * wrapper lifecycle. It imports no host capability or generated module loader.
 */
import type {
  AuthorizationDecision,
  CedarEntity,
  MultiIssuerAuthorizationRequest,
  PolicyEvaluationError,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { CedarlingEngine } from "./engine.js";
import type { CedarlingOperation } from "../errors/types.js";
import {
  createSdkError,
  isSdkErrorCode,
} from "../errors/errors.js";
import type {
  CedarlingLogEntry,
  LogQuery,
} from "../logs/types.js";
import { normalizeGeneratedLog } from "../logs/normalize.js";
import type { ContextDataValue } from "../values/types.js";
import { snapshotCedarContextValue } from "../values/snapshot.js";
import {
  CEDAR_DATA_TYPE_SET,
  JS_SAFE_U64_MAX,
  LIMITS,
  LOG_KIND_SET,
} from "../helpers/constants.js";
import {
  isObjectRecord,
  ownDataProperty,
} from "../helpers/records.js";
import type {
  CedarDataType,
  ContextDataEntry,
  ContextDataStats,
} from "../context/types.js";
import type { IssuerReference } from "../issuers/types.js";

/** Operations consumed from one compatible generated Cedarling wrapper. */
interface GeneratedClientBoundary {
  /** Checks one generated trusted issuer by configured identifier. */
  isIssuerLoadedById(id: string): unknown;

  /** Checks one generated trusted issuer by exact issuer claim. */
  isIssuerLoadedByIss(iss: string): unknown;

  /** Stores one detached value in the generated data context. */
  pushDataContext(
    key: string,
    value: ContextDataValue,
    ttlSeconds?: bigint,
  ): unknown;

  /** Reads one value from the generated data context. */
  getDataContext(key: string): unknown;

  /** Reads one generated context-data metadata wrapper. */
  getDataContextEntry(key: string): unknown;

  /** Removes one generated context-data entry. */
  removeDataContext(key: string): unknown;

  /** Clears the generated context-data store. */
  clearDataContext(): unknown;

  /** Lists generated context-data metadata wrappers. */
  listDataContext(): unknown;

  /** Reads one generated context-data statistics wrapper. */
  getDataContextStats(): unknown;

  /** Enumerates retained generated log identifiers. */
  getLogIds(): unknown;

  /** Reads one retained generated entry by exact identifier. */
  getLogById(id: string): unknown;

  /** Reads retained generated entries for one authorization request. */
  getLogsByRequestId(requestId: string): unknown;

  /** Reads generated entries by request and generated tag index. */
  getLogsByRequestIdAndTag(requestId: string, tag: string): unknown;

  /** Reads generated entries by exact tag value. */
  getLogsByTag(tag: string): unknown;

  /** Returns and removes all retained generated entries. */
  popLogs(): unknown;

  /** Invokes the generated unsigned-authorization method. */
  authorizeUnsigned(request: string): Promise<unknown>;

  /** Invokes the generated multi-issuer authorization method. */
  authorizeMultiIssuer(request: string): Promise<unknown>;

  /** Requests normal Cedarling background-service shutdown. */
  shutDown(): Promise<unknown>;

  /** Releases the generated wrapper's WASM-owned allocation. */
  dispose(): void;
}

/** Operations consumed from one compatible generated authorization wrapper. */
interface GeneratedResultBoundary {
  /** Serializes the generated decision through its stable JSON method. */
  jsonString(): unknown;

  /** Releases the generated result's WASM-owned allocation. */
  dispose(): void;
}

/** Generated context-data metadata wrapper operations consumed by the SDK. */
interface GeneratedFieldBoundary {
  /** Reads one generated wrapper field. */
  field(name: string): unknown;
  /** Releases the wrapper's WASM-owned allocation. */
  dispose(): void;
}

/** Generated context-data wrapper with an additional value conversion. */
interface GeneratedDataEntryBoundary extends GeneratedFieldBoundary {
  /** Converts the stored value to a JavaScript value. */
  value(): unknown;
}

/** Generated policy error shape encoded inside the wrapper's JSON result. */
interface GeneratedPolicyEvaluationError {
  /** Generated policy identifier. */
  readonly id: string;

  /** Generated policy-evaluation message. */
  readonly error: string;
}

/** Validated generated authorization result before SDK-owned conversion. */
interface GeneratedAuthorizationResult {
  /** Final allow or deny value. */
  readonly decision: boolean;

  /** Generated request correlation identifier. */
  readonly requestId: string;

  /** Policy identifiers contributing to the decision. */
  readonly reasons: readonly string[];

  /** Generated evaluation failures copied into SDK-owned data. */
  readonly errors: readonly GeneratedPolicyEvaluationError[];
}

/** Returns true for non-null object or function values. */
function isObjectLike(value: unknown): value is object {
  return (
    (typeof value === "object" && value !== null) ||
    typeof value === "function"
  );
}

/** Checks the minimum generated module-output shape used as a version guard. */
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

/** Reads one method without allowing an accessor failure to escape. */
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

/** Lazily invokes one optional generated method through a safe receiver. */
function invokeGeneratedMethod(
  value: object,
  name: PropertyKey,
  operation: CedarlingOperation,
  ...arguments_: readonly unknown[]
): unknown {
  const method = readMethod(value, name);
  if (method === undefined) {
    throw createSdkError("GENERATED_PROTOCOL_ERROR", operation);
  }
  return Reflect.apply(method, value, arguments_);
}

/** Adapts field reads and disposal shared by generated metadata wrappers. */
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

/** Adapts one generated context-data entry wrapper. */
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

/** Converts one generated integer counter without precision loss. */
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

/** Copies and releases one generated context-data metadata wrapper. */
function copyGeneratedDataEntry(
  value: unknown,
  operation: "context.getEntry" | "context.entries",
): ContextDataEntry {
  const entry = adaptGeneratedDataEntry(value);
  if (entry === undefined) {
    throw createSdkError("GENERATED_PROTOCOL_ERROR", operation);
  }
  try {
    const key = entry.field("key");
    const dataType = entry.field("data_type");
    const createdAt = entry.field("created_at");
    const expiresAt = entry.field("expires_at");
    const accessCount = safeCounter(entry.field("access_count"));
    if (accessCount === undefined) {
      throw createSdkError("RESULT_CONVERSION_FAILED", operation);
    }
    let contextValue: ContextDataValue;
    try {
      contextValue = snapshotCedarContextValue(entry.value());
    } catch (error: unknown) {
      throw createSdkError("RESULT_CONVERSION_FAILED", operation, {
        rawCause: error,
      });
    }
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
      throw createSdkError("GENERATED_PROTOCOL_ERROR", operation);
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
  } finally {
    try {
      entry.dispose();
    } catch (error: unknown) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", operation, {
        rawCause: error,
      });
    }
  }
}

/** Copies and releases one generated context-store statistics wrapper. */
function copyGeneratedDataStats(value: unknown): ContextDataStats {
  const stats = adaptGeneratedFields(value);
  if (stats === undefined) {
    throw createSdkError("GENERATED_PROTOCOL_ERROR", "context.stats");
  }
  try {
    const entryCount = safeCounter(stats.field("entry_count"));
    const maxEntries = safeCounter(stats.field("max_entries"));
    const maxEntrySizeBytes = safeCounter(stats.field("max_entry_size"));
    const totalSizeBytes = safeCounter(stats.field("total_size_bytes"));
    const averageEntrySizeBytes = safeCounter(
      stats.field("avg_entry_size_bytes"),
    );
    const metricsEnabled = stats.field("metrics_enabled");
    const capacityUsagePercent = stats.field("capacity_usage_percent");
    const memoryAlertThresholdPercent = stats.field(
      "memory_alert_threshold",
    );
    const memoryAlertTriggered = stats.field("memory_alert_triggered");
    if (
      entryCount === undefined ||
      maxEntries === undefined ||
      maxEntrySizeBytes === undefined ||
      totalSizeBytes === undefined ||
      averageEntrySizeBytes === undefined ||
      typeof metricsEnabled !== "boolean" ||
      typeof capacityUsagePercent !== "number" ||
      !Number.isFinite(capacityUsagePercent) ||
      typeof memoryAlertThresholdPercent !== "number" ||
      !Number.isFinite(memoryAlertThresholdPercent) ||
      typeof memoryAlertTriggered !== "boolean"
    ) {
      throw createSdkError("RESULT_CONVERSION_FAILED", "context.stats");
    }
    return {
      entryCount,
      maxEntries,
      maxEntrySizeBytes,
      metricsEnabled,
      totalSizeBytes,
      averageEntrySizeBytes,
      capacityUsagePercent,
      memoryAlertThresholdPercent,
      memoryAlertTriggered,
    };
  } finally {
    try {
      stats.dispose();
    } catch (error: unknown) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "context.stats", {
        rawCause: error,
      });
    }
  }
}

/** Validates and adapts the generated client methods used by this slice. */
function adaptGeneratedClient(value: unknown): GeneratedClientBoundary | undefined {
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

  return {
    isIssuerLoadedById: (id: string) =>
      invokeGeneratedMethod(
        value,
        "is_trusted_issuer_loaded_by_name",
        "issuers.isLoaded",
        id,
      ),
    isIssuerLoadedByIss: (iss: string) =>
      invokeGeneratedMethod(
        value,
        "is_trusted_issuer_loaded_by_iss",
        "issuers.isLoaded",
        iss,
      ),
    pushDataContext: (
      key: string,
      contextValue: ContextDataValue,
      ttlSeconds?: bigint,
    ) =>
      invokeGeneratedMethod(
        value,
        "push_data_ctx",
        "context.set",
        key,
        contextValue,
        ttlSeconds,
      ),
    getDataContext: (key: string) =>
      invokeGeneratedMethod(value, "get_data_ctx", "context.get", key),
    getDataContextEntry: (key: string) =>
      invokeGeneratedMethod(
        value,
        "get_data_entry_ctx",
        "context.getEntry",
        key,
      ),
    removeDataContext: (key: string) =>
      invokeGeneratedMethod(
        value,
        "remove_data_ctx",
        "context.delete",
        key,
      ),
    clearDataContext: () =>
      invokeGeneratedMethod(value, "clear_data_ctx", "context.clear"),
    listDataContext: () =>
      invokeGeneratedMethod(value, "list_data_ctx", "context.entries"),
    getDataContextStats: () =>
      invokeGeneratedMethod(value, "get_stats_ctx", "context.stats"),
    getLogIds: () =>
      invokeGeneratedMethod(value, "get_log_ids", "logs.ids"),
    getLogById: (id: string) =>
      invokeGeneratedMethod(value, "get_log_by_id", "logs.find", id),
    getLogsByRequestId: (requestId: string) =>
      invokeGeneratedMethod(
        value,
        "get_logs_by_request_id",
        "logs.find",
        requestId,
      ),
    getLogsByRequestIdAndTag: (requestId: string, tag: string) =>
      invokeGeneratedMethod(
        value,
        "get_logs_by_request_id_and_tag",
        "logs.find",
        requestId,
        tag,
      ),
    getLogsByTag: (tag: string) =>
      invokeGeneratedMethod(value, "get_logs_by_tag", "logs.find", tag),
    popLogs: () =>
      invokeGeneratedMethod(value, "pop_logs", "logs.drain"),
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

/** Releases an unadapted generated client wrapper when its protocol permits it. */
function disposeUnadaptedGeneratedClient(value: unknown): void {
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

/** Validates and adapts one generated authorization result wrapper. */
function adaptGeneratedResult(
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
          "GENERATED_PROTOCOL_ERROR",
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

/** Requires a generated field to be a string array and copies its contents. */
function copyStringArray(value: unknown): readonly string[] | undefined {
  if (!Array.isArray(value) || !value.every((item) => typeof item === "string")) {
    return undefined;
  }
  return [...value];
}

/** Validates and copies generated policy diagnostics. */
function copyPolicyErrors(
  value: unknown,
): readonly GeneratedPolicyEvaluationError[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const copied: GeneratedPolicyEvaluationError[] = [];
  for (const item of value) {
    if (!isObjectRecord(item)) {
      return undefined;
    }
    const id = ownDataProperty(item, "id");
    const error = ownDataProperty(item, "error");
    if (typeof id !== "string" || typeof error !== "string") {
      return undefined;
    }
    copied.push({ id, error });
  }
  return copied;
}

/**
 * Validates the binding's serialized result protocol.
 *
 * Malformed JSON is a conversion failure. Valid JSON with an incompatible
 * generated field layout is an adapter protocol failure.
 */
function parseGeneratedResult(
  serialized: string,
  operation: "authorizeUnsigned" | "authorizeMultiIssuer",
): GeneratedAuthorizationResult {
  let value: unknown;
  try {
    value = JSON.parse(serialized) as unknown;
  } catch (error: unknown) {
    throw createSdkError(
      "RESULT_CONVERSION_FAILED",
      operation,
      { rawCause: error },
    );
  }

  if (!isObjectRecord(value)) {
    throw createSdkError(
      "GENERATED_PROTOCOL_ERROR",
      operation,
    );
  }

  const decision = ownDataProperty(value, "decision");
  const requestId = ownDataProperty(value, "request_id");
  const response = ownDataProperty(value, "response");
  if (
    typeof decision !== "boolean" ||
    typeof requestId !== "string" ||
    !isObjectRecord(response)
  ) {
    throw createSdkError(
      "GENERATED_PROTOCOL_ERROR",
      operation,
    );
  }

  const diagnostics = ownDataProperty(response, "diagnostics");
  if (!isObjectRecord(diagnostics)) {
    throw createSdkError(
      "GENERATED_PROTOCOL_ERROR",
      operation,
    );
  }

  const reasons = copyStringArray(ownDataProperty(diagnostics, "reason"));
  const errors = copyPolicyErrors(ownDataProperty(diagnostics, "errors"));
  if (reasons === undefined || errors === undefined) {
    throw createSdkError(
      "GENERATED_PROTOCOL_ERROR",
      operation,
    );
  }

  return { decision, requestId, reasons, errors };
}

/** Converts a detached generated diagnostic into its public SDK shape. */
function toPolicyEvaluationError(
  error: GeneratedPolicyEvaluationError,
): PolicyEvaluationError {
  return {
    policyId: error.id,
    message: error.error,
  };
}

/** Converts validated generated data into a detached public decision. */
function toAuthorizationDecision(
  parsed: GeneratedAuthorizationResult,
): AuthorizationDecision {
  return {
    decision: parsed.decision,
    requestId: parsed.requestId,
    diagnostics: {
      reasons: [...parsed.reasons],
      errors: parsed.errors.map(toPolicyEvaluationError),
    },
  };
}

/** Converts a detached public entity into the generated binding's shape. */
function toGeneratedEntity(entity: CedarEntity): Record<string, unknown> {
  return {
    ...entity.attributes,
    cedar_entity_mapping: {
      entity_type: entity.type,
      id: entity.id,
    },
  };
}

/** Converts fields shared by both authorization trust models. */
function toGeneratedAuthorizationTarget(
  request: UnsignedAuthorizationRequest | MultiIssuerAuthorizationRequest,
): Record<string, unknown> {
  return {
    action: request.action,
    resource: toGeneratedEntity(request.resource),
    context: request.context ?? {},
  };
}

/** Converts an unsigned request into the generated binding's JSON shape. */
function toGeneratedRequest(
  request: UnsignedAuthorizationRequest,
): Record<string, unknown> {
  return {
    ...(request.principal === undefined
      ? {}
      : { principal: toGeneratedEntity(request.principal) }),
    ...toGeneratedAuthorizationTarget(request),
  };
}

/** Converts a multi-issuer request into generated binding JSON. */
function toGeneratedMultiIssuerRequest(
  request: MultiIssuerAuthorizationRequest,
): Record<string, unknown> {
  return {
    tokens: request.tokens.map((token) => ({
      mapping: token.mapping,
      payload: token.payload,
    })),
    ...toGeneratedAuthorizationTarget(request),
  };
}

/**
 * Private generated implementation of the host-independent engine Seam.
 *
 * The generated client has already been protocol-checked before construction.
 * Every generated result wrapper is disposed before the method settles.
 */
class GeneratedCedarlingEngine implements CedarlingEngine {
  /** Protocol-checked generated wrapper owned until shutdown. */
  readonly #generated: GeneratedClientBoundary;

  /** Creates an engine around one compatible generated wrapper. */
  constructor(generated: GeneratedClientBoundary) {
    this.#generated = generated;
  }

  /** Invokes one generated issuer observation with stable failure mapping. */
  #issuerValue(
    operation: "issuers.isLoaded",
    invoke: () => unknown,
  ): unknown {
    try {
      return invoke();
    } catch (error: unknown) {
      if (isSdkErrorCode(error, ["GENERATED_PROTOCOL_ERROR"])) {
        throw error;
      }
      throw createSdkError("ISSUER_OPERATION_FAILED", operation, {
        rawCause: error,
      });
    }
  }

  /** Observes one generated trusted issuer by ID or exact issuer claim. */
  async isIssuerLoaded(issuer: IssuerReference): Promise<boolean> {
    const value = this.#issuerValue(
      "issuers.isLoaded",
      () =>
        "id" in issuer && issuer.id !== undefined
          ? this.#generated.isIssuerLoadedById(issuer.id)
          : this.#generated.isIssuerLoadedByIss(issuer.iss),
    );
    if (typeof value !== "boolean") {
      throw createSdkError(
        "GENERATED_PROTOCOL_ERROR",
        "issuers.isLoaded",
      );
    }
    return value;
  }

  /** Stores one detached value through the generated context operation. */
  async setContext(
    key: string,
    value: ContextDataValue,
    ttlSeconds?: number,
  ): Promise<void> {
    this.#contextValue(
      "context.set",
      () => this.#generated.pushDataContext(
        key,
        value,
        ttlSeconds === undefined ? undefined : BigInt(ttlSeconds),
      ),
    );
  }

  /** Reads one generated context value into detached SDK-owned data. */
  async getContext(
    key: string,
  ): Promise<ContextDataValue | undefined> {
    const value = this.#contextValue(
      "context.get",
      () => this.#generated.getDataContext(key),
    );
    if (value === null || value === undefined) {
      return undefined;
    }
    try {
      return snapshotCedarContextValue(value);
    } catch (error: unknown) {
      throw createSdkError("RESULT_CONVERSION_FAILED", "context.get", {
        rawCause: error,
      });
    }
  }

  /** Invokes one generated context operation with stable failure mapping. */
  #contextValue(
    operation:
      | "context.set"
      | "context.get"
      | "context.getEntry"
      | "context.delete"
      | "context.clear"
      | "context.entries"
      | "context.stats",
    invoke: () => unknown,
  ): unknown {
    try {
      return invoke();
    } catch (error: unknown) {
      if (
        isSdkErrorCode(error, [
          "RESULT_CONVERSION_FAILED",
          "GENERATED_PROTOCOL_ERROR",
        ])
      ) {
        throw error;
      }
      throw createSdkError("CONTEXT_OPERATION_FAILED", operation, {
        rawCause: error,
      });
    }
  }

  /** Reads one generated context entry and releases its metadata wrapper. */
  async getContextEntry(
    key: string,
  ): Promise<ContextDataEntry | undefined> {
    const value = this.#contextValue(
      "context.getEntry",
      () => this.#generated.getDataContextEntry(key),
    );
    return value === undefined || value === null
      ? undefined
      : copyGeneratedDataEntry(value, "context.getEntry");
  }

  /** Removes one generated context-data entry. */
  async deleteContext(key: string): Promise<boolean> {
    const value = this.#contextValue(
      "context.delete",
      () => this.#generated.removeDataContext(key),
    );
    if (typeof value !== "boolean") {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "context.delete");
    }
    return value;
  }

  /** Removes all generated context-data entries. */
  async clearContext(): Promise<void> {
    this.#contextValue(
      "context.clear",
      () => this.#generated.clearDataContext(),
    );
  }

  /** Lists and releases each generated context-data metadata wrapper. */
  async contextEntries(): Promise<readonly ContextDataEntry[]> {
    const value = this.#contextValue(
      "context.entries",
      () => this.#generated.listDataContext(),
    );
    if (!Array.isArray(value)) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "context.entries");
    }
    const entries: ContextDataEntry[] = [];
    let failure: unknown;
    for (const entry of value) {
      try {
        entries.push(
          copyGeneratedDataEntry(entry, "context.entries"),
        );
      } catch (error: unknown) {
        failure ??= error;
      }
    }
    if (failure !== undefined) {
      throw failure;
    }
    return entries;
  }

  /** Reads and releases one generated context-store statistics wrapper. */
  async contextStats(): Promise<ContextDataStats> {
    const value = this.#contextValue(
      "context.stats",
      () => this.#generated.getDataContextStats(),
    );
    return copyGeneratedDataStats(value);
  }

  /** Converts a public tag into the generated index representation. */
  #generatedTag(tag: string): string {
    return LOG_KIND_SET.has(tag)
      ? `${tag[0]?.toUpperCase()}${tag.slice(1)}`
      : tag.toUpperCase();
  }

  /** Invokes a generated log operation and normalizes unexpected failures. */
  #logValue(
    operation: "logs.ids" | "logs.find" | "logs.drain",
    invoke: () => unknown,
  ): unknown {
    try {
      return invoke();
    } catch (error: unknown) {
      if (isSdkErrorCode(error, ["GENERATED_PROTOCOL_ERROR"])) {
        throw error;
      }
      throw createSdkError("LOG_OPERATION_FAILED", operation, {
        rawCause: error,
      });
    }
  }

  /** Enumerates and detaches physically retained log identifiers. */
  async logIds(): Promise<readonly string[]> {
    const value = this.#logValue(
      "logs.ids",
      () => this.#generated.getLogIds(),
    );
    if (
      !Array.isArray(value) ||
      !value.every((item) => typeof item === "string")
    ) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "logs.ids");
    }
    return [...value];
  }

  /** Finds retained entries for one validated public query. */
  async findLogs(
    query?: LogQuery,
  ): Promise<readonly CedarlingLogEntry[]> {
    if (
      query !== undefined &&
      "tag" in query &&
      query.tag !== undefined &&
      !("requestId" in query) &&
      query.tag.length > 0
    ) {
      const value = this.#logValue(
        "logs.find",
        () => this.#generated.getLogsByTag(query.tag!),
      );
      if (!Array.isArray(value)) {
        throw createSdkError("GENERATED_PROTOCOL_ERROR", "logs.find");
      }
      return value.map((entry) =>
        normalizeGeneratedLog(entry, "logs.find"),
      );
    }

    if (query === undefined) {
      const entries: CedarlingLogEntry[] = [];
      for (const id of await this.logIds()) {
        const value = this.#logValue(
          "logs.find",
          () => this.#generated.getLogById(id),
        );
        if (value !== null && value !== undefined) {
          entries.push(normalizeGeneratedLog(value, "logs.find"));
        }
      }
      return entries;
    }

    if ("id" in query && query.id !== undefined) {
      const value = this.#logValue(
        "logs.find",
        () => this.#generated.getLogById(query.id),
      );
      return value === null || value === undefined
        ? []
        : [normalizeGeneratedLog(value, "logs.find")];
    }

    if (query.requestId === undefined) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "logs.find");
    }
    const value = this.#logValue(
      "logs.find",
      () =>
        query.tag === undefined
          ? this.#generated.getLogsByRequestId(query.requestId)
          : this.#generated.getLogsByRequestIdAndTag(
              query.requestId,
              this.#generatedTag(query.tag),
            ),
    );
    if (!Array.isArray(value)) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "logs.find");
    }
    return value.map((entry) =>
      normalizeGeneratedLog(entry, "logs.find"),
    );
  }

  /** Returns, normalizes, and removes all physically retained entries. */
  async drainLogs(): Promise<readonly CedarlingLogEntry[]> {
    const value = this.#logValue(
      "logs.drain",
      () => this.#generated.popLogs(),
    );
    if (!Array.isArray(value)) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "logs.drain");
    }
    return value.map((entry) =>
      normalizeGeneratedLog(entry, "logs.drain"),
    );
  }

  /** Invokes one generated authorization operation and copies its result. */
  async #authorize(
    operation: "authorizeUnsigned" | "authorizeMultiIssuer",
    invoke: () => Promise<unknown>,
  ): Promise<AuthorizationDecision> {
    let generatedValue: unknown;
    try {
      generatedValue = await invoke();
    } catch (error) {
      const message =
        error instanceof Error ? error.message : String(error);
      throw createSdkError(
        "AUTHORIZATION_FAILED",
        operation,
        {
          details: { wasmMessage: message },
          rawCause: error,
        },
      );
    }

    const generatedResult = adaptGeneratedResult(
      generatedValue,
      operation,
    );
    if (generatedResult === undefined) {
      throw createSdkError(
        "GENERATED_PROTOCOL_ERROR",
        operation,
      );
    }

    try {
      let serialized: unknown;
      try {
        serialized = generatedResult.jsonString();
      } catch (error: unknown) {
        if (isSdkErrorCode(error, ["GENERATED_PROTOCOL_ERROR"])) {
          throw error;
        }
        throw createSdkError(
          "RESULT_CONVERSION_FAILED",
          operation,
          { rawCause: error },
        );
      }

      if (typeof serialized !== "string") {
        throw createSdkError(
          "GENERATED_PROTOCOL_ERROR",
          operation,
        );
      }

      return toAuthorizationDecision(
        parseGeneratedResult(serialized, operation),
      );
    } finally {
      try {
        generatedResult.dispose();
      } catch (error: unknown) {
        throw createSdkError(
          "GENERATED_PROTOCOL_ERROR",
          operation,
          { rawCause: error },
        );
      }
    }
  }

  /** Evaluates a detached unsigned request and copies the generated result. */
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationDecision> {
    return this.#authorize(
      "authorizeUnsigned",
      () =>
        this.#generated.authorizeUnsigned(
          JSON.stringify(toGeneratedRequest(request)),
        ),
    );
  }

  /** Evaluates a detached multi-issuer request through its distinct method. */
  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationDecision> {
    return this.#authorize(
      "authorizeMultiIssuer",
      () =>
        this.#generated.authorizeMultiIssuer(
          JSON.stringify(toGeneratedMultiIssuerRequest(request)),
        ),
    );
  }

  /** Attempts generated shutdown and disposal exactly once per engine shutdown. */
  async shutDown(): Promise<void> {
    const failures: unknown[] = [];

    // Attempt both shutdown and wrapper disposal; either failure is normalized.
    try {
      await this.#generated.shutDown();
    } catch (error: unknown) {
      failures.push(error);
    }

    try {
      this.#generated.dispose();
    } catch (error: unknown) {
      failures.push(error);
    }

    if (failures.length > 0) {
      throw createSdkError("LIFECYCLE_FAILED", "shutDown", {
        rawCause: failures.length === 1 ? failures[0] : Object.freeze(failures),
      });
    }
  }
}

/**
 * Adapts one unknown generated wrapper into the host-independent Engine.
 *
 * The Web engine remains responsible for module loading and construction.
 */
export function createGeneratedEngine(
  generatedValue: unknown,
): CedarlingEngine | undefined {
  const generated = adaptGeneratedClient(generatedValue);
  if (generated === undefined) {
    disposeUnadaptedGeneratedClient(generatedValue);
    return undefined;
  }
  return new GeneratedCedarlingEngine(generated);
}
