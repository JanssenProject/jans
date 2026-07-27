import type { LogLevel } from "../configuration/types.js";
import type { CedarlingLogError, Result } from "../errors/types.js";
import type { JsonObject } from "../values/types.js";

/**
 * Broad category assigned to one normalized Cedarling log entry.
 *
 * @example
 * ```ts
 * const kind: CedarlingLogKind = "decision";
 * ```
 */
export type CedarlingLogKind = "decision" | "system" | "metric";

/**
 * Tag accepted by retained-log queries.
 *
 * @example
 * ```ts
 * const tag: CedarlingLogTag = "warn";
 * ```
 */
export type CedarlingLogTag = CedarlingLogKind | LogLevel;

/**
 * Detached, normalized log entry owned by the JavaScript SDK.
 *
 * @example
 * ```ts
 * if (found.ok) console.log(found.value[0]?.payload);
 * ```
 */
export interface CedarlingLogEntry {
  /** Stable retained-log identifier. */
  readonly id: string;

  /** Authorization request identifier, when the source entry has one. */
  readonly requestId?: string;

  /** Source timestamp, when the generated entry supplies one. */
  readonly timestamp?: string;

  /** Normalized log category. */
  readonly kind: CedarlingLogKind;

  /** Normalized severity, when the source entry has one. */
  readonly level?: LogLevel;

  /** Cedarling policy-decision-point identifier. */
  readonly pdpId: string;

  /** Configured Cedarling application identifier, when present. */
  readonly applicationId?: string;

  /** Detached source fields outside the normalized envelope. */
  readonly payload: JsonObject;
}

/**
 * Exact retained-log query combinations supported by Cedarling.
 *
 * @example
 * ```ts
 * const query: LogQuery = { requestId, tag: "decision" };
 * ```
 */
export type LogQuery =
  | {
      /** Exact retained-log identifier. */
      readonly id: string;
      /** Excluded when the query selects an exact log ID. */
      readonly requestId?: never;
      /** Excluded when the query selects an exact log ID. */
      readonly tag?: never;
    }
  | {
      /** Authorization request identifier whose retained entries are returned. */
      readonly requestId: string;
      /** Optional Cedarling category or severity combined with the request. */
      readonly tag?: CedarlingLogTag;
      /** Excluded when the query selects a request identifier. */
      readonly id?: never;
    }
  | {
      /** Cedarling category or severity selected across retained entries. */
      readonly tag: CedarlingLogTag;
      /** Excluded when the query selects only a tag. */
      readonly id?: never;
      /** Excluded when the query selects only a tag. */
      readonly requestId?: never;
    };

/**
 * Public retained-log service attached to a Cedarling client.
 *
 * @example
 * ```ts
 * const decisions = await client.logs.find({ tag: "decision" });
 * ```
 */
export interface CedarlingLogs {
  /** Enumerates physically retained log identifiers without removing them. */
  ids(): Promise<Result<readonly string[], CedarlingLogError>>;

  /**
   * Finds retained entries correlated with one authorization request.
   *
   * @param query - Exact request identifier lookup.
   * @returns Detached normalized entries or a stable log-service error.
   */
  find(
    query?: LogQuery,
  ): Promise<Result<readonly CedarlingLogEntry[], CedarlingLogError>>;

  /** Returns and removes all physically retained log entries. */
  drain(): Promise<Result<readonly CedarlingLogEntry[], CedarlingLogError>>;
}
