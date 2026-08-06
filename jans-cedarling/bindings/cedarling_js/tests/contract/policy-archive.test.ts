import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import type { RuntimeFixtures } from "../run.js";

/** Registers public in-memory archive policy contracts. */
export default function registerPolicyArchiveTests(
  QUnit: QUnitApi,
  fixtures: RuntimeFixtures,
): void {
  QUnit.module("policy-archive");

  QUnit.test("valid cjar bytes initialize and authorize", async (assert) => {
    const bytes = await fixtures.loadTracerArchive();
    const created = await createCedarling({
      applicationName: "archive-policy",
      policyStore: { type: "archive", bytes },
    });

    assert.true(created.ok, "archive initialization succeeds");
    if (!created.ok) {
      return;
    }
    try {
      const authorized = await created.value.authorizeUnsigned({
        principal: { type: "Tracer::User", id: "alice" },
        action: 'Tracer::Action::"Read"',
        resource: { type: "Tracer::Resource", id: "document" },
      });
      assert.true(authorized.ok);
      if (authorized.ok) {
        assert.true(authorized.value.decision);
        assert.deepEqual(authorized.value.diagnostics.reasons, ["allow"]);
      }
    } finally {
      const closed = await created.value.shutDown();
      assert.true(closed.ok);
    }
  });

  QUnit.test("archive bytes are copied before asynchronous module work", async (assert) => {
    const bytes = await fixtures.loadTracerArchive();
    const pending = createCedarling({
      applicationName: "archive-mutation",
      policyStore: { type: "archive", bytes },
    });
    bytes.fill(0);
    const created = await pending;

    assert.true(created.ok);
    if (created.ok) {
      const closed = await created.value.shutDown();
      assert.true(closed.ok);
    }
  });

  QUnit.test("empty and malformed archives use stable safe failures", async (assert) => {
    const empty = await createCedarling({
      applicationName: "empty-archive",
      policyStore: { type: "archive", bytes: new Uint8Array() },
    });
    const malformedBytes = new TextEncoder().encode(
      "private malformed archive material",
    );
    const malformed = await createCedarling({
      applicationName: "malformed-archive",
      policyStore: { type: "archive", bytes: malformedBytes },
    });

    assert.false(empty.ok);
    if (!empty.ok) {
      assert.strictEqual(empty.error.code, "INVALID_INPUT");
      assert.deepEqual(empty.error.issues?.[0]?.path, [
        "policyStore",
        "bytes",
      ]);
    }
    assert.false(malformed.ok);
    if (!malformed.ok) {
      assert.strictEqual(malformed.error.code, "INITIALIZATION_FAILED");
      assert.false(
        JSON.stringify(malformed.error).includes("private malformed"),
      );
    }
  });
}
