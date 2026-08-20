import type { CedarlingOptions } from "./types.js";
import type { JsonObject } from "../values/types.js";
import { DEFAULTS, INPUT_FIELDS, JS_SAFE_U64_MAX } from "../helpers/constants.js";
import { snapshotJsonObject } from "../values/snapshot.js";
import { applyTypedBootstrap } from "./bootstrap.js";
import { preparePolicyStore, type PreparedPolicySource } from "./policy-source.js";
import { field, invalid, prepareDebug, record, rejectUnknown, requiredString } from "./validation.js";
import { errorCode } from "../errors/types.js";

export interface PreparedClientCapabilities {
  readonly exposeRawErrors: boolean;

  readonly memoryLogging: boolean;

  readonly contextMaxTtlSeconds: number;
}

export interface PreparedEngineOptions {
  readonly bootstrapConfig: Readonly<Record<string, unknown>>;

  readonly policyStore: PreparedPolicySource;
}

export interface PreparedCedarlingOptions extends PreparedEngineOptions {
  readonly clientCapabilities: PreparedClientCapabilities;
}

function contextMaxTtlSeconds(value: unknown): number {
  let candidate = value;

  if (typeof candidate === "string") {
    try {
      candidate = JSON.parse(candidate) as unknown;
    } catch {
      return DEFAULTS.contextStore.maxTtlSeconds;
    }
  }

  if (
    typeof candidate !== "number" ||
    !Number.isInteger(candidate) ||
    candidate < 0
  ) {
    return DEFAULTS.contextStore.maxTtlSeconds;
  }

  return Math.min(candidate, JS_SAFE_U64_MAX);
}

function prepareClientCapabilities(
  bootstrap: Readonly<Record<string, unknown>>,
  exposeRawErrors: boolean,
): PreparedClientCapabilities {
  return Object.freeze({
    exposeRawErrors,
    memoryLogging: bootstrap.CEDARLING_LOG_TYPE === "memory",
    contextMaxTtlSeconds: contextMaxTtlSeconds(
      bootstrap.CEDARLING_DATA_STORE_MAX_TTL,
    ),
  });
}

/**
 * Validates, normalizes, and detaches public options before asynchronous work.
 *
 * @param input - Untrusted runtime value typed as the public options contract.
 * @returns Frozen bootstrap data plus one detached adapter policy source.
 */
export function prepareCedarlingOptions(
  input: CedarlingOptions,
): PreparedCedarlingOptions {
  const options = record(input, []);
  const rawBootstrap = field(options, "bootstrapProperties", []);

  if (rawBootstrap !== undefined) {
    rejectUnknown(options, INPUT_FIELDS.rawBootstrap, []);
    const exposeRawErrors = prepareDebug(field(options, "debug", []));

    let bootstrapConfig: JsonObject;
    try {
      bootstrapConfig = snapshotJsonObject(rawBootstrap, "initialize");
    } catch {
      return invalid(errorCode.inputInvalidType, ["bootstrapProperties"]);
    }

    return {
      bootstrapConfig: Object.freeze(bootstrapConfig),
      policyStore: { type: "bootstrap" },
      clientCapabilities: prepareClientCapabilities(
        bootstrapConfig,
        exposeRawErrors,
      ),
    };
  }

  rejectUnknown(
    options,
    INPUT_FIELDS.webNativeOptions,
    [],
  );
  const exposeRawErrors = prepareDebug(field(options, "debug", []));
  const bootstrap: Record<string, unknown> = {
    CEDARLING_APPLICATION_NAME: requiredString(
      field(options, "applicationName", []),
      ["applicationName"],
    ),
  };
  const policyStore = preparePolicyStore(
    field(options, "policyStore", []),
    bootstrap,
  );
  applyTypedBootstrap(options, bootstrap);

  return {
    bootstrapConfig: Object.freeze(bootstrap),
    policyStore,
    clientCapabilities: prepareClientCapabilities(bootstrap, exposeRawErrors),
  };
}
