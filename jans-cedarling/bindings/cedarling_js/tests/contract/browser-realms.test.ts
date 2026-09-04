import type QUnitApi from "qunit";
import {
  createCedarling,
  type PolicyStoreSource,
} from "@janssenproject/cedarling";

async function initializeAndClose(
  assert: Assert,
  name: string,
  policyStore: PolicyStoreSource,
): Promise<void> {
  const created = await createCedarling({
    applicationName: `browser-realm-${name}`,
    policyStore,
  });
  assert.true(
    created.ok,
    created.ok ? `${name} initializes` : `${name}: ${created.error.code}`,
  );
  if (!created.ok) return;

  const shutdown = await created.value.shutDown();
  assert.true(
    shutdown.ok,
    shutdown.ok ? `${name} shuts down` : `${name}: ${shutdown.error.code}`,
  );
}

export default function registerBrowserRealmTests(QUnit: QUnitApi): void {
  QUnit.module("browser-realms");

  QUnit.test("accepts policy sources created in another realm", async (assert) => {
    const frame = document.createElement("iframe");
    document.body.append(frame);
    const realm = frame.contentWindow as unknown as typeof globalThis | null;
    assert.notStrictEqual(realm, null, "the iframe has a realm");
    if (realm === null) {
      frame.remove();
      return;
    }

    try {
      assert.notStrictEqual(realm.URL, URL, "the URL constructor is foreign");
      assert.notStrictEqual(
        realm.Uint8Array,
        Uint8Array,
        "the typed-array constructor is foreign",
      );

      const url = new realm.URL(
        `${location.origin}/tracer-policy-store.cjar`,
      );
      const response = await fetch(url.href);
      assert.true(response.ok, "the policy archive fixture is available");
      const archive = new Uint8Array(await response.arrayBuffer());

      await initializeAndClose(assert, "url", {
        type: "url",
        url: url as unknown as URL,
      });

      const archiveBytes = new realm.Uint8Array(archive);
      await initializeAndClose(assert, "archive", {
        type: "archive",
        bytes: archiveBytes as unknown as Uint8Array,
      });

      const loaderBytes = new realm.Uint8Array(archive);
      await initializeAndClose(assert, "loader", {
        type: "loader",
        async load() {
          return loaderBytes as unknown as Uint8Array;
        },
      });
    } finally {
      frame.remove();
    }
  });
}
