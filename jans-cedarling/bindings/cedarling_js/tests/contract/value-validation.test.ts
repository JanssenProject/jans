import type QUnitApi from "qunit";
import { createCedarling } from "@janssenproject/cedarling";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

/** Recursively removes readonly modifiers for post-call mutation probes. */
type Mutable<T> = T extends string
  ? string
  : T extends number
    ? number
    : T extends boolean
      ? boolean
      : {
          -readonly [Key in keyof T]: Mutable<T[Key]>;
        };

/** Registers public value-validation and detachment contract tests. */
export default function registerValueValidationTests(QUnit: QUnitApi): void {
  // Registration begins only after the host runner configures QUnit.
  QUnit.module("value-validation");

  QUnit.test("inline policy data is detached before asynchronous initialization", async (assert) => {
    const document = JSON.parse(
      JSON.stringify(tracerPolicyStore),
    ) as Mutable<
      typeof tracerPolicyStore
    >;
    const creation = createCedarling({
      applicationName: "cedarling-js-value-snapshot",
      policyStore: {
        type: "inline",
        document,
      },
    });

    document.policy_stores.tracer.policies.allow.policy_content.body =
      "not valid Cedar";

    const created = await creation;

    assert.true(created.ok, "initialization uses the accepted snapshot");
    if (!created.ok) {
      return;
    }

    try {
      const authorized = await created.value.authorizeUnsigned({
        principal: {
          type: "Tracer::User",
          id: "alice",
        },
        action: 'Tracer::Action::"Read"',
        resource: {
          type: "Tracer::Resource",
          id: "document",
        },
      });

      assert.true(authorized.ok);
      if (authorized.ok) {
        assert.true(authorized.value.decision);
      }
    } finally {
      await created.value.shutDown();
    }
  });

  QUnit.test("authorization rejects entity accessors without invoking them", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-request-validation",
      policyStore: {
        type: "inline",
        document: tracerPolicyStore,
      },
    });

    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    let getterInvoked = false;
    const attributes = {};
    Object.defineProperty(attributes, "score", {
      enumerable: true,
      get() {
        getterInvoked = true;
        return 7;
      },
    });

    try {
      const authorized = await created.value.authorizeUnsigned({
        principal: {
          type: "Tracer::User",
          id: "alice",
          attributes,
        },
        action: 'Tracer::Action::"Read"',
        resource: {
          type: "Tracer::Resource",
          id: "document",
        },
      });

      assert.false(authorized.ok, "the unsafe request is rejected");
      assert.false(getterInvoked, "the accessor is never invoked");
    } finally {
      await created.value.shutDown();
    }
  });
  // The shared contract contains no host startup or exit handling.
}
