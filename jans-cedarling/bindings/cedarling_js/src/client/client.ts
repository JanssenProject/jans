import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { CedarlingClient } from "./types.js";
import type { CedarlingOptions } from "../configuration/types.js";
import {
  errorCode,
  type CedarlingError,
  type CedarlingOperation,
  type Result,
} from "../errors/types.js";
import { DEFAULTS } from "../helpers/constants.js";
import {
  snapshotMultiIssuerRequest,
  snapshotUnsignedRequest,
} from "../authorization/request.js";
import type { CedarlingEngine, EngineFactory } from "../engine/engine.js";
import {
  createSdkError,
  normalizeInputError,
  normalizeOperationError,
} from "../errors/errors.js";
import {
  prepareCedarlingOptions,
  type PreparedClientCapabilities,
} from "../configuration/prepare.js";
import type { CedarlingLogs } from "../logs/types.js";
import { snapshotLogQuery } from "../logs/query.js";
import type { CedarlingContext, ContextSetOptions } from "../context/types.js";
import {
  snapshotContextKey,
  snapshotContextSet,
} from "../context/input.js";
import type { ContextDataValue } from "../values/types.js";
import type { CedarlingIssuers } from "../issuers/types.js";
import { snapshotIssuerReference } from "../issuers/input.js";

function ok<T>(value: T): Result<T> {
  return { ok: true, value };
}

function failed(error: CedarlingError): Result<never> {
  return { ok: false, error };
}

/**
 * Private client facade that keeps the generated engine and its lifecycle out
 * of the public package surface.
 */
class CedarlingClientImplementation implements CedarlingClient {
  readonly #engine: CedarlingEngine;

  readonly #capabilities: PreparedClientCapabilities;

  readonly logs: CedarlingLogs;

  readonly context: CedarlingContext;

  readonly issuers: CedarlingIssuers;

  #open = true;

  #inFlight = 0;

