import type { JsonObject } from "../values/types.js";
import { DEFAULTS, INPUT_FIELDS } from "../helpers/constants.js";
import { snapshotJsonObject } from "../values/snapshot.js";
import { field, httpUrl, invalid, record, refreshInterval, rejectUnknown } from "./validation.js";
import { errorCode } from "../errors/types.js";

/** Detached policy source selected after public option validation. */
export type PreparedPolicySource =
  | { readonly type: "inline"; readonly document: JsonObject }
  | { readonly type: "url"; readonly url: string }
  | { readonly type: "archive"; readonly bytes: Uint8Array }
  | { readonly type: "loader"; readonly load: () => Promise<Uint8Array> }
  | { readonly type: "bootstrap" };

/**
 * Validates and detaches exactly one policy source.
 *
 * Inline and URL sources populate Cedarling-owned bootstrap keys. Archive and
 * loader sources retain only the data needed for generated archive loading.
 */
export function preparePolicyStore(
  sourceValue: unknown,
  bootstrap: Record<string, unknown>,
): PreparedPolicySource {
  const source = record(sourceValue, ["policyStore"]);
  const type = field(source, "type", ["policyStore"]);
  if (typeof type !== "string") {
    return invalid(
      type === undefined
        ? errorCode.inputRequired
        : errorCode.inputInvalidType,
      ["policyStore", "type"],
    );
  }

  switch (type) {
    case "inline": {
      rejectUnknown(source, INPUT_FIELDS.policyInline, ["policyStore"]);
      const document = field(source, "document", ["policyStore"]);
      if (document === undefined) {
        invalid(errorCode.inputRequired, ["policyStore", "document"]);
      }
      let snapshot: JsonObject;
      try {
        snapshot = snapshotJsonObject(document, "initialize");
      } catch {
        return invalid(errorCode.inputInvalidType, [
          "policyStore",
          "document",
        ]);
      }
      bootstrap.CEDARLING_POLICY_STORE_LOCAL = JSON.stringify(snapshot);
      return { type, document: snapshot };
    }
    case "url": {
      rejectUnknown(source, INPUT_FIELDS.policyUrl, ["policyStore"]);
      const url = httpUrl(field(source, "url", ["policyStore"]), [
        "policyStore",
        "url",
      ]);
      const refreshValue = field(source, "refresh", ["policyStore"]);
      const refresh =
        refreshValue === undefined
          ? DEFAULTS.policyRefreshIntervalSeconds
          : (() => {
              const options = record(refreshValue, [
                "policyStore",
                "refresh",
              ]);
              rejectUnknown(
                options,
                INPUT_FIELDS.policyRefresh,
                ["policyStore", "refresh"],
              );
              return refreshInterval(
                field(options, "intervalSeconds", [
                  "policyStore",
                  "refresh",
                ]),
                DEFAULTS.policyRefreshIntervalSeconds,
                ["policyStore", "refresh", "intervalSeconds"],
              ) as number;
            })();
      bootstrap.CEDARLING_POLICY_STORE_URI = url;
      bootstrap.CEDARLING_POLICY_STORE_REFRESH_INTERVAL = refresh;
      return { type, url };
    }
    case "archive": {
      rejectUnknown(source, INPUT_FIELDS.policyArchive, ["policyStore"]);
      const bytes = field(source, "bytes", ["policyStore"]);
      if (!(bytes instanceof Uint8Array)) {
        return invalid(errorCode.inputInvalidType, ["policyStore", "bytes"]);
      }
      if (bytes.byteLength === 0) {
        return invalid(errorCode.inputOutOfRange, ["policyStore", "bytes"]);
      }
      return { type, bytes: new Uint8Array(bytes) };
    }
    case "loader": {
      rejectUnknown(source, INPUT_FIELDS.policyLoader, ["policyStore"]);
      const load = field(source, "load", ["policyStore"]);
      if (typeof load !== "function") {
        return invalid(errorCode.inputInvalidType, ["policyStore", "load"]);
      }
      return { type, load: load as () => Promise<Uint8Array> };
    }
    default:
      return invalid(errorCode.inputUnsupported, ["policyStore", "type"]);
  }
}
