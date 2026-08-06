import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import { createTestEngine } from "./engine-fixture.js";

const decision = {
  decision: true,
  requestId: "unsigned-unit",
  diagnostics: { reasons: ["allow"], errors: [] },
};

/** Registers deterministic unsigned request-boundary tests. */
export default function registerUnsignedAuthorizationUnitTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("authorize-unsigned");

  /** Creates a client whose engine records accepted authorization work. */
  function createRecordingClient() {
    let calls = 0;
    const engine = createTestEngine({
      async authorizeUnsigned() {
        calls += 1;
        return decision;
      },
    });
    return {
      client: createClientForEngine(engine),
      calls: () => calls,
    };
  }

  QUnit.test("rejects malformed request structure before the engine", async (assert) => {
    const recording = createRecordingClient();
    const cases: readonly {
      readonly name: string;
      readonly value: unknown;
      readonly path: readonly (string | number)[];
      readonly code: string;
    }[] = [
      {
        name: "non-object request",
        value: null,
        path: [],
        code: "type",
      },
      {
        name: "missing action",
        value: { resource: { type: "Example::Resource", id: "document" } },
        path: ["action"],
        code: "required",
      },
      {
        name: "empty action",
        value: {
          action: " ",
          resource: { type: "Example::Resource", id: "document" },
        },
        path: ["action"],
        code: "required",
      },
      {
        name: "missing resource",
        value: { action: 'Example::Action::"Read"' },
        path: ["resource"],
        code: "type",
      },
      {
        name: "empty resource identifier",
        value: {
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "" },
        },
        path: ["resource", "id"],
        code: "required",
      },
    ];

    for (const testCase of cases) {
      const result = await recording.client.authorizeUnsigned(
        testCase.value as never,
      );
      assert.false(result.ok, testCase.name);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
        assert.deepEqual(result.error.issues?.[0]?.path, testCase.path);
        assert.strictEqual(result.error.issues?.[0]?.code, testCase.code);
      }
    }

    assert.strictEqual(recording.calls(), 0, "no invalid request reaches the engine");
    await recording.client.shutDown();
  });

  QUnit.test("rejects unknown fields without reading accessors", async (assert) => {
    const recording = createRecordingClient();
    const baseRequest = {
      principal: { type: "Example::User", id: "alice" },
      action: 'Example::Action::"Read"',
      resource: { type: "Example::Resource", id: "document" },
    };
    const cases = [
      {
        name: "top-level field",
        value: { ...baseRequest, ressource: baseRequest.resource },
        path: ["ressource"],
      },
      {
        name: "principal field",
        value: {
          ...baseRequest,
          principal: { ...baseRequest.principal, role: "admin" },
        },
        path: ["principal", "role"],
      },
      {
        name: "resource field",
        value: {
          ...baseRequest,
          resource: { ...baseRequest.resource, owner: "alice" },
        },
        path: ["resource", "owner"],
      },
    ] as const;

    for (const testCase of cases) {
      const result = await recording.client.authorizeUnsigned(
        testCase.value as never,
      );
      assert.false(result.ok, testCase.name);
      if (!result.ok) {
        assert.deepEqual(result.error.issues?.[0], {
          path: testCase.path,
          code: "unknownField",
          message: "The field is not supported.",
        });
      }
    }

    let accessorReads = 0;
    const requestWithAccessor = { ...baseRequest } as Record<string, unknown>;
    Object.defineProperty(requestWithAccessor, "ressource", {
      enumerable: true,
      get() {
        accessorReads += 1;
        return baseRequest.resource;
      },
    });
    const accessorResult = await recording.client.authorizeUnsigned(
      requestWithAccessor as never,
    );
    assert.false(accessorResult.ok);
    if (!accessorResult.ok) {
      assert.deepEqual(accessorResult.error.issues?.[0]?.path, ["ressource"]);
      assert.strictEqual(
        accessorResult.error.issues?.[0]?.code,
        "unknownField",
      );
    }
    assert.strictEqual(accessorReads, 0, "unknown accessors are not executed");
    assert.strictEqual(recording.calls(), 0, "no invalid request reaches the engine");
    await recording.client.shutDown();
  });

  QUnit.test("rejects unsafe Cedar values before the engine", async (assert) => {
    const recording = createRecordingClient();
    const cases: readonly {
      readonly name: string;
      readonly value: unknown;
      readonly path: readonly (string | number)[];
    }[] = [
      {
        name: "reserved generated mapping",
        value: {
          principal: {
            type: "Example::User",
            id: "alice",
            attributes: {
              cedar_entity_mapping: "attacker-controlled",
            },
          },
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
        },
        path: ["principal", "attributes", "cedar_entity_mapping"],
      },
      {
        name: "unsafe entity integer",
        value: {
          principal: {
            type: "Example::User",
            id: "alice",
            attributes: {
              count: Number.MAX_SAFE_INTEGER + 1,
            },
          },
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
        },
        path: ["principal", "attributes"],
      },
      {
        name: "implicit fractional entity number",
        value: {
          principal: {
            type: "Example::User",
            id: "alice",
            attributes: {
              score: 1.25,
            },
          },
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
        },
        path: ["principal", "attributes"],
      },
      {
        name: "raw fractional context number",
        value: {
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
          context: { score: 1.25 },
        },
        path: ["context"],
      },
      {
        name: "malformed extension marker",
        value: {
          action: 'Example::Action::"Read"',
          resource: { type: "Example::Resource", id: "document" },
          context: {
            network: {
              __extn: {
                fn: "ip",
                arg: "",
              },
            },
          },
        },
        path: ["context"],
      },
    ];

    for (const testCase of cases) {
      const result = await recording.client.authorizeUnsigned(
        testCase.value as never,
      );
      assert.false(result.ok, testCase.name);
      if (!result.ok) {
        assert.strictEqual(result.error.code, "INVALID_INPUT");
        assert.deepEqual(result.error.issues?.[0]?.path, testCase.path);
      }
    }

    assert.strictEqual(recording.calls(), 0, "no invalid value reaches the engine");
    await recording.client.shutDown();
  });
}
