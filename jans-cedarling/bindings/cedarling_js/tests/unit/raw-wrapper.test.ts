import type { Cedarling as GeneratedCedarling } from "@janssenproject/cedarling_wasm";
import type QUnitApi from "qunit";

import { createCedarlingApi } from "../../src/client.js";

function jsonWrapper(events: string[], label: string, value: unknown): object {
  return {
    json_string() {
      return JSON.stringify(value);
    },
    free() {
      events.push(label);
    },
  };
}

function authorization(events: string[], label: string, decision = true): object {
  return jsonWrapper(events, label, {
    decision,
    request_id: label,
    response: { decision, diagnostics: { reason: [label], errors: [] } },
  });
}

function batch(events: string[], label: string): object {
  return {
    batch_id: label,
    results: [
      {
        is_ok: true,
        unwrap() {
          return authorization(events, label + ":result");
        },
        free() {
          events.push(label + ":item:ok");
        },
      },
      {
        is_ok: false,
        error: {
          category: "request_validation",
          item_index: 1,
          message: "invalid",
          free() {
            events.push(label + ":error");
          },
        },
        free() {
          events.push(label + ":item:error");
        },
      },
    ],
    free() {
      events.push(label + ":batch");
    },
  };
}

function containsGeneratedValue(value: unknown, visited = new Set<object>()): boolean {
  if (typeof value !== "object" || value === null || visited.has(value)) return false;
  visited.add(value);
  if ("free" in value || Symbol.dispose in value) return true;
  return Reflect.ownKeys(value).some((key) =>
    containsGeneratedValue(Reflect.get(value, key), visited)
  );
}

function generatedClient(events: string[]): GeneratedCedarling {
  const stored = { role: "editor" };
  return {
    annotation_values() { return ["redirect"]; },
    annotations_by_policy() { return new Map([["allow", { redirect: "/home" }]]); },
    annotations_map() { return { redirect: "/home" }; },
    async authorize_multi_issuer() { return authorization(events, "multi"); },
    async authorize_multi_issuer_batch() { return batch(events, "multi-batch"); },
    async authorize_unsigned() { return authorization(events, "unsigned"); },
    async authorize_unsigned_batch() { return batch(events, "unsigned-batch"); },
    clear_data_ctx() { events.push("clear"); },
    failed_trusted_issuer_ids() { return ["failed"]; },
    get_data_ctx() { return stored; },
    get_data_entry_ctx() { return jsonWrapper(events, "entry", { key: "session", value: stored }); },
    get_log_by_id() { return { id: "log-1" }; },
    get_log_ids() { return ["log-1"]; },
    get_logs_by_request_id() { return [{ request_id: "request" }]; },
    get_logs_by_request_id_and_tag() { return [{ request_id: "request", tag: "Decision" }]; },
    get_logs_by_tag() { return [{ tag: "Decision" }]; },
    get_stats_ctx() { return jsonWrapper(events, "stats", { entry_count: 1 }); },
    is_trusted_issuer_loaded_by_iss() { return true; },
    is_trusted_issuer_loaded_by_name() { return false; },
    list_data_ctx() { return [jsonWrapper(events, "entries", { key: "session", value: stored })]; },
    loaded_trusted_issuer_ids() { return ["loaded"]; },
    loaded_trusted_issuers_count() { return 1; },
    pop_logs() { return [{ id: "log-1" }]; },
    push_data_ctx(key: string, value: unknown, ttl: bigint | null | undefined) { events.push("push:" + key + ":" + JSON.stringify(value) + ":" + String(ttl)); },
    remove_data_ctx() { return true; },
    async shut_down() { events.push("shut_down"); },
    total_issuers() { return 2; },
    free() { events.push("client.free"); },
  } as unknown as GeneratedCedarling;
}

