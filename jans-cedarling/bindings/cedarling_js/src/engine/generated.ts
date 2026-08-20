import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { CedarlingEngine } from "./engine.js";
import type { CedarlingOperation } from "../errors/types.js";
import { errorCode } from "../errors/types.js";
import {
  createSdkError,
  isSdkErrorCode,
  normalizeOperationError,
} from "../errors/errors.js";
import type { CedarlingLogEntry, LogQuery } from "../logs/types.js";
import { normalizeGeneratedLog } from "../logs/normalize.js";
import type { ContextDataValue } from "../values/types.js";
import { snapshotCedarValue } from "../values/snapshot.js";
import { LOG_KIND_SET } from "../helpers/constants.js";
import type { ContextDataEntry, ContextDataStats } from "../context/types.js";
import type { IssuerReference } from "../issuers/types.js";
import {
  adaptGeneratedClient,
  adaptGeneratedResult,
  copyGeneratedDataEntry,
  copyGeneratedDataStats,
  disposeUnadaptedGeneratedClient,
  type GeneratedClientBoundary,
  withGeneratedWrapper,
} from "./generated-wrapper.js";
import {
  parseGeneratedResult,
  toAuthorizationDecision,
  toGeneratedMultiIssuerRequest,
  toGeneratedRequest,
} from "./generated-authorization.js";

export { hasGeneratedModuleOutput } from "./generated-wrapper.js";

/**
 * Private generated implementation of the host-independent engine Seam.
 *
 * The generated client has already been protocol-checked before construction.
 * Every generated result wrapper is disposed before the method settles.
 */
class GeneratedCedarlingEngine implements CedarlingEngine {
  readonly #generated: GeneratedClientBoundary;

  constructor(generated: GeneratedClientBoundary) {
    this.#generated = generated;
  }

