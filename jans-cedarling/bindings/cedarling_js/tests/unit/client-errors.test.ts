import type QUnitApi from "qunit";

import {
  createCedarlingForEngine,
  createClientForEngine,
} from "../../dist/client/client.js";
import { createSdkError } from "../../dist/errors/errors.js";
import { createTestEngine } from "./engine-fixture.js";

const request = {
  principal: { type: "Errors::User", id: "alice" },
  action: 'Errors::Action::"Read"',
  resource: { type: "Errors::Resource", id: "document" },
};

/** Registers public-result normalization tests for facade-owned operations. */
export default function registerClientErrorTests(QUnit: QUnitApi): void {
  QUnit.module("client-errors");

  QUnit.test("normalizes opaque initialization failures", async (assert) => {
    const secret = "private-initialization-detail"; // # gitleaks:allow
    const createCedarling = createCedarlingForEngine(async () => {
      throw { secret };
    });

    const result = await createCedarling({
      applicationName: "client-errors-initialization",
      policyStore: { type: "inline", document: {} },
    });

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "INITIALIZATION_FAILED");
      assert.strictEqual(result.error.operation, "initialize");
      assert.false(JSON.stringify(result.error).includes(secret));
    }
  });

  QUnit.test("normalizes heterogeneous authorization failures", async (assert) => {
    const secret = "private-authorization-detail"; // # gitleaks:allow
    let failure: unknown = new Error(secret);
    const client = createClientForEngine(createTestEngine({
      async authorizeUnsigned() {
        throw failure;
      },
    }));

    for (failure of [
      new Error(secret),
      secret,
      { nested: { secret } },
    ]) {
      const result = await client.authorizeUnsigned(request);
      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "AUTHORIZATION_FAILED");
        assert.strictEqual(result.error.operation, "authorizeUnsigned");
        assert.false(JSON.stringify(result.error).includes(secret));
      }
    }

    await client.shutDown();
  });

  QUnit.test("preserves recognized authorization failures", async (assert) => {
    const expected = createSdkError(
      "RESULT_CONVERSION_FAILED",
      "authorizeUnsigned",
    );
    const client = createClientForEngine(createTestEngine({
      async authorizeUnsigned() {
        throw expected;
      },
    }));

    const result = await client.authorizeUnsigned(request);

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error, expected);
    }
    await client.shutDown();
  });

  QUnit.test("normalizes and memoizes opaque shutdown failures", async (assert) => {
    const secret = "private-shutdown-detail"; // # gitleaks:allow
    const client = createClientForEngine(createTestEngine({
      async shutDown() {
        throw { secret };
      },
    }));

    const firstPromise = client.shutDown();
    const secondPromise = client.shutDown();
    const first = await firstPromise;

    assert.strictEqual(secondPromise, firstPromise);
    assert.false(first.ok);
    if (!first.ok) {
      assert.strictEqual(first.error.code, "LIFECYCLE_FAILED");
      assert.strictEqual(first.error.operation, "shutDown");
      assert.false(JSON.stringify(first.error).includes(secret));
    }
    assert.strictEqual(await client.shutDown(), first);
  });
}
