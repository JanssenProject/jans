import type { LogLevel } from "../configuration/types.js";
import type { Result } from "../errors/types.js";
import type { JsonObject } from "../values/types.js";
import type { LOG_KINDS } from "../helpers/constants.js";

export type CedarlingLogKind = (typeof LOG_KINDS)[number] | "unknown";

export type CedarlingLogTag = (typeof LOG_KINDS)[number] | LogLevel;

/**
 * Detached, normalized retained log entry with residual source fields in its
 * payload. Unrecognized generated kinds use `unknown` and retain their original
 * value in `payload.log_kind`.
 */
export interface CedarlingLogEntry {
  readonly id: string;
  readonly requestId?: string;
  readonly timestamp?: string;
  readonly kind: CedarlingLogKind;
  readonly level?: LogLevel;
  readonly pdpId: string;
  readonly applicationId?: string;
  readonly payload: JsonObject;
}

/**
 * Exact retained-log query combinations supported by Cedarling.
 */
export type LogQuery =
  | {
      readonly id: string;
      readonly requestId?: never;
      readonly tag?: never;
    }
  | {
      readonly requestId: string;
      readonly tag?: CedarlingLogTag;
      readonly id?: never;
    }
  | {
      readonly tag: CedarlingLogTag;
      readonly id?: never;
      readonly requestId?: never;
    };

export interface CedarlingLogs {
  ids(): Promise<Result<readonly string[]>>;
  find(
    query?: LogQuery,
  ): Promise<Result<readonly CedarlingLogEntry[]>>;

  drain(): Promise<Result<readonly CedarlingLogEntry[]>>;
}
