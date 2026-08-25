import type {
  AuthorizationDecision,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import type { PreparedEngineOptions } from "../configuration/prepare.js";
import type { CedarlingLogEntry, LogQuery } from "../logs/types.js";
import type { ContextDataValue } from "../values/types.js";
import type { ContextDataEntry, ContextDataStats } from "../context/types.js";
import type { IssuerReference } from "../issuers/types.js";

/**
 * Private runtime-neutral Seam implemented by generated engines.
 *
 * Inputs have already been validated and detached, outputs are SDK-owned, and
 * generated WASM classes and values never cross this Interface.
 *
 * @internal
 */
export interface CedarlingEngine {
  isIssuerLoaded(issuer: IssuerReference): Promise<boolean>;

  setContext(
    key: string,
    value: ContextDataValue,
    ttlSeconds?: number,
  ): Promise<void>;

  getContext(key: string): Promise<ContextDataValue | undefined>;

  getContextEntry(key: string): Promise<ContextDataEntry | undefined>;

  deleteContext(key: string): Promise<boolean>;
  clearContext(): Promise<void>;

  contextEntries(): Promise<readonly ContextDataEntry[]>;

  contextStats(): Promise<ContextDataStats>;

  logIds(): Promise<readonly string[]>;

  findLogs(query?: LogQuery): Promise<readonly CedarlingLogEntry[]>;

  drainLogs(): Promise<readonly CedarlingLogEntry[]>;

  authorizeUnsigned(
    request: UnsignedAuthorizationRequest,
  ): Promise<AuthorizationDecision>;

  authorizeMultiIssuer(
    request: MultiIssuerAuthorizationRequest,
  ): Promise<AuthorizationDecision>;

  shutDown(): Promise<void>;
}

/**
 * Creates one private engine from detached public options.
 *
 * Private runtime entries bind this Seam to one Runtime Adapter. It is not a
 * consumer-extension contract.
 *
 * @internal
 */
export type EngineFactory = (
  options: PreparedEngineOptions,
) => Promise<CedarlingEngine>;
