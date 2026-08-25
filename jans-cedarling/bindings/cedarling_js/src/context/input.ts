import { INPUT_FIELDS, LIMITS } from "../helpers/constants.js";
import {
  createInputValidator,
  FIELD_BEHAVIORS,
  isSafeIntegerInRange,
} from "../helpers/validation.js";
import { snapshotCedarValue } from "../values/snapshot.js";
import type { ContextDataValue } from "../values/types.js";
import type { ContextSetOptions } from "./types.js";
import { errorCode, type CedarlingOperation } from "../errors/types.js";

/** Requires one non-empty context-data key without changing its identity. */
export function snapshotContextKey(
  value: string,
  operation: Extract<
    CedarlingOperation,
    "context.get" | "context.getEntry" | "context.delete"
  >,
): string {
  return createInputValidator(operation).requiredString(value, [], {
    missingIsInvalidType: true,
  });
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
  const operation = "context.set";
  const { exactFields, field, invalid, record, requiredString } =
    createInputValidator(operation);
  const snapshotKey = requiredString(key, [], {
    missingIsInvalidType: true,
  });
  const snapshotValue = snapshotCedarValue(value, operation);
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
    invalid(errorCode.inputOutOfRange, ["options", "ttlSeconds"]);
  }
  const ttlSeconds = ttlValue === undefined ? undefined : ttlValue as number;
  return {
    key: snapshotKey,
    value: snapshotValue,
    ...(ttlSeconds === undefined ? {} : { ttlSeconds }),
  };
}