  #generatedValue(
    operation: CedarlingOperation,
    invoke: () => unknown,
  ): unknown {
    try {
      return invoke();
    } catch (error: unknown) {
      throw normalizeOperationError(error, operation, false);
    }
  }

  async isIssuerLoaded(issuer: IssuerReference): Promise<boolean> {
    const value = this.#generatedValue(
      "issuers.isLoaded",
      () =>
        "id" in issuer && issuer.id !== undefined
          ? this.#generated.isIssuerLoadedById(issuer.id)
          : this.#generated.isIssuerLoadedByIss(issuer.iss),
    );
    if (typeof value !== "boolean") {
      throw createSdkError(
        errorCode.generatedProtocolError,
        "issuers.isLoaded",
      );
    }
    return value;
  }

  async setContext(
    key: string,
    value: ContextDataValue,
    ttlSeconds?: number,
  ): Promise<void> {
    this.#generatedValue(
      "context.set",
      () => this.#generated.pushDataContext(
        key,
        value,
        ttlSeconds === undefined ? undefined : BigInt(ttlSeconds),
      ),
    );
  }

  async getContext(
    key: string,
  ): Promise<ContextDataValue | undefined> {
    const value = this.#generatedValue(
      "context.get",
      () => this.#generated.getDataContext(key),
    );
    if (value === null || value === undefined) {
      return undefined;
    }
    try {
      return snapshotCedarValue(value, "context.get");
    } catch (error: unknown) {
      throw createSdkError(errorCode.resultConversionFailed, "context.get", {
        rawCause: error,
      });
    }
  }

  async getContextEntry(
    key: string,
  ): Promise<ContextDataEntry | undefined> {
    const value = this.#generatedValue(
      "context.getEntry",
      () => this.#generated.getDataContextEntry(key),
    );
    return value === undefined || value === null
      ? undefined
      : copyGeneratedDataEntry(value, "context.getEntry");
  }

  async deleteContext(key: string): Promise<boolean> {
    const value = this.#generatedValue(
      "context.delete",
      () => this.#generated.removeDataContext(key),
    );
    if (typeof value !== "boolean") {
      throw createSdkError(errorCode.generatedProtocolError, "context.delete");
    }
    return value;
  }

  async clearContext(): Promise<void> {
    this.#generatedValue(
      "context.clear",
      () => this.#generated.clearDataContext(),
    );
  }

  async contextEntries(): Promise<readonly ContextDataEntry[]> {
    const value = this.#generatedValue(
      "context.entries",
      () => this.#generated.listDataContext(),
    );
    if (!Array.isArray(value)) {
      throw createSdkError(errorCode.generatedProtocolError, "context.entries");
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

  async contextStats(): Promise<ContextDataStats> {
    const value = this.#generatedValue(
      "context.stats",
      () => this.#generated.getDataContextStats(),
    );
    return copyGeneratedDataStats(value);
  }

  #generatedTag(tag: string): string {
    return LOG_KIND_SET.has(tag)
      ? `${tag[0]?.toUpperCase()}${tag.slice(1)}`
      : tag.toUpperCase();
  }

  #logEntries(
    value: unknown,
    operation: "logs.find" | "logs.drain",
  ): readonly CedarlingLogEntry[] {
    if (!Array.isArray(value)) {
      throw createSdkError(errorCode.generatedProtocolError, operation);
    }
    return value.map((entry) => normalizeGeneratedLog(entry, operation));
  }

  async logIds(): Promise<readonly string[]> {
    const value = this.#generatedValue(
      "logs.ids",
      () => this.#generated.getLogIds(),
    );
    if (
      !Array.isArray(value) ||
      !value.every((item) => typeof item === "string")
    ) {
      throw createSdkError(errorCode.generatedProtocolError, "logs.ids");
    }
    return [...value];
  }

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
      const value = this.#generatedValue(
        "logs.find",
        () => this.#generated.getLogsByTag(this.#generatedTag(query.tag!)),
      );
      return this.#logEntries(value, "logs.find");
    }

    if (query === undefined) {
      const entries: CedarlingLogEntry[] = [];
      for (const id of await this.logIds()) {
        const value = this.#generatedValue(
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
      const value = this.#generatedValue(
        "logs.find",
        () => this.#generated.getLogById(query.id),
      );
      return value === null || value === undefined
        ? []
        : [normalizeGeneratedLog(value, "logs.find")];
    }

    if (query.requestId === undefined) {
      throw createSdkError(errorCode.generatedProtocolError, "logs.find");
    }
    const value = this.#generatedValue(
      "logs.find",
      () =>
        query.tag === undefined
          ? this.#generated.getLogsByRequestId(query.requestId)
          : this.#generated.getLogsByRequestIdAndTag(
              query.requestId,
              this.#generatedTag(query.tag),
            ),
    );
    return this.#logEntries(value, "logs.find");
  }

  async drainLogs(): Promise<readonly CedarlingLogEntry[]> {
    const value = this.#generatedValue(
      "logs.drain",
      () => this.#generated.popLogs(),
    );
    return this.#logEntries(value, "logs.drain");
  }

  async #authorize(
    operation: "authorizeUnsigned" | "authorizeMultiIssuer",
    invoke: () => Promise<unknown>,
  ): Promise<AuthorizationDecision> {
    let generatedValue: unknown;
    try {
      generatedValue = await invoke();
    } catch (error: unknown) {
      throw createSdkError(errorCode.authorizationFailed, operation, {
        rawCause: error,
      });
    }

    const generatedResult = adaptGeneratedResult(
      generatedValue,
      operation,
    );
    if (generatedResult === undefined) {
      throw createSdkError(
        errorCode.generatedProtocolError,
        operation,
      );
    }

    return withGeneratedWrapper(generatedResult, operation, () => {
      let serialized: unknown;
      try {
        serialized = generatedResult.jsonString();
      } catch (error: unknown) {
        if (isSdkErrorCode(error, [errorCode.generatedProtocolError])) {
          throw error;
        }
        throw createSdkError(
          errorCode.resultConversionFailed,
          operation,
          { rawCause: error },
        );
      }

      if (typeof serialized !== "string") {
        throw createSdkError(
          errorCode.generatedProtocolError,
          operation,
        );
      }

      return toAuthorizationDecision(
        parseGeneratedResult(serialized, operation),
      );
    });
  }

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
      throw createSdkError(errorCode.lifecycleFailed, "shutDown", {
        rawCause: failures.length === 1 ? failures[0] : Object.freeze(failures),
      });
    }
  }
}

/**
 * Adapts one unknown generated wrapper into the host-independent Engine.
 *
 * Runtime Adapters remain responsible for module loading and construction.
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
