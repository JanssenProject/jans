/** Reusable validation mechanics for untrusted JavaScript input records. */
import { InputValidationError } from "../errors/errors.js";
import type { ValidationIssue, ValidationIssueCode } from "../errors/types.js";
import { DEFAULTS } from "./constants.js";
import {
  inspectOwnProperty,
  isPlainDataRecord,
  ownEnumerableStringKeys,
  type DataRecord,
} from "./records.js";

type ValidationPath = ValidationIssue["path"];

/** Creates one scoped validator while retaining feature-specific diagnostics. */
export function createInputValidator(
  message: string,
  defaults: {
    readonly allowNullPrototype?: boolean;
    readonly stringNormalization?: "preserve" | "trim";
  } = {},
) {
  function invalid(
    code: ValidationIssueCode,
    path: ValidationPath = [],
  ): never {
    throw new InputValidationError(code, message, path);
  }

  function record(value: unknown, path: ValidationPath): DataRecord {
    return isPlainDataRecord(value, defaults.allowNullPrototype === true)
      ? value
      : invalid("type", path);
  }

  function field(
    value: DataRecord,
    key: string,
    path: ValidationPath,
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
      return invalidAccessor ? invalid("type", [...path, key]) : undefined;
    }
    if (!property.enumerable) {
      return behavior.nonEnumerableData === "invalid"
        ? invalid("type", [...path, key])
        : undefined;
    }
    return property.value;
  }

  function exactFields(
    value: DataRecord,
    allowed: readonly string[],
    path: ValidationPath,
  ): void {
    for (const key of ownEnumerableStringKeys(value)) {
      if (!allowed.includes(key)) {
        invalid("unknownField", [...path, key]);
      }
    }
  }

  function requiredString(
    value: unknown,
    path: ValidationPath,
    behavior: {
      readonly empty?: "empty" | "blank";
      readonly normalize?: "preserve" | "trim";
      readonly undefinedCode?: "required" | "type";
    } = {},
  ): string {
    if (typeof value !== "string") {
      return invalid(
        value === undefined
          ? behavior.undefinedCode ?? DEFAULTS.validation.undefinedStringCode
          : "type",
        path,
      );
    }
    const blank = behavior.empty === "empty"
      ? value.length === 0
      : value.trim().length === 0;
    if (blank) {
      return invalid("required", path);
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
