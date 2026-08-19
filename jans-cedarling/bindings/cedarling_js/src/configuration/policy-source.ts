import type { JsonObject } from "../values/types.js";
import { INPUT_FIELDS } from "../helpers/constants.js";
import { snapshotJsonObject } from "../values/snapshot.js";
import {
  field,
  invalid,
  record,
  rejectUnknown,
} from "./validation.js";
import { errorCode } from "../errors/types.js";

export function preparePolicyStore(
  sourceValue: unknown,
  bootstrap: Record<string, unknown>,
): void {
  const source = record(sourceValue, ["policyStore"]);
  rejectUnknown(source, INPUT_FIELDS.policyInline, ["policyStore"]);
  const type = field(source, "type", ["policyStore"]);
  if (type !== "inline") {
    return invalid(
      type === undefined
        ? errorCode.inputRequired
        : errorCode.inputUnsupported,
      ["policyStore", "type"],
    );
  }
  const document = field(source, "document", ["policyStore"]);
  if (document === undefined) {
    return invalid(errorCode.inputRequired, ["policyStore", "document"]);
  }
  let snapshot: JsonObject;
  try {
    snapshot = snapshotJsonObject(document, "initialize");
  } catch {
    return invalid(errorCode.inputInvalidType, ["policyStore", "document"]);
  }
  bootstrap.CEDARLING_POLICY_STORE_LOCAL = JSON.stringify(snapshot);
}
