import type {
  CedarEntity,
  MultiIssuerAuthorizationRequest,
  TokenInput,
  UnsignedAuthorizationRequest,
} from "./types.js";
import { normalizeInputError } from "../errors/errors.js";
import { errorCode, type CedarlingOperation } from "../errors/types.js";
import { snapshotCedarObject } from "../values/snapshot.js";
import {
  inspectDenseArray,
  type DataRecord,
} from "../helpers/records.js";
import { INPUT_FIELDS } from "../helpers/constants.js";
import { createInputValidator } from "../helpers/validation.js";

const CEDAR_IDENTIFIER_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*$/u;
const FORMAL_ACTION_PATTERN =
  /^(?:[A-Za-z_][A-Za-z0-9_]*::)*Action::("(?:[^"\\\u0000-\u001F]|\\(?:["\\/bfnrt]|u[0-9A-Fa-f]{4}))*")$/u;

function validator(
  operation: Extract<
    CedarlingOperation,
    "authorizeUnsigned" | "authorizeMultiIssuer"
  >,
) {
  return createInputValidator(operation, { allowNullPrototype: true });
}

/** Validates and normalizes either public action representation to one UID. */
function snapshotAction(
  value: unknown,
  operation: Extract<
    CedarlingOperation,
    "authorizeUnsigned" | "authorizeMultiIssuer"
  >,
): string {
  const { exactFields, field, invalid, record, requiredString } =
    validator(operation);
  if (value === undefined) {
    return invalid(errorCode.inputRequired, ["action"]);
  }

  if (typeof value === "string") {
    const match = FORMAL_ACTION_PATTERN.exec(value);
    if (match === null) {
      return invalid(
        value.trim().length === 0
          ? errorCode.inputRequired
          : errorCode.inputInvalidFormat,
        ["action"],
      );
    }
    try {
      const id = JSON.parse(match[1] as string) as unknown;
      if (typeof id !== "string" || id.trim().length === 0) {
        return invalid(errorCode.inputRequired, ["action"]);
      }
    } catch {
      return invalid(errorCode.inputInvalidFormat, ["action"]);
    }
    return value;
  }

  const action = record(value, ["action"]);
  exactFields(action, INPUT_FIELDS.action, ["action"]);

  const id = requiredString(field(action, "id", ["action"]), [
    "action",
    "id",
  ]);
  const namespaceValue = field(action, "namespace", ["action"]);
  if (namespaceValue === undefined) {
    return `Action::${JSON.stringify(id)}`;
  }
  if (typeof namespaceValue !== "string") {
    return invalid(errorCode.inputInvalidType, ["action", "namespace"]);
  }
  const namespace = namespaceValue.split("::");
  if (namespace.some((part) => !CEDAR_IDENTIFIER_PATTERN.test(part))) {
    return invalid(errorCode.inputInvalidFormat, ["action", "namespace"]);
  }

  return `${namespace.join("::")}::Action::${JSON.stringify(id)}`;
}

/** Runs a nested value snapshot while retaining its public request path. */
function snapshotAtPath<T>(
  path: readonly (string | number)[],
  operation: Extract<
    CedarlingOperation,
    "authorizeUnsigned" | "authorizeMultiIssuer"
  >,
  snapshot: () => T,
): T {
  try {
    return snapshot();
  } catch (error: unknown) {
    throw normalizeInputError(error, operation, path);
  }
}

/** Creates an SDK-owned entity snapshot before crossing the engine Seam. */
function snapshotEntity(
  value: unknown,
  path: readonly (string | number)[],
  operation: Extract<
    CedarlingOperation,
    "authorizeUnsigned" | "authorizeMultiIssuer"
  >,
): CedarEntity {
  const { exactFields, field, invalid, record, requiredString } =
    validator(operation);
  const entity = record(value, path);
  exactFields(entity, INPUT_FIELDS.entity, path);
  const type = requiredString(field(entity, "type", path), [...path, "type"]);
  const id = requiredString(field(entity, "id", path), [...path, "id"]);
  const attributes = field(entity, "attributes", path);

  if (
    attributes !== undefined &&
    typeof attributes === "object" &&
    attributes !== null &&
    Object.hasOwn(attributes, "cedar_entity_mapping")
  ) {
    return invalid(errorCode.inputUnknownField, [
      ...path,
      "attributes",
      "cedar_entity_mapping",
    ]);
  }

  return {
    type,
    id,
    ...(attributes === undefined
      ? {}
      : {
          attributes: snapshotAtPath(
            [...path, "attributes"],
            operation,
            () => snapshotCedarObject(attributes, operation),
          ),
        }),
  };
}

/** Detaches the action, resource, and context shared by both trust models. */
function snapshotAuthorizationTarget(
  request: DataRecord,
  operation: Extract<
    CedarlingOperation,
    "authorizeUnsigned" | "authorizeMultiIssuer"
  >,
): Pick<
  UnsignedAuthorizationRequest,
  "action" | "resource" | "context"
> {
  const { field } = validator(operation);
  const context = field(request, "context", []);

  return {
    action: snapshotAction(field(request, "action", []), operation),
    resource: snapshotEntity(
      field(request, "resource", []),
      ["resource"],
      operation,
    ),
    ...(context === undefined
      ? {}
      : {
          context: snapshotAtPath(
            ["context"],
            operation,
            () => snapshotCedarObject(context, operation),
          ),
        }),
  };
}

/** Detaches all value-bearing parts of an unsigned authorization request. */
export function snapshotUnsignedRequest(
  value: UnsignedAuthorizationRequest,
): UnsignedAuthorizationRequest {
  const operation = "authorizeUnsigned";
  const { exactFields, field, record } = validator(operation);
  const request = record(value, []);
  exactFields(request, INPUT_FIELDS.unsignedAuthorizationRequest, []);
  const principal = field(request, "principal", []);

  return {
    ...(principal === undefined
      ? {}
      : { principal: snapshotEntity(principal, ["principal"], operation) }),
    ...snapshotAuthorizationTarget(request, operation),
  };
}

/** Validates and detaches a non-empty ordered token input array. */
function snapshotTokens(
  value: unknown,
  operation: "authorizeMultiIssuer",
): readonly TokenInput[] {
  const { exactFields, field, invalid, record, requiredString } =
    validator(operation);
  if (!Array.isArray(value)) {
    return invalid(
      value === undefined
        ? errorCode.inputRequired
        : errorCode.inputInvalidType,
      ["tokens"],
    );
  }
  if (value.length === 0) {
    return invalid(errorCode.inputOutOfRange, ["tokens"]);
  }

  const inspected = inspectDenseArray(value);
  if (inspected.kind === "invalid") {
    return invalid(
      errorCode.inputInvalidType,
      inspected.index === undefined ? ["tokens"] : ["tokens", inspected.index],
    );
  }

  const tokens: TokenInput[] = [];
  for (const [index, tokenValue] of inspected.values.entries()) {
    const token = record(tokenValue, ["tokens", index]);
    exactFields(token, INPUT_FIELDS.token, ["tokens", index]);
    tokens.push({
      mapping: requiredString(
        field(token, "mapping", ["tokens", index]),
        ["tokens", index, "mapping"],
      ),
      payload: requiredString(
        field(token, "payload", ["tokens", index]),
        ["tokens", index, "payload"],
      ),
    });
  }
  return tokens;
}

/** Detaches all value-bearing parts of a multi-issuer request. */
export function snapshotMultiIssuerRequest(
  value: MultiIssuerAuthorizationRequest,
): MultiIssuerAuthorizationRequest {
  const operation = "authorizeMultiIssuer";
  const { exactFields, field, record } = validator(operation);
  const request = record(value, []);
  exactFields(request, INPUT_FIELDS.multiIssuerAuthorizationRequest, []);

  return {
    tokens: snapshotTokens(field(request, "tokens", []), operation),
    ...snapshotAuthorizationTarget(request, operation),
  };
}
