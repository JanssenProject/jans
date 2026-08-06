import type QUnitApi from "qunit";

import {
  createCedarling,
  type AuthorizationResult,
  type CedarlingClient,
} from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

type AssertFalse<Value extends false> = Value;
type AssertTrue<Value extends true> = Value;
type _NamedAuthorizationOnly = AssertFalse<
  "authorize" extends keyof CedarlingClient ? true : false
>;
type _LegacyCloseRemoved = AssertFalse<
  "close" extends keyof CedarlingClient ? true : false
>;
type _ShutDownPresent = AssertTrue<
  "shutDown" extends keyof CedarlingClient ? true : false
>;
type _CanonicalAuthorizationResult = AssertFalse<
  "allowed" extends keyof AuthorizationResult ? true : false
>;

/** Registers the public authorization-method surface contract. */
export default function registerAuthorizationContractTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorization");

  QUnit.test("the public client exposes only the two named authorization methods", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-named-authorization",
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
      assert.strictEqual(typeof created.value.authorizeUnsigned, "function");
      assert.strictEqual(
        typeof created.value.authorizeMultiIssuer,
        "function",
      );
      assert.false(
        "authorize" in created.value,
        "the redundant generic dispatcher is not public",
      );
    } finally {
      assert.true((await created.value.shutDown()).ok);
    }
  });
}
