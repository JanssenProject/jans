import type {
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { CedarlingClient } from "./types.js";
import type { CedarlingOptions } from "../configuration/types.js";
import type {
  CedarlingAuthorizationError,
  CedarlingError,
  CedarlingInitializationError,
  CedarlingLifecycleError,
  CedarlingLogError,
  CedarlingOperation,
  AuthorizationResult,
  Result,
} from "../errors/types.js";
import {
  DEFAULTS,
  OPERATION_ERROR_POLICIES,
  type OperationErrorPolicy,
} from "../helpers/constants.js";
import {
  snapshotMultiIssuerRequest,
  snapshotUnsignedRequest,
} from "../authorization/request.js";
import type { CedarlingEngine, EngineFactory } from "../engine/engine.js";
import {
  createSdkError,
  exposeSdkErrorCause,
  isSdkErrorCode,
  validationIssuesAt,
} from "../errors/errors.js";
import {
  prepareCedarlingOptions,
  type PreparedClientCapabilities,
} from "../configuration/prepare.js";
import type {
  CedarlingLogs,
  LogQuery,
} from "../logs/types.js";
import { snapshotLogQuery } from "../logs/query.js";
import type {
  CedarlingContext,
  ContextSetOptions,
} from "../context/types.js";
import {
  snapshotContextKey,
  snapshotContextSet,
} from "../context/input.js";
import type { ContextDataValue } from "../values/types.js";
import type {
  CedarlingIssuers,
  IssuerReference,
} from "../issuers/types.js";
import { snapshotIssuerReference } from "../issuers/input.js";

/** Constructs the successful branch of a public SDK result. */
function ok<T>(value: T): Result<T, never> {
  return { ok: true, value };
}

/** Preserves allowlisted SDK failures and safely normalizes opaque failures. */
function normalizeOperationError<Policy extends OperationErrorPolicy>(
  error: unknown,
  operation: CedarlingOperation,
  policy: Policy,
  exposeRawErrors: boolean,
): CedarlingError<Policy[number]> {
  const normalized = isSdkErrorCode(error, policy)
    ? error
    : createSdkError(policy[0], operation, { rawCause: error });
  return exposeRawErrors ? exposeSdkErrorCause(normalized) : normalized;
}

/**
 * Private client facade that keeps the generated engine and its lifecycle out
 * of the public package surface.
 */
class CedarlingClientImplementation implements CedarlingClient {
  /** Isolated private Cedarling engine owned by this facade. */
  readonly #engine: CedarlingEngine;

  /** Capabilities fixed by validated initialization options. */
  readonly #capabilities: PreparedClientCapabilities;

  /** Public retained-log service sharing this client's lifecycle boundary. */
  readonly logs: CedarlingLogs;

  /** Public context-data service sharing this client's lifecycle boundary. */
  readonly context: CedarlingContext;

  /** Public trusted-issuer service sharing this client's lifecycle boundary. */
  readonly issuers: CedarlingIssuers;

  /** Lifecycle state used to reject work once shutdown begins. */
  #state: "open" | "closing" | "closed" = "open";

  /** Number of operations accepted while the facade was open. */
  #inFlight = 0;

  /** Shared waiter created only when shutdown must drain accepted operations. */
  #idle:
    | {
        readonly promise: Promise<void>;
        readonly resolve: () => void;
      }
    | undefined;

  /** Memoized shutdown promise and result, including a shutdown failure. */
  #shutDownResult:
    | Promise<Result<void, CedarlingLifecycleError>>
    | undefined;

  /** Creates one lifecycle facade around an isolated Cedarling engine. */
  constructor(
    engine: CedarlingEngine,
    capabilities: PreparedClientCapabilities,
  ) {
    this.#engine = engine;
    this.#capabilities = capabilities;
    this.issuers = Object.freeze({
      isLoaded: (issuer: IssuerReference) =>
        this.#runPreparedOperation(
          "issuers.isLoaded",
          () => snapshotIssuerReference(issuer),
          (snapshot) => this.#engine.isIssuerLoaded(snapshot),
          OPERATION_ERROR_POLICIES.issuer,
        ),
    });
    this.context = Object.freeze({
      set: (
        key: string,
        value: ContextDataValue,
        options?: ContextSetOptions,
      ) =>
        this.#runPreparedOperation(
          "context.set",
          () =>
            snapshotContextSet(
              key,
              value,
              options,
              this.#capabilities.contextMaxTtlSeconds,
            ),
          async (snapshot) => {
            await this.#engine.setContext(
              snapshot.key,
              snapshot.value,
              snapshot.ttlSeconds,
            );
          },
          OPERATION_ERROR_POLICIES.context,
        ),
      get: (key: string) =>
        this.#runPreparedOperation(
          "context.get",
          () => snapshotContextKey(key),
          (snapshotKey) => this.#engine.getContext(snapshotKey),
          OPERATION_ERROR_POLICIES.context,
        ),
      getEntry: (key: string) =>
        this.#runPreparedOperation(
          "context.getEntry",
          () => snapshotContextKey(key),
          (snapshotKey) => this.#engine.getContextEntry(snapshotKey),
          OPERATION_ERROR_POLICIES.context,
        ),
      delete: (key: string) =>
        this.#runPreparedOperation(
          "context.delete",
          () => snapshotContextKey(key),
          (snapshotKey) => this.#engine.deleteContext(snapshotKey),
          OPERATION_ERROR_POLICIES.context,
        ),
      clear: () =>
        this.#runPreparedOperation(
          "context.clear",
          () => undefined,
          () => this.#engine.clearContext(),
          OPERATION_ERROR_POLICIES.context,
        ),
      entries: () =>
        this.#runPreparedOperation(
          "context.entries",
          () => undefined,
          () => this.#engine.contextEntries(),
          OPERATION_ERROR_POLICIES.context,
        ),
      stats: () =>
        this.#runPreparedOperation(
          "context.stats",
          () => undefined,
          () => this.#engine.contextStats(),
          OPERATION_ERROR_POLICIES.context,
        ),
    });
    this.logs = Object.freeze({
      ids: () =>
        this.#runLogOperation(
          "logs.ids",
          () => undefined,
          () => this.#engine.logIds(),
        ),
      find: (query?: LogQuery) =>
        this.#runLogOperation(
          "logs.find",
          () => snapshotLogQuery(query),
          (snapshot) => this.#engine.findLogs(snapshot),
        ),
      drain: () =>
        this.#runLogOperation(
          "logs.drain",
          () => undefined,
          () => this.#engine.drainLogs(),
        ),
    });
  }

  /** Validates and executes one prepared operation through shared error rules. */
  #runPreparedOperation<
    T,
    Prepared,
    Policy extends OperationErrorPolicy,
  >(
    operation: CedarlingOperation,
    prepare: () => Prepared,
    work: (prepared: Prepared) => Promise<T>,
    policy: Policy,
  ): Promise<
    Result<
      T,
      CedarlingError<"INVALID_INPUT" | "CLIENT_CLOSED" | Policy[number]>
    >
  > {
    return this.#runWhileOpen<
      T,
      CedarlingError<"INVALID_INPUT" | "CLIENT_CLOSED" | Policy[number]>
    >(
      operation,
      async () => {
        let prepared: Prepared;
        try {
          prepared = prepare();
        } catch (error: unknown) {
          return {
            ok: false,
            error: createSdkError("INVALID_INPUT", operation, {
              issues: validationIssuesAt(error, []),
            }),
          };
        }
        try {
          return ok(await work(prepared));
        } catch (error: unknown) {
          return {
            ok: false,
            error: normalizeOperationError(
              error,
              operation,
              policy,
              this.#capabilities.exposeRawErrors,
            ),
          };
        }
      },
    );
  }

  /** Validates and runs one retained-log operation through shared guards. */
  #runLogOperation<T, Prepared>(
    operation: Extract<
      CedarlingOperation,
      "logs.ids" | "logs.find" | "logs.drain"
    >,
    prepare: () => Prepared,
    work: (prepared: Prepared) => Promise<T>,
  ): Promise<Result<T, CedarlingLogError>> {
    return this.#runPreparedOperation(
      operation,
      prepare,
      (prepared) => {
        if (!this.#capabilities.memoryLogging) {
          throw createSdkError("LOG_STORAGE_UNAVAILABLE", operation);
        }
        return work(prepared);
      },
      OPERATION_ERROR_POLICIES.log,
    );
  }

  /** Admits one operation and accounts for it until its work settles. */
  async #runWhileOpen<T, E extends CedarlingError>(
    operation: CedarlingOperation,
    work: () => Promise<Result<T, E>>,
  ): Promise<Result<T, E | CedarlingError<"CLIENT_CLOSED">>> {
    if (this.#state !== "open") {
      return {
        ok: false,
        error: createSdkError("CLIENT_CLOSED", operation),
      };
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

  /**
   * Accepts unsigned work only while open and accounts for it until settled.
   */
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>> {
    return this.#runPreparedOperation(
      "authorizeUnsigned",
      () => snapshotUnsignedRequest(request),
      (snapshot) => this.#engine.authorizeUnsigned(snapshot),
      OPERATION_ERROR_POLICIES.authorization,
    );
  }

  /** Validates, detaches, and evaluates one multi-issuer token request. */
  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationResult<CedarlingAuthorizationError>> {
    return this.#runPreparedOperation(
      "authorizeMultiIssuer",
      () => snapshotMultiIssuerRequest(request),
      (snapshot) => this.#engine.authorizeMultiIssuer(snapshot),
      OPERATION_ERROR_POLICIES.authorization,
    );
  }

  /** Returns one promise that resolves when all accepted work has settled. */
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

  /** Performs the single shutdown/disposal attempt shared by all shutdown calls. */
  async #finishShutDown(): Promise<Result<void, CedarlingLifecycleError>> {
    try {
      await this.#waitUntilIdle();
      await this.#engine.shutDown();
      return ok(undefined);
    } catch (error: unknown) {
      return {
        ok: false,
        error: normalizeOperationError(
          error,
          "shutDown",
          OPERATION_ERROR_POLICIES.lifecycle,
          this.#capabilities.exposeRawErrors,
        ),
      };
    } finally {
      this.#state = "closed";
    }
  }

  /** Atomically begins shutdown and returns the shared shutdown promise. */
  shutDown(): Promise<Result<void, CedarlingLifecycleError>> {
    if (this.#shutDownResult === undefined) {
      this.#state = "closing";
      this.#shutDownResult = this.#finishShutDown();
    }
    return this.#shutDownResult;
  }
}

