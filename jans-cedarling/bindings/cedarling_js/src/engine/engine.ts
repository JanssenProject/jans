import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { PreparedCedarlingOptions } from "../configuration/prepare.js";
import type { CedarlingLogEntry, LogQuery } from "../logs/types.js";
import type { ContextDataValue } from "../values/types.js";
import type {
  ContextDataEntry,
  ContextDataStats,
} from "../context/types.js";
import type { IssuerReference } from "../issuers/types.js";

/**
 * Private boundary implemented by the generated Web engine.
 *
 * Public clients depend only on this interface and never expose generated WASM
 * classes or generated values.
 *
 * @internal
 */
export interface CedarlingEngine {
  /** Observes current readiness for one validated issuer reference. */
  isIssuerLoaded(issuer: IssuerReference): Promise<boolean>;

  /** Stores one validated, detached context-data value. */
  setContext(
    key: string,
    value: ContextDataValue,
    ttlSeconds?: number,
  ): Promise<void>;

  /** Reads and detaches one context-data value when present. */
  getContext(key: string): Promise<ContextDataValue | undefined>;

  /** Reads one context-data value and its generated metadata. */
  getContextEntry(key: string): Promise<ContextDataEntry | undefined>;

  /** Removes one context-data entry and reports whether it existed. */
  deleteContext(key: string): Promise<boolean>;

  /** Removes every generated context-data entry. */
  clearContext(): Promise<void>;

  /** Lists current context-data entries and detaches their metadata. */
  contextEntries(): Promise<readonly ContextDataEntry[]>;

  /** Reads and detaches generated context-store statistics. */
  contextStats(): Promise<ContextDataStats>;

  /** Enumerates physically retained generated log identifiers. */
  logIds(): Promise<readonly string[]>;

  /** Finds and normalizes retained entries selected by a validated query. */
  findLogs(query?: LogQuery): Promise<readonly CedarlingLogEntry[]>;

  /** Returns and removes all physically retained generated log entries. */
  drainLogs(): Promise<readonly CedarlingLogEntry[]>;

  /** Evaluates a previously validated unsigned request. */
  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationDecision>;

  /** Evaluates a previously validated multi-issuer request. */
  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationDecision>;

  /** Shuts down the generated client and releases its WASM wrapper. */
  shutDown(): Promise<void>;
}

/**
 * Creates one private engine from detached public options.
 *
 * The package root has exactly one production implementation:
 * `createWebEngine`. This type is a package composition boundary, not a
 * runtime-adapter or consumer-extension contract.
 *
 * @internal
 */
export type EngineFactory = (
  options: PreparedCedarlingOptions,
) => Promise<CedarlingEngine>;