  #idle: {
    readonly promise: Promise<void>;
    readonly resolve: () => void;
  } | undefined;

  #shutDownResult: Promise<Result<void>> | undefined;

  constructor(
    engine: CedarlingEngine,
    capabilities: PreparedClientCapabilities,
  ) {
    this.#engine = engine;
    this.#capabilities = capabilities;
    this.issuers = Object.freeze({
      isLoaded: this.#bindOperation(
        "issuers.isLoaded",
        snapshotIssuerReference,
        (issuer) => this.#engine.isIssuerLoaded(issuer),
      ),
    });
    this.context = Object.freeze({
      set: this.#bindOperation(
        "context.set",
        (
          key: string,
          value: ContextDataValue,
          options?: ContextSetOptions,
        ) =>
          snapshotContextSet(
            key,
            value,
            options,
            this.#capabilities.contextMaxTtlSeconds,
          ),
        (snapshot) =>
          this.#engine.setContext(
            snapshot.key,
            snapshot.value,
            snapshot.ttlSeconds,
          ),
      ),
      get: this.#bindContextKeyOperation(
        "context.get",
        (key) => this.#engine.getContext(key),
      ),
      getEntry: this.#bindContextKeyOperation(
        "context.getEntry",
        (key) => this.#engine.getContextEntry(key),
      ),
      delete: this.#bindContextKeyOperation(
        "context.delete",
        (key) => this.#engine.deleteContext(key),
      ),
      clear: this.#bindDirectOperation("context.clear", () => this.#engine.clearContext()),
      entries: this.#bindDirectOperation("context.entries", () => this.#engine.contextEntries()),
      stats: this.#bindDirectOperation("context.stats", () => this.#engine.contextStats()),
    });
    this.logs = Object.freeze({
      ids: this.#bindDirectOperation("logs.ids", () => this.#engine.logIds()),
      find: this.#bindOperation(
        "logs.find",
        snapshotLogQuery,
        (query) => this.#engine.findLogs(query),
      ),
      drain: this.#bindDirectOperation("logs.drain", () => this.#engine.drainLogs()),
    });
  }

  #bindOperation<Args extends unknown[], Prepared, T>(
    operation: CedarlingOperation,
    prepare: (...args: Args) => Prepared,
    work: (prepared: Prepared) => Promise<T>,
  ): (...args: Args) => Promise<Result<T>> {
    return (...args) =>
      this.#runPreparedOperation(operation, () => prepare(...args), work);
  }

  #bindDirectOperation<T>(
    operation: CedarlingOperation,
    work: () => Promise<T>,
  ): () => Promise<Result<T>> {
    return () => this.#runPreparedOperation(operation, () => undefined, work);
  }

  #bindContextKeyOperation<T>(
    operation: Extract<
      CedarlingOperation,
      "context.get" | "context.getEntry" | "context.delete"
    >,
    work: (key: string) => Promise<T>,
  ): (key: string) => Promise<Result<T>> {
    return this.#bindOperation(
      operation,
      (key: string) => snapshotContextKey(key, operation),
      work,
    );
  }

  /** Admits, snapshots, executes, and normalizes one operation. */
  #runPreparedOperation<T, Prepared>(
    operation: CedarlingOperation,
    prepare: () => Prepared,
    work: (prepared: Prepared) => Promise<T>,
  ): Promise<Result<T>> {
    return this.#runWhileOpen(
      operation,
      async () => {
        if (operation.startsWith("logs.") && !this.#capabilities.memoryLogging) {
          return failed(createSdkError(errorCode.logStorageUnavailable, operation));
        }

        let prepared: Prepared;
        try {
          prepared = prepare();
        } catch (error: unknown) {
          return failed(normalizeInputError(error, operation));
        }
        try {
          return ok(await work(prepared));
        } catch (error: unknown) {
          return failed(
            normalizeOperationError(
              error,
              operation,
              this.#capabilities.exposeRawErrors,
            ),
          );
        }
      },
    );
  }

  async #runWhileOpen<T>(
    operation: CedarlingOperation,
    work: () => Promise<Result<T>>,
  ): Promise<Result<T>> {
    if (!this.#open) {
      return failed(createSdkError(errorCode.clientClosed, operation));
    }

    this.#inFlight += 1;
    try {
      return await work();
    } finally {
      this.#inFlight -= 1;
      if (this.#inFlight === 0 && this.#idle !== undefined) {
        const idle = this.#idle;
        this.#idle = undefined;
        idle.resolve();
      }
    }
  }

  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>> {
    return this.#runPreparedOperation(
      "authorizeUnsigned",
      () => snapshotUnsignedRequest(request),
      (snapshot) => this.#engine.authorizeUnsigned(snapshot),
    );
  }

  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>> {
    return this.#runPreparedOperation(
      "authorizeMultiIssuer",
      () => snapshotMultiIssuerRequest(request),
      (snapshot) => this.#engine.authorizeMultiIssuer(snapshot),
    );
  }

  #waitUntilIdle(): Promise<void> {
    if (this.#inFlight === 0) {
      return Promise.resolve();
    }
    if (this.#idle === undefined) {
      let resolveIdle: (() => void) | undefined;
      const promise = new Promise<void>((resolve) => {
        resolveIdle = resolve;
      });
      this.#idle = {
        promise,
        resolve: () => resolveIdle?.(),
      };
    }
    return this.#idle.promise;
  }

  async #finishShutDown(): Promise<Result<void>> {
    try {
      await this.#waitUntilIdle();
      await this.#engine.shutDown();
      return ok(undefined);
    } catch (error: unknown) {
      return failed(
        normalizeOperationError(
          error,
          "shutDown",
          this.#capabilities.exposeRawErrors,
        ),
      );
    }
  }

  shutDown(): Promise<Result<void>> {
    if (this.#shutDownResult === undefined) {
      this.#open = false;
      this.#shutDownResult = this.#finishShutDown();
    }
    return this.#shutDownResult;
  }
}

/** @internal */
export function createClientForEngine(
  engine: CedarlingEngine,
  capabilities: Partial<PreparedClientCapabilities> = {},
): CedarlingClient {
  return new CedarlingClientImplementation(engine, {
    exposeRawErrors: capabilities.exposeRawErrors ??
      DEFAULTS.client.exposeRawErrors,
    memoryLogging: capabilities.memoryLogging ?? DEFAULTS.client.memoryLogging,
    contextMaxTtlSeconds: capabilities.contextMaxTtlSeconds ??
      DEFAULTS.contextStore.maxTtlSeconds,
  });
}

/** @internal */
export function createCedarlingForEngine(
  createEngine: EngineFactory,
): (
  options: CedarlingOptions,
) => Promise<Result<CedarlingClient>> {
  return async function createCedarling(
    options: CedarlingOptions,
  ): Promise<Result<CedarlingClient>> {
    let snapshot;

    try {
      snapshot = prepareCedarlingOptions(options);
    } catch (error: unknown) {
      return failed(normalizeInputError(error, "initialize"));
    }

    try {
      return ok(
        createClientForEngine(
          await createEngine(snapshot),
          snapshot.clientCapabilities,
        ),
      );
    } catch (error: unknown) {
      return failed(
        normalizeOperationError(
          error,
          "initialize",
          snapshot.clientCapabilities.exposeRawErrors,
        ),
      );
    }
  };
}
