import type QUnitApi from "qunit";
import { assertCedarlingError } from "../run.js";

import { createClientForEngine } from "../../dist/client/client.js";
import { createTestEngine } from "./engine-fixture.js";

export default function registerIssuerUnitTests(QUnit: QUnitApi): void {
  QUnit.module("issuers");

  QUnit.test("validates exact-one references before observing the engine", async (assert) => {
    let calls = 0;
    const client = createClientForEngine(createTestEngine({
      async isIssuerLoaded() {
        calls += 1;
        return false;
      },
    }));

    for (
      const { reference, code, path } of [
        { reference: {}, code: "INPUT_CONFLICT", path: [] },
        {
          reference: { id: "issuer", iss: "https://issuer.example" },
          code: "INPUT_CONFLICT",
          path: [],
        },
        { reference: { id: "" }, code: "INPUT_REQUIRED", path: ["id"] },
        { reference: { iss: " " }, code: "INPUT_REQUIRED", path: ["iss"] },
        {
          reference: { id: null },
          code: "INPUT_INVALID_TYPE",
          path: ["id"],
        },
        {
          reference: { iss: null },
          code: "INPUT_INVALID_TYPE",
          path: ["iss"],
        },
        {
          reference: { id: "issuer", extra: true },
          code: "INPUT_UNKNOWN_FIELD",
          path: ["extra"],
        },
      ] as const
    ) {
      const result = await client.issuers.isLoaded(reference as never);
      assertCedarlingError(assert, result, {
        code,
        operation: "issuers.isLoaded",
        path,
      });
    }
    assert.strictEqual(calls, 0);
    await client.shutDown();
  });

  QUnit.test("normalizes opaque issuer failures without retaining secrets", async (assert) => {
    const secret = "raw-issuer-observation-secret"; // # gitleaks:allow
    const client = createClientForEngine(createTestEngine({
      async isIssuerLoaded() {
        throw new Error(secret);
      },
    }));

    const result = await client.issuers.isLoaded({ id: "issuer" });
    assertCedarlingError(assert, result, {
      code: "ISSUER_OPERATION_FAILED",
      operation: "issuers.isLoaded",
    }, (error) => {
      assert.strictEqual(error.details, undefined);
      assert.false("cause" in error);
      assert.false(JSON.stringify(error).includes(secret));
    });
    await client.shutDown();
  });
}
