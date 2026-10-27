import {
  createTestTokens,
  exampleConfig,
  issuerUrls,
} from "../fixtures/test-tokens.mjs";

const principal = {
  cedar_entity_mapping: { entity_type: "Example::User", id: "alice" },
};

function unsignedItem(documentId) {
  return {
    action: 'Example::Action::"Read"',
    resource: {
      cedar_entity_mapping: {
        entity_type: "Example::Document",
        id: documentId,
      },
    },
    context: {},
  };
}

function multiIssuerItem(orgId) {
  return {
    action: 'MultiIssuer::Action::"Read"',
    resource: {
      cedar_entity_mapping: {
        entity_type: "MultiIssuer::Resource",
        id: "document",
      },
      org_id: orgId,
    },
    context: {},
  };
}

/** Snapshot a generated resource before releasing its WebAssembly allocation. */
function consumeJson(resource) {
  if (resource === undefined || resource === null) return resource;
  try {
    return JSON.parse(resource.jsonString());
  } finally {
    resource.free();
  }
}

function consumeBatch(response) {
  const batch_id = response.batch_id;
  const items = response.results;
  try {
    return {
      batch_id,
      items: items.map((item) => {
        try {
          if (item.is_ok) return consumeJson(item.unwrap());
          const error = item.error;
          try {
            return {
              category: error?.category,
              item_index: error?.item_index,
              message: error?.message,
            };
          } finally {
            error?.free();
          }
        } finally {
          item.free();
        }
      }),
    };
  } finally {
    response.free();
  }
}

