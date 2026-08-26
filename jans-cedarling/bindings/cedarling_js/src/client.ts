import type {
  AuthorizeResult as GeneratedAuthorizeResult,
  BatchAuthorizeMultiIssuerResponse,
  BatchAuthorizeUnsignedResponse,
  BatchItemError as GeneratedBatchItemError,
  BatchItemMultiIssuerResult,
  BatchItemUnsignedResult,
  Cedarling as GeneratedCedarling,
  DataEntry as GeneratedDataEntry,
  DataStoreStats as GeneratedDataStoreStats,
  MultiIssuerAuthorizeResult,
} from "@janssenproject/cedarling_wasm";

import type {
  AuthorizationResult,
  BatchAuthorizationResult,
  BatchItemAuthorizationResult,
  BatchItemError,
  BootstrapProperties,
  Cedarling,
  CedarlingApi,
} from "./types.js";

type Releasable = { free(): void };
type JsonResult = Releasable & { json_string(): string };

interface GeneratedInitializers {
  readonly init: (
    properties: BootstrapProperties,
  ) => Promise<GeneratedCedarling>;
  readonly initFromArchiveBytes: (
    properties: BootstrapProperties,
    archiveBytes: Uint8Array,
  ) => Promise<GeneratedCedarling>;
}

function release<T extends Releasable, Result>(
  value: T,
  copy: (value: T) => Result,
): Result {
  let result: Result | undefined;
  let failure: unknown;
  try {
    result = copy(value);
  } catch (error: unknown) {
    failure = error;
  }
  try {
    value.free();
  } catch (error: unknown) {
    if (failure === undefined) throw error;
  }
  if (failure !== undefined) throw failure;
  return result as Result;
}

function copyPlain<Value>(value: Value): Value {
  return structuredClone(value);
}

function copyJson<Result>(value: JsonResult): Result {
  return release(
    value,
    (generated) => JSON.parse(generated.json_string()) as Result,
  );
}

function copyAuthorization(
  value: GeneratedAuthorizeResult | MultiIssuerAuthorizeResult,
): AuthorizationResult {
  return copyJson<AuthorizationResult>(value);
}

function copyDataEntry(value: GeneratedDataEntry): Record<string, unknown> {
  return copyJson<Record<string, unknown>>(value);
}

function copyDataStoreStats(
  value: GeneratedDataStoreStats,
): Record<string, unknown> {
  return copyJson<Record<string, unknown>>(value);
}

function copyBatchError(value: GeneratedBatchItemError): BatchItemError {
  return release(value, (generated) => ({
    category: generated.category,
    item_index: generated.item_index,
    message: generated.message,
  }));
}

function copyUnsignedBatchItem(
  value: BatchItemUnsignedResult,
): BatchItemAuthorizationResult {
  return release(value, (generated) => {
    if (generated.is_ok) {
      return { is_ok: true, result: copyAuthorization(generated.unwrap()) };
    }
    return {
      is_ok: false,
      error:
        generated.error === undefined
          ? undefined
          : copyBatchError(generated.error),
    };
  });
}

function copyMultiIssuerBatchItem(
  value: BatchItemMultiIssuerResult,
): BatchItemAuthorizationResult {
  return release(value, (generated) => {
    if (generated.is_ok) {
      return { is_ok: true, result: copyAuthorization(generated.unwrap()) };
    }
    return {
      is_ok: false,
      error:
        generated.error === undefined
          ? undefined
          : copyBatchError(generated.error),
    };
  });
}

function copyUnsignedBatch(
  value: BatchAuthorizeUnsignedResponse,
): BatchAuthorizationResult {
  return release(value, (generated) => ({
    batch_id: generated.batch_id,
    results: generated.results.map(copyUnsignedBatchItem),
  }));
}

function copyMultiIssuerBatch(
  value: BatchAuthorizeMultiIssuerResponse,
): BatchAuthorizationResult {
  return release(value, (generated) => ({
    batch_id: generated.batch_id,
    results: generated.results.map(copyMultiIssuerBatchItem),
  }));
}

class CedarlingClient implements Cedarling {
  readonly #generated: GeneratedCedarling;
  #inFlight = 0;
  #idle: Promise<void> | undefined;
  #resolveIdle: (() => void) | undefined;
  #shutdown: Promise<void> | undefined;

  constructor(generated: GeneratedCedarling) {
    this.#generated = generated;
  }

  #runAuthorization<Result>(operation: () => Promise<Result>): Promise<Result> {
    this.#inFlight += 1;
    return operation().finally(() => {
      this.#inFlight -= 1;
      if (this.#inFlight === 0) this.#resolveIdle?.();
    });
  }

