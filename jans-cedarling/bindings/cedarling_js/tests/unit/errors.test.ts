import type QUnitApi from "qunit";

import { createSdkError } from "../../dist/errors/errors.js";

/** Registers private error-normalization unit tests. */
export default function registerErrorTests(QUnit: QUnitApi): void {
  QUnit.module("errors");

  QUnit.test("enumerable diagnostics omit secrets and unsafe nested causes", (assert) => {
    const secret = "super-secret-token"; // # gitleaks:allow
    const nestedCause = new Error(`authorization failed for ${secret}`, {
      cause: {
        policyContent: `permit when token == "${secret}"`,
      },
    });
    const error = createSdkError(
      "INITIALIZATION_FAILED",
      "initialize",
      {
        details: {
          runtimeCapability: "wasm",
          url: `https://user:${secret}@example.test/policies?token=${secret}#fragment`,
          path: `/home/alice/private/${secret}/policy.cedar`,
          token: secret,
          credentials: secret,
          authorizationHeader: `Bearer ${secret}`,
          policyContent: `permit when token == "${secret}"`,
          archiveBytes: new Uint8Array([1, 2, 3]),
          entityAttributes: { token: secret },
          context: { token: secret },
          value: secret,
        },
        cause: nestedCause,
      },
    );

    const serialized = JSON.stringify(error);

    assert.deepEqual(JSON.parse(serialized), {
      name: "CedarlingError",
      code: "INITIALIZATION_FAILED",
      operation: "initialize",
      details: {
        runtimeCapability: "wasm",
        url: "https://example.test/policies",
      },
    });
    assert.false(serialized.includes(secret));
    assert.false(serialized.includes("/home/alice/private"));
    assert.false("cause" in error, "an unsafe cause is omitted");
  });

  QUnit.test("validation issue messages are selected by the SDK", (assert) => {
    const secret = "unsafe-validation-message"; // # gitleaks:allow
    const error = createSdkError("INVALID_INPUT", "authorizeUnsigned", {
      issues: [
        {
          path: ["context"],
          code: "type",
          message: secret,
        },
      ],
    });

    assert.deepEqual(error.issues, [
      {
        path: ["context"],
        code: "type",
        message: "The value has an invalid type.",
      },
    ]);
    assert.false(JSON.stringify(error).includes(secret));
  });

  QUnit.test("a previously normalized safe cause can be retained", (assert) => {
    const secret = "mutated-cause-secret"; // # gitleaks:allow
    const cause = createSdkError("WASM_LOAD_FAILED", "initialize");
    const mutationAccepted = Reflect.set(cause, "policyContent", secret);
    const error = createSdkError(
      "INITIALIZATION_FAILED",
      "initialize",
      { cause },
    );

    assert.false(mutationAccepted, "normalized errors cannot gain unsafe fields");
    assert.strictEqual(error.cause, cause);
    assert.false(JSON.stringify(error).includes(secret));
  });
}
