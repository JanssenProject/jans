function freezeValues<T extends Record<string, object>>(record: T): Readonly<T> {
  for (const value of Object.values(record)) Object.freeze(value);
  return Object.freeze(record);
}

export const DEFAULTS = freezeValues({
  authorization: {
    decisionLogTokenIdClaim: "jti",
    disableSchemaValidation: false,
  },
  client: {
    exposeRawErrors: false,
  },
  jwt: {
    disableSignatureValidation: false,
    disableStatusValidation: false,
  },
} as const);

export const JWT_ALGORITHMS = [
  "HS256",
  "HS384",
  "HS512",
  "ES256",
  "ES384",
  "RS256",
  "RS384",
  "RS512",
  "PS256",
  "PS384",
  "PS512",
  "EdDSA",
] as const;

export const JWT_ALGORITHM_SET: ReadonlySet<string> =
  new Set(JWT_ALGORITHMS);

export const CEDAR_EXTENSION_FUNCTIONS = [
  "decimal",
  "ip",
  "datetime",
  "duration",
] as const;

export const CEDAR_EXTENSION_FUNCTION_SET: ReadonlySet<string> =
  new Set(CEDAR_EXTENSION_FUNCTIONS);

export const INPUT_FIELDS = freezeValues({
  action: ["namespace", "id"],
  authorization: [
    "dangerouslyDisableSchemaValidation",
    "decisionLogTokenIdClaim",
  ],
  debug: ["dangerouslyExposeRawErrors"],
  entity: ["type", "id", "attributes"],
  jwt: [
    "dangerouslyDisableSignatureValidation",
    "dangerouslyDisableStatusValidation",
    "allowedAlgorithms",
  ],
  multiIssuerAuthorizationRequest: ["tokens", "action", "resource", "context"],
  policyInline: ["type", "document"],
  token: ["mapping", "payload"],
  webNativeOptions: [
    "applicationName",
    "policyStore",
    "debug",
    "authorization",
    "jwt",
  ],
  unsignedAuthorizationRequest: ["principal", "action", "resource", "context"],
} as const);