export default function registerRawWrapperTests(QUnit: QUnitApi): void {
  QUnit.module("raw wrapper");

  QUnit.test("forwards raw properties and archive bytes unchanged", async (assert) => {
    const events: string[] = [];
    const rawProperties = {
      CEDARLING_APPLICATION_NAME: "raw-wrapper-unit",
      CEDARLING_POLICY_STORE_LOCAL: "{}",
      nested: { preserved: true },
    };
    const archive = new Uint8Array([1, 2, 3]);
    let moduleInitializations = 0;
    let receivedProperties: unknown;
    let receivedArchive: unknown;
    const api = createCedarlingApi(
      async () => { moduleInitializations += 1; },
      {
        async init(properties) {
          receivedProperties = properties;
          return generatedClient(events);
        },
        async initFromArchiveBytes(properties, bytes) {
          receivedProperties = properties;
          receivedArchive = bytes;
          return generatedClient(events);
        },
      },
    );

    const ordinary = await api.init(rawProperties);
    assert.strictEqual(receivedProperties, rawProperties, "init receives the original properties object");
    await ordinary.shutDown();
    const archived = await api.initFromArchiveBytes(rawProperties, archive);
    assert.strictEqual(receivedProperties, rawProperties, "archive init receives the original properties object");
    assert.strictEqual(receivedArchive, archive, "archive init receives the original Uint8Array");
    assert.strictEqual(moduleInitializations, 1, "the runtime module initializes once");
    await archived.shutDown();
  });

  QUnit.test("copies every result and releases every generated wrapper", async (assert) => {
    const events: string[] = [];
    const api = createCedarlingApi(async () => {}, {
      async init() { return generatedClient(events); },
      async initFromArchiveBytes() { return generatedClient(events); },
    });
    const cedarling = await api.init({ CEDARLING_APPLICATION_NAME: "unit" });
    const values = [
      cedarling.annotationValues(["allow"], "redirect"),
      cedarling.annotationsByPolicy(["allow"]),
      cedarling.annotationsMap(["allow"]),
      await cedarling.authorizeUnsigned("{}"),
      await cedarling.authorizeMultiIssuer("{}"),
      await cedarling.authorizeUnsignedBatch("{}"),
      await cedarling.authorizeMultiIssuerBatch("{}"),
      cedarling.failedTrustedIssuerIds(),
      cedarling.getDataCtx("session"),
      cedarling.getDataEntryCtx("session"),
      cedarling.getLogById("log-1"),
      cedarling.getLogIds(),
      cedarling.getLogsByRequestId("request"),
      cedarling.getLogsByRequestIdAndTag("request", "Decision"),
      cedarling.getLogsByTag("Decision"),
      cedarling.getStatsCtx(),
      cedarling.listDataCtx(),
      cedarling.loadedTrustedIssuerIds(),
      cedarling.popLogs(),
    ];
    cedarling.clearDataCtx();
    cedarling.pushDataCtx("session", { role: "editor" }, 60n);

    assert.true(values.every((value) => !containsGeneratedValue(value)), "no generated wrapper or disposal hook escapes");
    assert.true(cedarling.isTrustedIssuerLoadedByIss("https://issuer.example"));
    assert.false(cedarling.isTrustedIssuerLoadedByName("issuer"));
    assert.true(cedarling.removeDataCtx("session"));
    assert.strictEqual(cedarling.loadedTrustedIssuersCount(), 1);
    assert.strictEqual(cedarling.totalIssuers(), 2);
    for (const event of [
      "unsigned", "multi", "entry", "entries", "stats",
      "unsigned-batch:batch", "unsigned-batch:item:ok", "unsigned-batch:result", "unsigned-batch:error",
      "multi-batch:batch", "clear",
    ]) {
      assert.true(events.includes(event), event + " is released or delegated");
    }
    assert.true(events.some((event) => event.startsWith("push:session:")), "context input is forwarded directly");
    await cedarling.shutDown();
    assert.deepEqual(events.slice(-2), ["shut_down", "client.free"], "shutdown calls the generated lifecycle then frees the client");
  });

  QUnit.test("releases the generated client when shutdown rejects", async (assert) => {
    const failure = new Error("shutdown failed");
    let freed = 0;
    const api = createCedarlingApi(async () => {}, {
      async init() {
        return {
          ...generatedClient([]),
          async shut_down() { throw failure; },
          free() { freed += 1; },
        } as unknown as GeneratedCedarling;
      },
      async initFromArchiveBytes() { throw new Error("unused"); },
    });
    const cedarling = await api.init({});
    await assert.rejects(cedarling.shutDown(), (error: unknown) => error === failure);
    assert.strictEqual(freed, 1, "shutdown failure still releases the generated client");
  });
}
