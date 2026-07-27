import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import { createSdkError } from "../../dist/errors/errors.js";
import { createTestEngine } from "./engine-fixture.js";

const unsignedDecision = {
  decision: true,
  requestId: "dispatch-unsigned",
  diagnostics: { reasons: ["unsigned"], errors: [] },
};

const multiIssuerDecision = {
  decision: false,
  requestId: "dispatch-multi-issuer",
  diagnostics: { reasons: [], errors: [] },
};

/** Registers deterministic discriminated authorization dispatch tests. */
export default function registerAuthorizationDispatchUnitTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-dispatch");

  QUnit.test("delegates each envelope to exactly one named engine path", async (assert) => {
    let unsignedCalls = 0;
    let multiIssuerCalls = 0;
    const engine = createTestEngine({
      async authorizeUnsigned() {
        unsignedCalls += 1;
        return unsignedDecision;
      },
      async authorizeMultiIssuer() {
        multiIssuerCalls += 1;
        return multiIssuerDecision;
      },
    });
    const client = createClientForEngine(engine);

    const unsigned = await client.authorize({
      type: "unsigned",
      request: {
        action: 'Example::Action::"Read"',
        resource: { type: "Example::Resource", id: "document" },
      },
    });
    const multiIssuer = await client.authorize({
      type: "multiIssuer",
      request: {
        tokens: [
          {
            mapping: "Authorization::AccessToken",
            payload: "header.payload.signature",
          },
        ],
        action: 'Example::Action::"Read"',
        resource: { type: "Example::Resource", id: "document" },
      },
    });

    assert.true(unsigned.ok);
    if (unsigned.ok) {
      assert.strictEqual(unsigned.value.requestId, "dispatch-unsigned");
    }
    assert.true(multiIssuer.ok);
    if (multiIssuer.ok) {
      assert.strictEqual(
        multiIssuer.value.requestId,
        "dispatch-multi-issuer",
      );
    }
    assert.strictEqual(unsignedCalls, 1);
    assert.strictEqual(multiIssuerCalls, 1);
    await client.close();
  });

  QUnit.test("rejects invalid envelopes without invoking caller behavior", async (assert) => {
    let calls = 0;
    const engine = createTestEngine({
      async authorizeUnsigned() {
        calls += 1;
        return unsignedDecision;
      },
      async authorizeMultiIssuer() {
        calls += 1;
        return multiIssuerDecision;
      },
    });
    const client = createClientForEngine(engine);

    let reads = 0;
    const unknown = Object.defineProperties({}, {
      type: {
        enumerable: true,
        value: "future",
      },
      request: {
        enumerable: true,
        get() {
          reads += 1;
          return {};
        },
      },
    });
    const unknownResult = await client.authorize(unknown as never);

    assert.false(unknownResult.ok);
    if (!unknownResult.ok) {
      assert.strictEqual(unknownResult.error.code, "INVALID_INPUT");
      assert.strictEqual(unknownResult.error.operation, "authorize");
      assert.deepEqual(unknownResult.error.issues?.[0]?.path, ["type"]);
      assert.strictEqual(
        unknownResult.error.issues?.[0]?.code,
        "unsupported",
      );
    }
    assert.strictEqual(reads, 0, "an unrelated accessor is not invoked");

    const accessorRequest = Object.defineProperties({}, {
      type: {
        enumerable: true,
        value: "unsigned",
      },
      request: {
        enumerable: true,
        get() {
          reads += 1;
          return {};
        },
      },
    });
    const accessorResult = await client.authorize(
      accessorRequest as never,
    );
    assert.false(accessorResult.ok);
    if (!accessorResult.ok) {
      assert.strictEqual(accessorResult.error.operation, "authorize");
      assert.deepEqual(accessorResult.error.issues?.[0]?.path, [
        "request",
      ]);
      assert.strictEqual(
        accessorResult.error.issues?.[0]?.code,
        "type",
      );
    }
    assert.strictEqual(reads, 0, "the request accessor is not invoked");

    const missingRequest = await client.authorize({
      type: "unsigned",
    } as never);
    assert.false(missingRequest.ok);
    if (!missingRequest.ok) {
      assert.strictEqual(missingRequest.error.operation, "authorize");
      assert.deepEqual(missingRequest.error.issues?.[0]?.path, [
        "request",
      ]);
      assert.strictEqual(
        missingRequest.error.issues?.[0]?.code,
        "required",
      );
    }
    assert.strictEqual(calls, 0);

    const missingMultiIssuerRequest = await client.authorize({
      type: "multiIssuer",
    } as never);
    assert.false(missingMultiIssuerRequest.ok);
    if (!missingMultiIssuerRequest.ok) {
      assert.strictEqual(missingMultiIssuerRequest.error.operation, "authorize");
      assert.deepEqual(missingMultiIssuerRequest.error.issues?.[0]?.path, [
        "request",
      ]);
      assert.strictEqual(
        missingMultiIssuerRequest.error.issues?.[0]?.code,
        "required",
      );
    }
    assert.strictEqual(calls, 0, "no engine call happens for either envelope");
    await client.close();
  });

  QUnit.test("relabels selected-operation errors as authorize", async (assert) => {
    const engine = createTestEngine({
      async authorizeUnsigned() {
        throw createSdkError(
          "AUTHORIZATION_FAILED",
          "authorizeUnsigned",
        );
      },
      async authorizeMultiIssuer() {
        throw new Error("multi-issuer authorization is outside this test");
      },
    });
    const client = createClientForEngine(engine);

    const invalid = await client.authorize({
      type: "unsigned",
      request: {
        action: " ",
        resource: { type: "Example::Resource", id: "document" },
      },
    });

    assert.false(invalid.ok);
    if (!invalid.ok) {
      assert.strictEqual(invalid.error.code, "INVALID_INPUT");
      assert.strictEqual(invalid.error.operation, "authorize");
      assert.deepEqual(invalid.error.issues?.[0]?.path, ["action"]);
    }

    const failed = await client.authorize({
      type: "unsigned",
      request: {
        action: 'Example::Action::"Read"',
        resource: { type: "Example::Resource", id: "document" },
      },
    });
    assert.false(failed.ok);
    if (!failed.ok) {
      assert.strictEqual(failed.error.code, "AUTHORIZATION_FAILED");
      assert.strictEqual(failed.error.operation, "authorize");
    }

    await client.close();
    const closed = await client.authorize({
      type: "unsigned",
      request: {
        action: 'Example::Action::"Read"',
        resource: { type: "Example::Resource", id: "document" },
      },
    });
    assert.false(closed.ok);
    if (!closed.ok) {
      assert.strictEqual(closed.error.code, "CLIENT_CLOSED");
      assert.strictEqual(closed.error.operation, "authorize");
    }

    const closedMultiIssuer = await client.authorize({
      type: "multiIssuer",
      request: {
        tokens: [
          {
            mapping: "Authorization::AccessToken",
            payload: "header.payload.signature",
          },
        ],
        action: 'Authorization::Action::"Read"',
        resource: { type: "Authorization::Resource", id: "document" },
      },
    });
    assert.false(closedMultiIssuer.ok);
    if (!closedMultiIssuer.ok) {
      assert.strictEqual(closedMultiIssuer.error.code, "CLIENT_CLOSED");
      assert.strictEqual(closedMultiIssuer.error.operation, "authorize");
    }
  });

  QUnit.test("closed dispatch rejects before inspecting its envelope", async (assert) => {
    const client = createClientForEngine(
      createTestEngine({
        async authorizeUnsigned() {
          throw new Error("closed work must not reach the engine");
        },
        async authorizeMultiIssuer() {
          throw new Error("closed work must not reach the engine");
        },
      }),
    );
    await client.close();

    let inspections = 0;
    const envelope = new Proxy(
      {
        type: "unsigned",
        request: {
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
        },
      } as const,
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

    const result = await client.authorize(envelope);

    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "CLIENT_CLOSED");
      assert.strictEqual(result.error.operation, "authorize");
    }
    assert.strictEqual(inspections, 0, "the closed client does not inspect input");
  });

  QUnit.test("dispatch admitted while open survives reentrant close", async (assert) => {
    const client = createClientForEngine(
      createTestEngine({
        async authorizeUnsigned() {
          return unsignedDecision;
        },
        async authorizeMultiIssuer() {
          throw new Error(
            "unsigned dispatch must not reach multi-issuer work",
          );
        },
      }),
    );
    let closePromise: ReturnType<typeof client.close> | undefined;
    const envelope = new Proxy(
      {
        type: "unsigned",
        request: {
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
        },
      } as const,
      {
        getPrototypeOf(target) {
          closePromise ??= client.close();
          return Reflect.getPrototypeOf(target);
        },
      },
    );

    const result = await client.authorize(envelope);

    assert.true(result.ok, "work admitted before close is allowed to settle");
    assert.ok(closePromise, "caller behavior started close during inspection");
    if (closePromise === undefined) {
      throw new Error("reentrant close did not start");
    }
    assert.true(
      (await closePromise).ok,
      "close waits for the admitted dispatch",
    );
  });
}
