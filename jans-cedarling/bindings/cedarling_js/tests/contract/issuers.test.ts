import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";

/** Registers real-WASM public trusted-issuer readiness contracts. */
export default function registerIssuerContractTests(QUnit: QUnitApi): void {
  QUnit.module("issuers");

  QUnit.test("observes one configured issuer by ID and iss", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-issuer-observation",
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
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      assert.deepEqual(
        await created.value.issuers.isLoaded({ id: "TestIssuer" }),
        { ok: true, value: true },
      );
      assert.deepEqual(
        await created.value.issuers.isLoaded({
          iss: "https://issuer.example",
        }),
        { ok: true, value: true },
      );
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("reports unknown issuers as normal observations", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-issuer-status",
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
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    try {
      assert.deepEqual(
        await created.value.issuers.isLoaded({ id: "UnknownIssuer" }),
        { ok: true, value: false },
      );
      assert.deepEqual(
        await created.value.issuers.isLoaded({
          iss: "https://unknown.example",
        }),
        { ok: true, value: false },
      );
    } finally {
      assert.true((await created.value.close()).ok);
    }
  });

  QUnit.test("closed issuer observation returns CLIENT_CLOSED without inspecting input", async (assert) => {
    const created = await createCedarling({
      applicationName: "cedarling-js-issuer-closed",
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
    });
    assert.true(created.ok);
    if (!created.ok) {
      return;
    }

    assert.true((await created.value.close()).ok);

    let inspections = 0;
    const reference = new Proxy(
      { id: "TestIssuer" } as const,
      {
        getPrototypeOf(target) {
          inspections += 1;
          return Reflect.getPrototypeOf(target);
        },
        getOwnPropertyDescriptor(target, key) {
          inspections += 1;
          return Reflect.getOwnPropertyDescriptor(target, key);
        },
      },
    );

    const observed = await created.value.issuers.isLoaded(reference);

    assert.false(observed.ok);
    if (!observed.ok) {
      assert.strictEqual(observed.error.code, "CLIENT_CLOSED");
      assert.strictEqual(observed.error.operation, "issuers.isLoaded");
    }
    assert.strictEqual(inspections, 0, "the closed client does not inspect input");
  });
}
