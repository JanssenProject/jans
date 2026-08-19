/** Reusable validation mechanics for untrusted JavaScript input records. */
import { createInputError } from "../errors/errors.js";
import type {
  CedarlingErrorCode,
  CedarlingOperation,
} from "../errors/types.js";
import { errorCode } from "../errors/types.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
  ownEnumerableStringKeys,
  type DataRecord,
} from "./records.js";

/** Descriptor handling policies shared by feature input validators. */
export const FIELD_BEHAVIORS = Object.freeze({
  rejectAccessors: {
    accessor: "alwaysInvalid",
  },
  strictEnumerableData: {
    accessor: "alwaysInvalid",
    nonEnumerableData: "invalid",
  },
} as const);

/** Creates one validator scoped to an exact public operation. */
export function createInputValidator(
  operation: CedarlingOperation,
  defaults: {
    readonly allowNullPrototype?: boolean;
    readonly stringNormalization?: "preserve" | "trim";
  } = {},
) {
  function invalid(
    code: CedarlingErrorCode,
    path: readonly (string | number)[] = [],
  ): never {
    throw createInputError(code, operation, path);
  }

  function record(
    value: unknown,
    path: readonly (string | number)[],
  ): DataRecord {
    return isPlainDataRecord(value, defaults.allowNullPrototype === true)
      ? value
      : invalid(errorCode.inputInvalidType, path);
  }

  function field(
    value: DataRecord,
    key: string,
    path: readonly (string | number)[],
    behavior: {
      readonly accessor?: "alwaysInvalid" | "invalidWhenEnumerable";
      readonly nonEnumerableData?: "invalid" | "missing";
    } = {},
  ): unknown {
    const property = inspectOwnProperty(value, key);
    if (property.kind === "missing") {
      return undefined;
    }
    if (property.kind === "accessor") {
      const invalidAccessor =
        behavior.accessor === "alwaysInvalid" || property.enumerable;
      return invalidAccessor
        ? invalid(errorCode.inputInvalidType, [...path, key])
        : undefined;
    }
    if (!property.enumerable) {
      return behavior.nonEnumerableData === "invalid"
        ? invalid(errorCode.inputInvalidType, [...path, key])
        : undefined;
    }
    return property.value;
  }

  function exactFields(
    value: DataRecord,
    allowed: readonly string[],
    path: readonly (string | number)[],
  ): void {
    for (const key of ownEnumerableStringKeys(value)) {
      if (!allowed.includes(key)) {
        invalid(errorCode.inputUnknownField, [...path, key]);
      }
    }
  }

  function requiredString(
    value: unknown,
    path: readonly (string | number)[],
    behavior: {
      readonly empty?: "empty" | "blank";
      readonly normalize?: "preserve" | "trim";
      readonly missingIsInvalidType?: boolean;
    } = {},
  ): string {
    if (typeof value !== "string") {
      return invalid(
        value === undefined
          ? behavior.missingIsInvalidType === true
            ? errorCode.inputInvalidType
            : errorCode.inputRequired
          : errorCode.inputInvalidType,
        path,
      );
    }
    const blank = behavior.empty === "empty"
      ? value.length === 0
      : value.trim().length === 0;
    if (blank) {
      return invalid(errorCode.inputRequired, path);
    }
    return (behavior.normalize ?? defaults.stringNormalization) === "trim"
      ? value.trim()
      : value;
  }

  return Object.freeze({
    exactFields,
    field,
    invalid,
    record,
    requiredString,
  });
}

/** Reports whether a value is a safe integer inside an inclusive range. */
export function isSafeIntegerInRange(
  value: unknown,
  minimum: number,
  maximum: number,
): value is number {
  return (
    typeof value === "number" &&
    Number.isSafeInteger(value) &&
    value >= minimum &&
    value <= maximum
  );
}