  #waitUntilIdle(): Promise<void> {
    if (this.#inFlight === 0) return Promise.resolve();
    if (this.#idle === undefined) {
      this.#idle = new Promise<void>((resolve) => {
        this.#resolveIdle = resolve;
      });
    }
    return this.#idle;
  }

  annotationValues(policyIds: string[], key: string): readonly string[] {
    return copyPlain(this.#generated.annotation_values(policyIds, key));
  }

  annotationsByPolicy(policyIds: string[]): unknown {
    return copyPlain(this.#generated.annotations_by_policy(policyIds));
  }

  annotationsMap(policyIds: string[]): unknown {
    return copyPlain(this.#generated.annotations_map(policyIds));
  }

  authorizeMultiIssuer(request: string): Promise<AuthorizationResult> {
    return this.#runAuthorization(async () =>
      copyAuthorization(await this.#generated.authorize_multi_issuer(request)),
    );
  }

  authorizeMultiIssuerBatch(
    request: string,
  ): Promise<BatchAuthorizationResult> {
    return this.#runAuthorization(async () =>
      copyMultiIssuerBatch(
        await this.#generated.authorize_multi_issuer_batch(request),
      ),
    );
  }

  authorizeUnsigned(request: string): Promise<AuthorizationResult> {
    return this.#runAuthorization(async () =>
      copyAuthorization(await this.#generated.authorize_unsigned(request)),
    );
  }

  authorizeUnsignedBatch(request: string): Promise<BatchAuthorizationResult> {
    return this.#runAuthorization(async () =>
      copyUnsignedBatch(
        await this.#generated.authorize_unsigned_batch(request),
      ),
    );
  }

  clearDataCtx(): void {
    this.#generated.clear_data_ctx();
  }

  failedTrustedIssuerIds(): readonly unknown[] {
    return copyPlain(this.#generated.failed_trusted_issuer_ids());
  }

  getDataCtx(key: string): unknown {
    return copyPlain(this.#generated.get_data_ctx(key));
  }

  getDataEntryCtx(key: string): Record<string, unknown> | null | undefined {
    const entry = this.#generated.get_data_entry_ctx(key);
    return entry === undefined || entry === null ? entry : copyDataEntry(entry);
  }

  getLogById(id: string): unknown {
    return copyPlain(this.#generated.get_log_by_id(id));
  }

  getLogIds(): readonly unknown[] {
    return copyPlain(this.#generated.get_log_ids());
  }

  getLogsByRequestId(requestId: string): readonly unknown[] {
    return copyPlain(this.#generated.get_logs_by_request_id(requestId));
  }

  getLogsByRequestIdAndTag(requestId: string, tag: string): readonly unknown[] {
    return copyPlain(
      this.#generated.get_logs_by_request_id_and_tag(requestId, tag),
    );
  }

  getLogsByTag(tag: string): readonly unknown[] {
    return copyPlain(this.#generated.get_logs_by_tag(tag));
  }

  getStatsCtx(): Record<string, unknown> {
    return copyDataStoreStats(this.#generated.get_stats_ctx());
  }

  totalIssuers(): number {
    return this.#generated.total_issuers();
  }

  isTrustedIssuerLoadedByIss(issClaim: string): boolean {
    return this.#generated.is_trusted_issuer_loaded_by_iss(issClaim);
  }

  isTrustedIssuerLoadedByName(issuerId: string): boolean {
    return this.#generated.is_trusted_issuer_loaded_by_name(issuerId);
  }

  listDataCtx(): readonly Record<string, unknown>[] {
    return this.#generated
      .list_data_ctx()
      .map((entry) => copyDataEntry(entry as GeneratedDataEntry));
  }

  loadedTrustedIssuerIds(): readonly unknown[] {
    return copyPlain(this.#generated.loaded_trusted_issuer_ids());
  }

  loadedTrustedIssuersCount(): number {
    return this.#generated.loaded_trusted_issuers_count();
  }

  popLogs(): readonly unknown[] {
    return copyPlain(this.#generated.pop_logs());
  }

  pushDataCtx(key: string, value: unknown, ttlSecs?: bigint | null): void {
    this.#generated.push_data_ctx(key, value, ttlSecs);
  }

  removeDataCtx(key: string): boolean {
    return this.#generated.remove_data_ctx(key);
  }

  shutDown(): Promise<void> {
    if (this.#shutdown === undefined) this.#shutdown = this.#close();
    return this.#shutdown;
  }

  async #close(): Promise<void> {
    await this.#waitUntilIdle();
    let shutdownFailure: unknown;
    try {
      await this.#generated.shut_down();
    } catch (error: unknown) {
      shutdownFailure = error;
    }
    try {
      this.#generated.free();
    } catch (error: unknown) {
      if (shutdownFailure === undefined) throw error;
    }
    if (shutdownFailure !== undefined) throw shutdownFailure;
  }
}

/** @internal Builds testable public initialization functions around generated WASM. */
export function createCedarlingApi(
  initializeModule: () => Promise<void>,
  initializers: GeneratedInitializers,
): CedarlingApi {
  let initialized: Promise<void> | undefined;
  async function ready(): Promise<void> {
    if (initialized === undefined) {
      const attempt = initializeModule();
      initialized = attempt.catch((error: unknown) => {
        initialized = undefined;
        throw error;
      });
    }
    return initialized;
  }
  return Object.freeze({
    async init(properties: BootstrapProperties): Promise<Cedarling> {
      await ready();
      return new CedarlingClient(await initializers.init(properties));
    },
    async initFromArchiveBytes(
      properties: BootstrapProperties,
      archiveBytes: Uint8Array,
    ): Promise<Cedarling> {
      await ready();
      return new CedarlingClient(
        await initializers.initFromArchiveBytes(properties, archiveBytes),
      );
    },
  });
}
