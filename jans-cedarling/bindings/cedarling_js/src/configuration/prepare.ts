import type { CedarlingOptions } from "./types.js";
import { INPUT_FIELDS } from "../helpers/constants.js";
import { applyTypedBootstrap } from "./bootstrap.js";
import {
  preparePolicyStore,
  type PreparedPolicySource,
} from "./policy-source.js";
import {
  field,
  prepareDebug,
  record,
  rejectUnknown,
  requiredString,
} from "./validation.js";

export interface PreparedClientCapabilities {
  readonly exposeRawErrors: boolean;
}

export interface PreparedEngineOptions {
  readonly bootstrapConfig: Readonly<Record<string, unknown>>;
  readonly policyStore: PreparedPolicySource;
}

export interface PreparedCedarlingOptions extends PreparedEngineOptions {
  readonly clientCapabilities: PreparedClientCapabilities;
}

export function prepareCedarlingOptions(
  input: CedarlingOptions,
): PreparedCedarlingOptions {
  const options = record(input, []);
  rejectUnknown(options, INPUT_FIELDS.webNativeOptions, []);
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
    clientCapabilities: Object.freeze({
      exposeRawErrors: prepareDebug(field(options, "debug", [])),
    }),
  };
}
