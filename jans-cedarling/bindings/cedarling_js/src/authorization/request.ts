import type {
  CedarEntity,
  MultiIssuerAuthorizationRequest,
  TokenInput,
  UnsignedAuthorizationRequest,
} from "./types.js";
import { InputValidationError } from "../errors/errors.js";
import {
  snapshotCedarContextObject,
  snapshotCedarObject,
} from "../values/snapshot.js";
import {
  inspectPropertyDescriptor,
  type DataRecord,
} from "../helpers/records.js";
import {
  CEDAR_IDENTIFIER_PATTERN,
  DEFAULTS,
  FORMAL_ACTION_PATTERN,
  INPUT_FIELDS,
} from "../helpers/constants.js";
import { createInputValidator } from "../helpers/validation.js";

const {
  exactFields,
  field,
  invalid,
  record,
  requiredString,
} = createInputValidator("invalid authorization request", {
  allowNullPrototype: true,
});

/** Validates and normalizes either public action representation to one UID. */
function snapshotAction(value: unknown): string {
  if (value === undefined) {
    return invalid("required", ["action"]);
  }

  if (typeof value === "string") {
    const match = FORMAL_ACTION_PATTERN.exec(value);
    if (match === null) {
      return invalid(
        value.trim().length === 0 ? "required" : "format",
        ["action"],
      );
    }
    try {
      const id = JSON.parse(match[1] as string) as unknown;
      if (typeof id !== "string" || id.trim().length === 0) {
        return invalid("required", ["action"]);
      }
    } catch {
      return invalid("format", ["action"]);
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
    return invalid("type", ["action", "namespace"]);
  }
  const namespace = namespaceValue.split("::");
  if (
    namespace.length === 0 ||
    namespace.some((part) => !CEDAR_IDENTIFIER_PATTERN.test(part))
  ) {
    return invalid("format", ["action", "namespace"]);
  }

  return `${namespace.join("::")}::Action::${JSON.stringify(id)}`;
}

/** Runs a nested value snapshot while retaining its public request path. */
function snapshotAtPath<T>(
  path: readonly (string | number)[],
  snapshot: () => T,
): T {
  try {
    return snapshot();
  } catch (error: unknown) {
    if (error instanceof InputValidationError) {
      const issue = error.issues[0];
      throw new InputValidationError(
        issue?.code ?? DEFAULTS.validation.fallbackIssueCode,
        "invalid authorization value",
        [...path, ...(issue?.path ?? [])],
      );
    }
    return invalid("type", path);
  }
}

/** Creates an SDK-owned entity snapshot before crossing the engine Seam. */
function snapshotEntity(
  value: unknown,
  path: readonly (string | number)[],
): CedarEntity {
  const entity = record(value, path);
  exactFields(entity, INPUT_FIELDS.entity, path);
  const type = requiredString(field(entity, "type", path), [...path, "type"]);
  const id = requiredString(field(entity, "id", path), [...path, "id"]);
  const attributes = field(entity, "attributes", path);

  if (
    attributes !== undefined &&
    typeof attributes === "object" &&
    attributes !== null &&
    Object.prototype.hasOwnProperty.call(
      attributes,
      "cedar_entity_mapping",
    )
  ) {
    return invalid("unknownField", [
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
            () => snapshotCedarObject(attributes),
          ),
        }),
  };
}

/** Detaches the action, resource, and context shared by both trust models. */
function snapshotAuthorizationTarget(
  request: DataRecord,
): Pick<
  UnsignedAuthorizationRequest,
  "action" | "resource" | "context"
> {
  const context = field(request, "context", []);

  return {
    action: snapshotAction(field(request, "action", [])),
    resource: snapshotEntity(field(request, "resource", []), ["resource"]),
    ...(context === undefined
      ? {}
      : {
          context: snapshotAtPath(
            ["context"],
            () => snapshotCedarContextObject(context),
          ),
        }),
  };
}

/** Detaches all value-bearing parts of an unsigned authorization request. */
export function snapshotUnsignedRequest(
  value: UnsignedAuthorizationRequest,
): UnsignedAuthorizationRequest {
  const request = record(value, []);
  exactFields(request, INPUT_FIELDS.unsignedAuthorizationRequest, []);
  const principal = field(request, "principal", []);

  return {
    ...(principal === undefined
      ? {}
      : { principal: snapshotEntity(principal, ["principal"]) }),
    ...snapshotAuthorizationTarget(request),
  };
}

/** Validates and detaches a non-empty ordered token input array. */
function snapshotTokens(value: unknown): readonly TokenInput[] {
  if (!Array.isArray(value)) {
    return invalid(value === undefined ? "required" : "type", ["tokens"]);
  }
  if (value.length === 0) {
    return invalid("range", ["tokens"]);
  }

  const ownKeys = Reflect.ownKeys(value);
  if (
    ownKeys.some((key) => typeof key === "symbol") ||
    ownKeys.length !== value.length + 1
  ) {
    return invalid("type", ["tokens"]);
  }

  const descriptors = Object.getOwnPropertyDescriptors(value);
  const tokens: TokenInput[] = [];
  for (let index = 0; index < value.length; index += 1) {
    const property = inspectPropertyDescriptor(descriptors[String(index)]);
    if (
      property.kind !== "data" ||
      !property.enumerable
    ) {
      return invalid("type", ["tokens", index]);
    }

    const token = record(property.value, ["tokens", index]);
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
  const request = record(value, []);
  exactFields(request, INPUT_FIELDS.multiIssuerAuthorizationRequest, []);

  return {
    tokens: snapshotTokens(field(request, "tokens", [])),
    ...snapshotAuthorizationTarget(request),
  };
}
