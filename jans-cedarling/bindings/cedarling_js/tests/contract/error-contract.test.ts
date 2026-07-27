import type QUnitApi from "qunit";
import {
  createCedarling,
  type CedarlingOptions,
  type UnsignedAuthorizationRequest,
} from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/** Registers public error normalization and redaction contract tests. */
export default function registerErrorContractTests(QUnit: QUnitApi): void {
  // Registration begins only after the host runner configures QUnit.
  QUnit.module("error-contract");

  QUnit.test("invalid initialization input is normalized before WASM work", async (assert) => {
    const secret = "policy-accessor-secret"; // # gitleaks:allow
    let getterInvoked = false;
    const document = {};
    Object.defineProperty(document, "policy", {
      enumerable: true,
      get() {
        getterInvoked = true;
        return secret;
      },
    });

    const result = await createCedarling({
      applicationName: "cedarling-js-invalid-input",
      policyStore: {
        type: "inline",
        document,
      },
    } as CedarlingOptions);

    assert.false(result.ok);
    if (result.ok) {
      return;
    }

    assert.strictEqual(result.error.code, "INVALID_INPUT");
    assert.strictEqual(result.error.operation, "initialize");
    assert.deepEqual(result.error.issues, [
      {
        path: ["policyStore", "document"],
        code: "type",
        message: "The value has an invalid type.",
      },
    ]);
    assert.false(getterInvoked, "the invalid accessor is not invoked");
    assert.false(JSON.stringify(result.error).includes(secret));
  });

  QUnit.test("request validation and opaque WASM failures use distinct stage codes", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-error-stages",
      policyStore: {
        type: "inline",
        document: tracerPolicyStore,
      },
    });

    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      const request = {
        action: 'Tracer::Action::"Read"',
        resource: {
          type: "Tracer::Resource",
          id: "document",
        },
        context: {
          rawFloat: 1.25,
        },
      } as unknown as UnsignedAuthorizationRequest;
      const result = await created.value.authorizeUnsigned(request);

      assert.false(result.ok);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
        assert.strictEqual(result.err, result.error, "err aliases the normalized error");
        assert.false(result.allowed, "operational failures are not allowed");
        assert.false(result.denied, "operational failures are not policy denials");
        assert.strictEqual(result.error.operation, "authorizeUnsigned");
        assert.deepEqual(result.error.issues?.[0]?.path, ["context"]);
      }
    } finally {
      await created.value.close();
    }

    const secret = "policy-source-secret"; // # gitleaks:allow
    const failed = await createCedarling({
      applicationName: "cedarling-js-init-failure",
      policyStore: {
        type: "inline",
        document: {
          policy_stores: {
            invalid: {
              policies: {
                secret: {
                  policy_content: {
                    type: "cedar",
                    body: `not valid Cedar ${secret}`,
                  },
                },
              },
            },
          },
        },
      },
    });

    assert.false(failed.ok);
    if (!failed.ok) {
      assert.strictEqual(failed.error.code, "INITIALIZATION_FAILED");
      assert.strictEqual(failed.error.operation, "initialize");
      assert.false(JSON.stringify(failed.error).includes(secret));
      assert.false(failed.error.message.includes(secret));
    }
  });
  // The shared contract contains no host startup or exit handling.
}
