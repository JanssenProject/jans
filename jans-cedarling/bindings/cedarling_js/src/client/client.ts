import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { CedarlingClient } from "./types.js";
import type { CedarlingOptions } from "../configuration/types.js";
import type {
  CedarlingError,
  CedarlingOperation,
  Result,
} from "../errors/types.js";
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
import { errorCode } from "../errors/types.js";
import {
  prepareCedarlingOptions,
  type PreparedClientCapabilities,
} from "../configuration/prepare.js";

const ok = <T>(value: T): Result<T> => ({ ok: true, value });
const failed = (error: CedarlingError): Result<never> => ({ ok: false, error });

class CedarlingClientImplementation implements CedarlingClient {
  readonly #engine: CedarlingEngine;
  readonly #capabilities: PreparedClientCapabilities;
  #open = true;
  #inFlight = 0;
  #idle:
    | { readonly promise: Promise<void>; readonly resolve: () => void }
    | undefined;
  #shutDownResult: Promise<Result<void>> | undefined;

  constructor(
    engine: CedarlingEngine,
    capabilities: PreparedClientCapabilities,
  ) {
    this.#engine = engine;
    this.#capabilities = capabilities;
  }

  async #run<T, Prepared>(
    operation: CedarlingOperation,
    prepare: () => Prepared,
    work: (prepared: Prepared) => Promise<T>,
  ): Promise<Result<T>> {
    if (!this.#open) {
      return failed(createSdkError(errorCode.clientClosed, operation));
    }
    let prepared: Prepared;
    try {
      prepared = prepare();
    } catch (error: unknown) {
      return failed(normalizeInputError(error, operation));
    }
    this.#inFlight += 1;
    try {
      return ok(await work(prepared));
    } catch (error: unknown) {
      return failed(normalizeOperationError(
        error,
        operation,
        this.#capabilities.exposeRawErrors,
      ));
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
    return this.#run(
      "authorizeUnsigned",
      () => snapshotUnsignedRequest(request),
      (snapshot) => this.#engine.authorizeUnsigned(snapshot),
    );
  }

  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<Result<AuthorizationDecision>> {
    return this.#run(
      "authorizeMultiIssuer",
      () => snapshotMultiIssuerRequest(request),
      (snapshot) => this.#engine.authorizeMultiIssuer(snapshot),
    );
  }

  #waitUntilIdle(): Promise<void> {
    if (this.#inFlight === 0) return Promise.resolve();
    if (this.#idle === undefined) {
      let resolveIdle: (() => void) | undefined;
      const promise = new Promise<void>((resolve) => {
        resolveIdle = resolve;
      });
      this.#idle = { promise, resolve: () => resolveIdle?.() };
    }
    return this.#idle.promise;
  }

  async #finishShutDown(): Promise<Result<void>> {
    try {
      await this.#waitUntilIdle();
      await this.#engine.shutDown();
      return ok(undefined);
    } catch (error: unknown) {
      return failed(normalizeOperationError(
        error,
        "shutDown",
        this.#capabilities.exposeRawErrors,
      ));
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

export function createClientForEngine(
  engine: CedarlingEngine,
  capabilities: Partial<PreparedClientCapabilities> = {},
): CedarlingClient {
  return new CedarlingClientImplementation(engine, {
    exposeRawErrors: capabilities.exposeRawErrors ?? false,
  });
}

export function createCedarlingForEngine(
  createEngine: EngineFactory,
): (options: CedarlingOptions) => Promise<Result<CedarlingClient>> {
  return async function createCedarling(options: CedarlingOptions) {
    let snapshot;
    try {
      snapshot = prepareCedarlingOptions(options);
    } catch (error: unknown) {
      return failed(normalizeInputError(error, "initialize"));
    }
    try {
      return ok(createClientForEngine(
        await createEngine(snapshot),
        snapshot.clientCapabilities,
      ));
    } catch (error: unknown) {
      return failed(normalizeOperationError(
        error,
        "initialize",
        snapshot.clientCapabilities.exposeRawErrors,
      ));
    }
  };
}
