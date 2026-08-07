import {
  FIELD_BEHAVIORS,
  INPUT_FIELDS,
  LIMITS,
} from "../helpers/constants.js";
import {
  createInputValidator,
  isSafeIntegerInRange,
} from "../helpers/validation.js";
import { snapshotCedarContextValue } from "../values/snapshot.js";
import type { ContextDataValue } from "../values/types.js";
import type { ContextSetOptions } from "./types.js";

const {
  exactFields,
  field,
  invalid,
  record,
  requiredString,
} = createInputValidator("invalid context input");

/** Requires one non-empty context-data key without changing its identity. */
export function snapshotContextKey(value: string): string {
  return requiredString(value, [], { undefinedCode: "type" });
}

/** Validates and detaches one context-data write. */
export function snapshotContextSet(
  key: string,
  value: ContextDataValue,
  options: ContextSetOptions | undefined,
  maxTtlSeconds: number,
): {
  readonly key: string;
  readonly value: ContextDataValue;
  readonly ttlSeconds?: number;
} {
  const snapshotKey = snapshotContextKey(key);
  const snapshotValue = snapshotCedarContextValue(value);
  if (options === undefined) {
    return { key: snapshotKey, value: snapshotValue };
  }
  const snapshotOptions = record(options, ["options"]);
  exactFields(snapshotOptions, INPUT_FIELDS.contextSet, ["options"]);
  const ttlValue = field(
    snapshotOptions,
    "ttlSeconds",
    ["options"],
    FIELD_BEHAVIORS.rejectAccessors,
  );
  if (
    ttlValue !== undefined &&
    !isSafeIntegerInRange(
      ttlValue,
      LIMITS.positiveInteger.minimum,
      maxTtlSeconds,
    )
  ) {
    invalid("range", ["options", "ttlSeconds"]);
  }
  const ttlSeconds = ttlValue === undefined ? undefined : ttlValue as number;
  return {
    key: snapshotKey,
    value: snapshotValue,
    ...(ttlSeconds === undefined ? {} : { ttlSeconds }),
  };
}
