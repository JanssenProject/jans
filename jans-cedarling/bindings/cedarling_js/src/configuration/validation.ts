import type { JwtAlgorithm } from "./types.js";
import {
  DEFAULTS,
  INPUT_FIELDS,
  JWT_ALGORITHM_SET,
} from "../helpers/constants.js";
import { createInputValidator } from "../helpers/validation.js";
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

export function optionalBoolean(
  value: unknown,
  fallback: boolean,
  path: readonly string[],
): boolean {
  if (value === undefined) return fallback;
  return typeof value === "boolean"
    ? value
    : invalid(errorCode.inputInvalidType, path);
}

export function prepareDebug(value: unknown): boolean {
  if (value === undefined) return DEFAULTS.client.exposeRawErrors;
  const options = record(value, ["debug"]);
  rejectUnknown(options, INPUT_FIELDS.debug, ["debug"]);
  return optionalBoolean(
    field(options, "dangerouslyExposeRawErrors", ["debug"]),
    DEFAULTS.client.exposeRawErrors,
    ["debug", "dangerouslyExposeRawErrors"],
  );
}

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