/** Fetch a Cedar archive as the bytes accepted by initFromArchiveBytes(). */
export async function fetchArchive(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Policy archive request failed with ${response.status}`);
  }
  return new Uint8Array(await response.arrayBuffer());
}

/**
 * Exercise Cedarling's public services and report each canonical result.
 * @param {Function} initFromArchiveBytes Package initialization function.
 * @param {ArrayBuffer|Uint8Array} archiveBytes Compiled Cedar archive bytes.
 * @param {(record: {section: string, label: string, value: unknown}) => void} [onResult]
 * @returns {Promise<Array<{section: string, label: string, value: unknown}>>}
 */
export async function runCedarling(
  initFromArchiveBytes,
  archiveBytes,
  onResult = () => {},
) {
  const records = [];
  const report = (section, label, value) => {
    const record = { section, label, value };
    records.push(record);
    onResult(record);
    return value;
  };
  const reportJson = async (section, label, result) =>
    report(section, label, consumeJson(await result));
  const reportAll = (section, entries) =>
    entries.forEach(([label, value]) => report(section, label, value));
  const reportBatch = (label, batch) => {
    report("Batch authorization", `${label} batch ID`, batch.batch_id);
    batch.items.forEach((value, index) =>
      report("Batch authorization", `${label} item ${index + 1}`, value),
    );
  };
  const bytes =
    archiveBytes instanceof Uint8Array
      ? archiveBytes
      : new Uint8Array(archiveBytes);
  let cedarling;
  let operationsCompleted = false;

  try {
    cedarling = await initFromArchiveBytes(exampleConfig, bytes);
    report("Initialization", "Cedarling initialized", true);

    // Generated authorization methods accept their request as JSON text.
    const authorizeUnsigned = async (label, documentId) =>
      reportJson(
        "Unsigned authorization",
        label,
        cedarling.authorizeUnsigned(
          JSON.stringify({ principal, ...unsignedItem(documentId) }),
        ),
      );
    const unsignedAllow = await authorizeUnsigned(
      "Allowed document",
      "document-1",
    );
    await authorizeUnsigned("Denied document", "document-2");

    const tokens = createTestTokens();
    const authorizeMultiIssuer = async (label, selectedTokens, orgId) =>
      reportJson(
        "Multi-issuer authorization",
        label,
        cedarling.authorizeMultiIssuer(
          JSON.stringify({
            tokens: selectedTokens,
            ...multiIssuerItem(orgId),
          }),
        ),
      );
    await authorizeMultiIssuer("Matching issuers", tokens, "example");
    await authorizeMultiIssuer("Acme token only", [tokens[0]], "example");
    await authorizeMultiIssuer("Dolphin token only", [tokens[1]], "example");

    // Batch wrappers own their item wrappers, so copy each result before free().
    const unsignedBatch = consumeBatch(
      await cedarling.authorizeUnsignedBatch(
        JSON.stringify({
          principal,
          items: [unsignedItem("document-1"), unsignedItem("document-2")],
        }),
      ),
    );
    reportBatch("Unsigned", unsignedBatch);

    const multiIssuerBatch = consumeBatch(
      await cedarling.authorizeMultiIssuerBatch(
        JSON.stringify({
          tokens,
          items: [multiIssuerItem("example"), multiIssuerItem("different")],
        }),
      ),
    );
    reportBatch("Multi-issuer", multiIssuerBatch);

    const policyIds = unsignedAllow.response.diagnostics.reason;
    reportAll("Policy annotations", [
      ["Merged annotations", cedarling.annotationsMap(policyIds)],
      [
        "Description values",
        cedarling.annotationValues(policyIds, "description"),
      ],
      ["Annotations by policy", cedarling.annotationsByPolicy(policyIds)],
    ]);

    // The optional third argument is the context entry's time-to-live in seconds.
    cedarling.pushDataCtx("account:alice", { plan: "pro" }, 120n);
    cedarling.pushDataCtx("request:temporary", { approved: true });
    reportAll("Context data", [
      ["Stored value", cedarling.getDataCtx("account:alice")],
      ["Stored entry", consumeJson(cedarling.getDataEntryCtx("account:alice"))],
    ]);
    const entries = cedarling.listDataCtx();
    report("Context data", "All entries", entries.map(consumeJson));
    report(
      "Context data",
      "Store statistics",
      consumeJson(cedarling.getStatsCtx()),
    );
    report(
      "Context data",
      "Entry removed",
      cedarling.removeDataCtx("account:alice"),
    );
    cedarling.clearDataCtx();
    report("Context data", "Entries after clear", cedarling.listDataCtx());

    reportAll("Trusted issuers", [
      ["Total issuers", cedarling.totalIssuers()],
      ["Loaded issuer count", cedarling.loadedTrustedIssuersCount()],
      ["Loaded issuer IDs", cedarling.loadedTrustedIssuerIds()],
      ["Failed issuer IDs", cedarling.failedTrustedIssuerIds()],
      ["Acme loaded by name", cedarling.isTrustedIssuerLoadedByName("Acme")],
      [
        "Acme loaded by issuer URL",
        cedarling.isTrustedIssuerLoadedByIss(issuerUrls.Acme),
      ],
    ]);

    const logIds = cedarling.getLogIds();
    report("Logs", "Log IDs", logIds);
    reportAll("Logs", [
      [
        "First log by ID",
        logIds.length ? cedarling.getLogById(logIds[0]) : null,
      ],
      ["Decision logs", cedarling.getLogsByTag("Decision")],
      [
        "Logs for allowed request",
        cedarling.getLogsByRequestId(unsignedAllow.request_id),
      ],
      [
        "Decision logs for allowed request",
        cedarling.getLogsByRequestIdAndTag(
          unsignedAllow.request_id,
          "Decision",
        ),
      ],
    ]);
    // popLogs() returns and removes all retained in-memory log entries.
    report("Logs", "Drained logs", cedarling.popLogs());
    operationsCompleted = true;
  } finally {
    if (cedarling) {
      // Always stop Cedarling after successful initialization, including when
      // an authorization operation or result reporter throws.
      try {
        await cedarling.shutDown();
        if (operationsCompleted) {
          report("Shutdown", "Cedarling stopped", true);
        }
      } finally {
        cedarling.free();
      }
    }
  }
  return records;
}
