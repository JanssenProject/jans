import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/** Registers public decision-log contracts against real WASM. */
export default function registerLogsContractTests(QUnit: QUnitApi): void {
  QUnit.module("logs");

  QUnit.test("closed log operations return CLIENT_CLOSED", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-logs-closed",
      logging: { type: "memory", level: "trace", ttlSeconds: 60 },
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    assert.true((await created.value.close()).ok);

    const find = await created.value.logs.find();
    assert.false(find.ok);
    if (!find.ok) {
      assert.strictEqual(find.error.code, "CLIENT_CLOSED");
      assert.strictEqual(find.error.operation, "logs.find");
    }

    const ids = await created.value.logs.ids();
    assert.false(ids.ok);
    if (!ids.ok) {
      assert.strictEqual(ids.error.code, "CLIENT_CLOSED");
      assert.strictEqual(ids.error.operation, "logs.ids");
    }

    const drain = await created.value.logs.drain();
    assert.false(drain.ok);
    if (!drain.ok) {
      assert.strictEqual(drain.error.code, "CLIENT_CLOSED");
      assert.strictEqual(drain.error.operation, "logs.drain");
    }
  });

  QUnit.test("finds a retained decision by authorization request ID", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-logs-decision",
      logging: { type: "memory", level: "trace", ttlSeconds: 60 },
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
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
      if (!authorized.ok) {
        return;
      }

      const found = await created.value.logs.find({
        requestId: authorized.value.requestId,
      });

      assert.true(found.ok);
      if (found.ok) {
        const decision = found.value.find(
          (entry) => entry.kind === "decision",
        );
        assert.ok(decision, "one decision log is retained");
        assert.strictEqual(
          decision?.requestId,
          authorized.value.requestId,
        );
        assert.strictEqual(decision?.applicationId, "cedarling-js-logs-decision");
        assert.strictEqual(typeof decision?.pdpId, "string");
        assert.strictEqual(decision?.payload.decision, "ALLOW");
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("supports ID, tag, all-log, and destructive drain semantics", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-logs-queries",
      logging: { type: "memory", level: "trace", ttlSeconds: 60 },
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
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
      if (!authorized.ok) {
        return;
      }

      const ids = await created.value.logs.ids();
      assert.true(ids.ok);
      if (!ids.ok) {
        return;
      }
      assert.true(ids.value.length > 0);

      const byId = await created.value.logs.find({ id: ids.value[0] ?? "" });
      assert.true(byId.ok);
      if (byId.ok) {
        assert.strictEqual(byId.value.length, 1);
        assert.strictEqual(byId.value[0]?.id, ids.value[0]);
      }

      const all = await created.value.logs.find();
      const decisions = await created.value.logs.find({ tag: "decision" });
      const requestDecisions = await created.value.logs.find({
        requestId: authorized.value.requestId,
        tag: "decision",
      });
      assert.true(all.ok);
      assert.true(decisions.ok);
      assert.true(requestDecisions.ok);
      if (all.ok && decisions.ok && requestDecisions.ok) {
        assert.true(all.value.length > 0);
        assert.true(decisions.value.every((entry) => entry.kind === "decision"));
        assert.true(
          requestDecisions.value.every(
            (entry) =>
              entry.kind === "decision" &&
              entry.requestId === authorized.value.requestId,
          ),
        );
      }

      const drained = await created.value.logs.drain();
      assert.true(drained.ok);
      if (drained.ok) {
        assert.true(drained.value.length > 0);
      }
      const empty = await created.value.logs.drain();
      const emptyIds = await created.value.logs.ids();
      assert.deepEqual(empty, { ok: true, value: [] });
      assert.deepEqual(emptyIds, { ok: true, value: [] });
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("reports unavailable storage instead of an ambiguous empty result", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-logs-off",
      logging: { type: "off" },
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      for (const result of [
        await created.value.logs.ids(),
        await created.value.logs.find(),
        await created.value.logs.drain(),
      ]) {
        assert.false(result.ok);
        if (!result.ok) {
          assert.strictEqual(result.error.code, "LOG_STORAGE_UNAVAILABLE");
        }
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("preserves the generated store's lazy-expiry distinctions", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-logs-expiry",
      logging: { type: "memory", level: "trace", ttlSeconds: 1 },
      policyStore: { type: "inline", document: tracerPolicyStore },
    });
    assert.true(created.ok);
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
      if (!authorized.ok) {
        return;
      }

      // Wait 2s (2× the configured TTL) to allow the 1s TTL to expire,
      // tolerating slow CI machines and timer drift.
      await new Promise((resolve) => setTimeout(resolve, 2_000));

      const ids = await created.value.logs.ids();
      const indexed = await created.value.logs.find({
        requestId: authorized.value.requestId,
      });
      const all = await created.value.logs.find();
      assert.true(ids.ok);
      assert.true(indexed.ok);
      assert.true(all.ok);
      if (ids.ok && indexed.ok && all.ok) {
        assert.true(
          ids.value.length >= all.value.length,
          "physical ID enumeration can retain entries filtered by ID lookup",
        );
        assert.true(
          indexed.value.length >=
            all.value.filter(
              (entry) =>
                entry.requestId === authorized.value.requestId,
            ).length,
          "the request index can retain lazily expired entries",
        );
      }
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });
}
