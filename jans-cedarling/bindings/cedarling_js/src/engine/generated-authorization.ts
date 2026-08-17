import type {
  AuthorizationDecision,
  CedarEntity,
  MultiIssuerAuthorizationRequest,
  UnsignedAuthorizationRequest,
} from "../authorization/types.js";
import {
  createPolicyEvaluationError,
  createSdkError,
} from "../errors/errors.js";
import {
  errorCode,
  type CedarlingError,
} from "../errors/types.js";
import { isObjectRecord, ownDataProperty } from "../helpers/records.js";

interface GeneratedAuthorizationResult {
  readonly decision: boolean;

  readonly requestId: string;

  readonly reasons: readonly string[];

  readonly errors: readonly CedarlingError[];
}
function copyStringArray(value: unknown): readonly string[] | undefined {
  if (!Array.isArray(value) || !value.every((item) => typeof item === "string")) {
    return undefined;
  }
  return [...value];
}

function copyPolicyErrors(
  value: unknown,
  operation: "authorizeUnsigned" | "authorizeMultiIssuer",
): readonly CedarlingError[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const copied: CedarlingError[] = [];
  for (const item of value) {
    if (!isObjectRecord(item)) {
      return undefined;
    }
    const id = ownDataProperty(item, "id");
    const error = ownDataProperty(item, "error");
    if (typeof id !== "string" || typeof error !== "string") {
      return undefined;
    }
    copied.push(createPolicyEvaluationError(id, error, operation));
  }
  return copied;
}

/**
 * Validates the binding's serialized result protocol.
 *
 * Malformed JSON is a conversion failure. Valid JSON with an incompatible
 * generated field layout is an adapter protocol failure.
 */
export function parseGeneratedResult(
  serialized: string,
  operation: "authorizeUnsigned" | "authorizeMultiIssuer",
): GeneratedAuthorizationResult {
  let value: unknown;
  try {
    value = JSON.parse(serialized) as unknown;
  } catch (error: unknown) {
    throw createSdkError(
      errorCode.resultConversionFailed,
      operation,
      { rawCause: error },
    );
  }

  if (!isObjectRecord(value)) {
    throw createSdkError(
      errorCode.generatedProtocolError,
      operation,
    );
  }

  const decision = ownDataProperty(value, "decision");
  const requestId = ownDataProperty(value, "request_id");
  const response = ownDataProperty(value, "response");
  if (
    typeof decision !== "boolean" ||
    typeof requestId !== "string" ||
    !isObjectRecord(response)
  ) {
    throw createSdkError(
      errorCode.generatedProtocolError,
      operation,
    );
  }

  const diagnostics = ownDataProperty(response, "diagnostics");
  if (!isObjectRecord(diagnostics)) {
    throw createSdkError(
      errorCode.generatedProtocolError,
      operation,
    );
  }

  const reasons = copyStringArray(ownDataProperty(diagnostics, "reason"));
  const errors = copyPolicyErrors(
    ownDataProperty(diagnostics, "errors"),
    operation,
  );
  if (reasons === undefined || errors === undefined) {
    throw createSdkError(
      errorCode.generatedProtocolError,
      operation,
    );
  }

  return { decision, requestId, reasons, errors };
}

export function toAuthorizationDecision(
  parsed: GeneratedAuthorizationResult,
): AuthorizationDecision {
  return {
    decision: parsed.decision,
    requestId: parsed.requestId,
    diagnostics: {
      reasons: [...parsed.reasons],
      errors: [...parsed.errors],
    },
  };
}

function toGeneratedEntity(entity: CedarEntity): Record<string, unknown> {
  return {
    ...entity.attributes,
    cedar_entity_mapping: {
      entity_type: entity.type,
      id: entity.id,
    },
  };
}

function toGeneratedAuthorizationTarget(
  request: UnsignedAuthorizationRequest | MultiIssuerAuthorizationRequest,
): Record<string, unknown> {
  return {
    action: request.action,
    resource: toGeneratedEntity(request.resource),
    context: request.context ?? {},
  };
}

export function toGeneratedRequest(
  request: UnsignedAuthorizationRequest,
): Record<string, unknown> {
  return {
    ...(request.principal === undefined
      ? {}
      : { principal: toGeneratedEntity(request.principal) }),
    ...toGeneratedAuthorizationTarget(request),
  };
}

export function toGeneratedMultiIssuerRequest(
  request: MultiIssuerAuthorizationRequest,
): Record<string, unknown> {
  return {
    tokens: request.tokens.map((token) => ({
      mapping: token.mapping,
      payload: token.payload,
    })),
    ...toGeneratedAuthorizationTarget(request),
  };
}