/**
 * Creates the private lifecycle facade around one injected Cedarling engine.
 *
 * This package-internal composition seam does not select a runtime and is not
 * exported from the package root. Each production entry reaches it only after
 * its runtime-specific engine has been constructed; focused unit tests inject
 * the same private engine contract directly.
 *
 * @internal
 */
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

/**
 * Binds the public factory workflow to one private Cedarling engine factory.
 *
 * Every public runtime entry composes this seam with its private engine
 * factory. It neither detects nor selects runtimes and is not a consumer
 * extension point. Configuration validation and detachment always precede
 * engine construction; expected input, capability, WASM, initialization, and
 * generated-protocol failures become a discriminated {@link Result}.
 *
 * @param createEngine - Package-private Cedarling engine factory.
 * @returns The public asynchronous Cedarling factory.
 *
 * @internal
 */
export function createCedarlingForEngine(
  createEngine: EngineFactory,
): (
  options: CedarlingOptions,
) => Promise<Result<CedarlingClient, CedarlingInitializationError>> {
  return async function createCedarling(
    options: CedarlingOptions,
  ): Promise<Result<CedarlingClient, CedarlingInitializationError>> {
    let snapshot;

    try {
      snapshot = prepareCedarlingOptions(options);
    } catch (error: unknown) {
      return {
        ok: false,
        error: isSdkErrorCode(error, ["INVALID_INPUT"])
          ? error
          : createSdkError("INVALID_INPUT", "initialize", {
              issues: validationIssuesAt(error, []),
            }),
      };
    }

    try {
      return ok(
        createClientForEngine(
          await createEngine(snapshot),
          snapshot.clientCapabilities,
        ),
      );
    } catch (error: unknown) {
      return {
        ok: false,
        error: normalizeOperationError(
          error,
          "initialize",
          OPERATION_ERROR_POLICIES.initialization,
          snapshot.clientCapabilities.exposeRawErrors,
        ),
      };
    }
  };
}
