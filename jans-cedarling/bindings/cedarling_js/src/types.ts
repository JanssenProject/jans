/** Cedarling bootstrap properties accepted by `init` and `initFromArchiveBytes`. */
export type BootstrapProperties = Readonly<Record<string, unknown>>;

/** A Cedarling policy-evaluation diagnostic returned with a decision. */
export interface PolicyEvaluationDiagnostic {
  readonly id: string;
  readonly error: string;
}

/** A completed Cedar authorization decision and its diagnostics. */
export interface AuthorizationResult {
  readonly decision: boolean;
  readonly request_id: string;
  readonly response: {
    readonly decision: boolean;
    readonly diagnostics: {
      readonly reason: readonly string[];
      readonly errors: readonly PolicyEvaluationDiagnostic[];
    };
  };
}

/** A per-item authorization failure in a batch response. */
export interface BatchItemError {
  readonly category: string;
  readonly item_index: number;
  readonly message: string;
}

/** One batch item: a decision or a per-item failure. */
export type BatchItemAuthorizationResult =
  | {
    readonly is_ok: true;
    readonly result: AuthorizationResult;
  }
  | {
    readonly is_ok: false;
    readonly error: BatchItemError | undefined;
  };

/** Ordered results for one batch authorization request. */
export interface BatchAuthorizationResult {
  readonly batch_id: string;
  readonly results: readonly BatchItemAuthorizationResult[];
}

/** A Cedarling client returned by `init` or `initFromArchiveBytes`. */
export interface Cedarling {
  annotationValues(policyIds: string[], key: string): readonly string[];
  annotationsByPolicy(policyIds: string[]): unknown;
  annotationsMap(policyIds: string[]): unknown;
  /** Authorizes a token-mapping request encoded as JSON. */
  authorizeMultiIssuer(request: string): Promise<AuthorizationResult>;
  /** Authorizes a batch of token-mapping requests encoded as JSON. */
  authorizeMultiIssuerBatch(request: string): Promise<BatchAuthorizationResult>;
  /** Authorizes an application-asserted request encoded as JSON. */
  authorizeUnsigned(request: string): Promise<AuthorizationResult>;
  /** Authorizes a batch of application-asserted requests encoded as JSON. */
  authorizeUnsignedBatch(request: string): Promise<BatchAuthorizationResult>;
  clearDataCtx(): void;
  failedTrustedIssuerIds(): readonly unknown[];
  getDataCtx(key: string): unknown;
  /** Returns the context entry and its metadata for `key`, if one is available. */
  getDataEntryCtx(key: string): Record<string, unknown> | null | undefined;
  getLogById(id: string): unknown;
  getLogIds(): readonly unknown[];
  getLogsByRequestId(requestId: string): readonly unknown[];
  getLogsByRequestIdAndTag(requestId: string, tag: string): readonly unknown[];
  getLogsByTag(tag: string): readonly unknown[];
  getStatsCtx(): Record<string, unknown>;
  isTrustedIssuerLoadedByIss(issClaim: string): boolean;
  isTrustedIssuerLoadedByName(issuerId: string): boolean;
  listDataCtx(): readonly Record<string, unknown>[];
  loadedTrustedIssuerIds(): readonly unknown[];
  loadedTrustedIssuersCount(): number;
  /** Returns and removes all retained logs. */
  popLogs(): readonly unknown[];
  /** Stores `value` under `key`, optionally expiring it after `ttlSecs` seconds. */
  pushDataCtx(key: string, value: unknown, ttlSecs?: bigint | null): void;
  removeDataCtx(key: string): boolean;
  /** Waits for active authorization calls, then closes this client. */
  shutDown(): Promise<void>;
  totalIssuers(): number;
}

/** Functions for creating a Cedarling client. */
export interface CedarlingApi {
  /** Initializes Cedarling from raw bootstrap properties. */
  init(properties: BootstrapProperties): Promise<Cedarling>;
  /**
   * Initializes Cedarling from raw bootstrap properties and Cedar Archive bytes.
   */
  initFromArchiveBytes(properties: BootstrapProperties, archiveBytes: Uint8Array): Promise<Cedarling>;
}
