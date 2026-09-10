import { readFile } from "node:fs/promises";
import { createServer } from "node:http";
import { join } from "node:path";
import type QUnitApi from "qunit";
import { createCedarling } from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";
import { withCedarling } from "../run.js";

const request = {
  principal: { type: "Tracer::User", id: "alice" },
  action: 'Tracer::Action::"Read"',
  resource: { type: "Tracer::Resource", id: "document" },
};

async function archiveBytes(): Promise<Uint8Array> {
  return new Uint8Array(await readFile(
    join(
      import.meta.dirname,
      "..",
      "..",
      "..",
      "tests",
      "fixtures",
      "tracer-policy-store.cjar",
    ),
  ));
}

export default function registerCapabilityContracts(QUnit: QUnitApi): void {
  QUnit.module("complete client capabilities");

  QUnit.test("context and retained logs operate against real WASM", async (assert) => {
    await withCedarling(assert, {
      applicationName: "cedarling-js-pr2-capabilities",
      logging: { type: "memory", level: "trace" },
      contextStore: { maxEntries: 4, metrics: true },
      policyStore: { type: "inline", document: tracerPolicyStore },
    }, async (client) => {
      const authorized = await client.authorizeUnsigned(request);
      assert.true(authorized.ok);
      const source = { nested: { enabled: true } };
      assert.true((await client.context.set("profile", source)).ok);
      source.nested.enabled = false;
      assert.deepEqual(await client.context.get("profile"), {
        ok: true,
        value: { nested: { enabled: true } },
      });
      const ids = await client.logs.ids();
      assert.true(ids.ok);
      if (ids.ok) assert.true(ids.value.length > 0);
      const drained = await client.logs.drain();
      assert.true(drained.ok);
      assert.deepEqual(await client.logs.ids(), { ok: true, value: [] });
      assert.deepEqual(await client.context.delete("profile"), {
        ok: true,
        value: true,
      });
    });
  });

  QUnit.test("issuer readiness exposes configured and unknown issuers", async (assert) => {
    await withCedarling(assert, {
      applicationName: "cedarling-js-pr2-issuer",
      authorization: { dangerouslyDisableSchemaValidation: true },
      jwt: {
        dangerouslyDisableSignatureValidation: true,
        dangerouslyDisableStatusValidation: true,
      },
      issuerLoading: { mode: "sync" },
      policyStore: {
        type: "inline",
        document: createMultiIssuerPolicyStore(),
      },
    }, async (client) => {
      assert.deepEqual(await client.issuers.isLoaded({ id: "TestIssuer" }), {
        ok: true,
        value: true,
      });
      assert.deepEqual(await client.issuers.isLoaded({
        iss: "https://unknown.example",
      }), { ok: true, value: false });
    });
  });

  QUnit.test("archive and application loader policy sources initialize", async (assert) => {
    const bytes = await archiveBytes();
    for (const [name, policyStore] of [
      ["archive", { type: "archive" as const, bytes }],
      ["loader", {
        type: "loader" as const,
        async load() {
          return bytes;
        },
      }],
    ] as const) {
      const created = await createCedarling({
        applicationName: "cedarling-js-pr2-" + name,
        policyStore,
      });
      assert.true(created.ok, name + " initializes");
      if (!created.ok) continue;
      assert.true((await created.value.authorizeUnsigned(request)).ok);
      assert.true((await created.value.shutDown()).ok);
    }
  });

  QUnit.test("URL policy source initializes through managed retrieval", async (assert) => {
    const server = createServer((_request, response) => {
      response.writeHead(200, { "content-type": "application/json" });
      response.end(JSON.stringify(tracerPolicyStore));
    });
    await new Promise<void>((resolve, reject) => {
      server.once("error", reject);
      server.listen(0, "127.0.0.1", () => resolve());
    });
    try {
      const address = server.address();
      if (address === null || typeof address === "string") {
        throw new Error("policy server did not expose a port");
      }
      const created = await createCedarling({
        applicationName: "cedarling-js-pr2-url",
        policyStore: {
          type: "url",
          url: "http://127.0.0.1:" + address.port + "/policy-store",
        },
      });
      assert.true(created.ok);
      if (created.ok) {
        assert.true((await created.value.authorizeUnsigned(request)).ok);
        assert.true((await created.value.shutDown()).ok);
      }
    } finally {
      await new Promise<void>((resolve, reject) => {
        server.close((error) => error === undefined ? resolve() : reject(error));
      });
    }
  });
}
