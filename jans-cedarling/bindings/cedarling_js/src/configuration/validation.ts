import type { JwtAlgorithm } from "./types.js";
import {
  DEFAULTS,
  INPUT_FIELDS,
  JS_SAFE_U64_MAX,
  JWT_ALGORITHM_SET,
  LIMITS,
} from "../helpers/constants.js";
import {
  createInputValidator,
  isSafeIntegerInRange,
} from "../helpers/validation.js";
import { snapshotJsonValue } from "../values/snapshot.js";
import { errorCode } from "../errors/types.js";

export const {
  exactFields: rejectUnknown,
  field,
  invalid,
  record,
  requiredString,
} = createInputValidator("initialize", {
  stringNormalization: "trim",
});
/** Validates the explicit local-development diagnostics policy. */
export function prepareDebug(value: unknown): boolean {
  if (value === undefined) {
    return DEFAULTS.client.exposeRawErrors;
  }
  const options = record(value, ["debug"]);
  rejectUnknown(options, INPUT_FIELDS.debug, ["debug"]);
  return optionalBoolean(
    field(options, "dangerouslyExposeRawErrors", ["debug"]),
    DEFAULTS.client.exposeRawErrors,
    ["debug", "dangerouslyExposeRawErrors"],
  );
}

/** Returns an optional boolean or its versioned default. */
export function optionalBoolean(
  value: unknown,
  fallback: boolean,
  path: readonly string[],
): boolean {
  if (value === undefined) {
    return fallback;
  }
  return typeof value === "boolean"
    ? value
    : invalid(errorCode.inputInvalidType, path);
}

/** Validates one safe integer against an inclusive range. */
export function integer(
  value: unknown,
  fallback: number,
  minimum: number,
  maximum: number,
  path: readonly string[],
): number {
  if (value === undefined) {
    return fallback;
  }
  if (!isSafeIntegerInRange(value, minimum, maximum)) {
    return invalid(errorCode.inputOutOfRange, path);
  }
  return value;
}

/** Validates an optional safe integer without materializing a default. */
export function optionalInteger(
  value: unknown,
  minimum: number,
  maximum: number,
  path: readonly string[],
): number | undefined {
  if (value === undefined) {
    return undefined;
  }
  return isSafeIntegerInRange(value, minimum, maximum)
    ? value
    : invalid(errorCode.inputOutOfRange, path);
}

/** Enforces the zero-or-at-least-five refresh interval convention. */
export function refreshInterval(
  value: unknown,
  fallback: number | undefined,
  path: readonly string[],
): number | undefined {
  const parsed = optionalInteger(
    value,
    LIMITS.unsignedInteger.minimum,
    JS_SAFE_U64_MAX,
    path,
  );
  const result = parsed ?? fallback;
  if (
    result !== undefined &&
    result !== 0 &&
    result < LIMITS.refreshIntervalSeconds.minimumEnabled
  ) {
    return invalid(errorCode.inputOutOfRange, path);
  }
  return result;
}

/** Reports whether a normalized URL hostname is an exact loopback address. */
function isLoopbackHostname(hostname: string): boolean {
  if (hostname === "localhost" || hostname === "[::1]") {
    return true;
  }

  const octets = hostname.split(".");
  return (
    octets.length === 4 &&
    octets[0] === "127" &&
    octets.every(
      (octet) =>
        /^(?:0|[1-9]\d{0,2})$/u.test(octet) && Number(octet) <= 255,
    )
  );
}

/** Returns an absolute credential-free HTTPS or loopback HTTP URL string. */
export function httpUrl(value: unknown, path: readonly string[]): string {
  if (!(typeof value === "string" || value instanceof URL)) {
    return invalid(errorCode.inputInvalidType, path);
  }
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return invalid(errorCode.inputInvalidFormat, path);
  }
  if (
    (
      url.protocol !== "https:" &&
      !(url.protocol === "http:" && isLoopbackHostname(url.hostname))
    ) ||
    url.username !== "" ||
    url.password !== ""
  ) {
    return invalid(errorCode.inputInvalidFormat, path);
  }
  return url.toString();
}

/** Detaches and validates the configured JWT algorithm allowlist once. */
export function jwtAlgorithms(
  value: unknown,
  path: readonly string[],
): readonly JwtAlgorithm[] {
  let snapshot: unknown;
  try {
    snapshot = snapshotJsonValue(value, "initialize");
  } catch {
    return invalid(errorCode.inputInvalidFormat, path);
  }

  if (
    !Array.isArray(snapshot) ||
    snapshot.length === 0 ||
    !snapshot.every(
      (algorithm) =>
        typeof algorithm === "string" &&
        JWT_ALGORITHM_SET.has(algorithm),
    ) ||
    new Set(snapshot).size !== snapshot.length
  ) {
    return invalid(errorCode.inputInvalidFormat, path);
  }

  return snapshot as readonly JwtAlgorithm[];
}
