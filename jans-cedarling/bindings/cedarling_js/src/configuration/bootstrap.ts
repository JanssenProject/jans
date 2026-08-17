import {
  DEFAULTS,
  INPUT_FIELDS,
  JWT_ALGORITHMS,
} from "../helpers/constants.js";
import {
  field,
  jwtAlgorithms,
  optionalBoolean,
  record,
  rejectUnknown,
  requiredString,
} from "./validation.js";

function section(value: unknown, name: string, fields: readonly string[]) {
  const options = value === undefined ? {} : record(value, [name]);
  rejectUnknown(options, fields, [name]);
  return {
    read: (fieldName: string) => field(options, fieldName, [name]),
    path: (fieldName: string) => [name, fieldName] as const,
  };
}

export function applyTypedBootstrap(
  options: Record<string, unknown>,
  bootstrap: Record<string, unknown>,
): void {
  const authorization = section(
    field(options, "authorization", []),
    "authorization",
    INPUT_FIELDS.authorization,
  );
  bootstrap.CEDARLING_STRICT_SCHEMA_VALIDATION = optionalBoolean(
    authorization.read("dangerouslyDisableSchemaValidation"),
    DEFAULTS.authorization.disableSchemaValidation,
    authorization.path("dangerouslyDisableSchemaValidation"),
  ) ? "disabled" : "enabled";
  bootstrap.CEDARLING_DECISION_LOG_DEFAULT_JWT_ID = requiredString(
    authorization.read("decisionLogTokenIdClaim") ??
      DEFAULTS.authorization.decisionLogTokenIdClaim,
    authorization.path("decisionLogTokenIdClaim"),
  );

  const jwt = section(field(options, "jwt", []), "jwt", INPUT_FIELDS.jwt);
  bootstrap.CEDARLING_JWT_SIG_VALIDATION = optionalBoolean(
    jwt.read("dangerouslyDisableSignatureValidation"),
    DEFAULTS.jwt.disableSignatureValidation,
    jwt.path("dangerouslyDisableSignatureValidation"),
  ) ? "disabled" : "enabled";
  bootstrap.CEDARLING_JWT_STATUS_VALIDATION = optionalBoolean(
    jwt.read("dangerouslyDisableStatusValidation"),
    DEFAULTS.jwt.disableStatusValidation,
    jwt.path("dangerouslyDisableStatusValidation"),
  ) ? "disabled" : "enabled";
  const algorithms = jwt.read("allowedAlgorithms");
  bootstrap.CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED =
    algorithms === undefined
      ? [...JWT_ALGORITHMS]
      : [...jwtAlgorithms(algorithms, jwt.path("allowedAlgorithms"))];
}
