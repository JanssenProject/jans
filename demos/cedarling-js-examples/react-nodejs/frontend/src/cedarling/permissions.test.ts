import { describe, expect, it } from "vitest";

import { checkPermissions } from "./permissions";

const task = { id: "one", title: "One", completed: false, owner: "bob" as const };

const allowed = { ok: true as const, value: { decision: true, requestId: "1", diagnostics: { reasons: [], errors: [] } } };
const denied = { ok: true as const, value: { decision: false, requestId: "2", diagnostics: { reasons: ["policy-deny"], errors: [] } } };

describe("checkPermissions", () => {
  it("uses the canonical decision and treats errors as denial", async () => {
    let call = 0;
    const client = {
      authorizeUnsigned: async () =>
        ++call === 1 ? allowed : { ok: false as const, error: { code: "AUTHORIZATION_FAILED" } },
    };
    const result = await checkPermissions(client as never, "bob", [task]);
    expect(result.one).toEqual({ canUpdate: true, canDelete: false });
  });

  it("returns false for both permissions when policy denies", async () => {
    const client = { authorizeUnsigned: async () => denied };
    const result = await checkPermissions(client as never, "alice", [task]);
    expect(result.one).toEqual({ canUpdate: false, canDelete: false });
  });

  it("uses the canonical decision in signed mode", async () => {
    const client = { authorizeMultiIssuer: async () => allowed };
    const result = await checkPermissions(client as never, "bob", [task], "signed-token");
    expect(result.one).toEqual({ canUpdate: true, canDelete: true });
  });
});
