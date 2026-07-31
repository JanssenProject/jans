import { describe, expect, it } from "vitest";

import { checkPermissions } from "./permissions";

const task = { id: "one", title: "One", completed: false, owner: "bob" as const };

describe("checkPermissions", () => {
  it("uses the canonical decision and treats errors as denial", async () => {
    let call = 0;
    const client = {
      authorizeUnsigned: async () =>
        ++call === 1
          ? { ok: true as const, value: { decision: true, requestId: "1", diagnostics: { reasons: [], errors: [] } } }
          : { ok: false as const, error: { code: "AUTHORIZATION_FAILED" } },
    };
    const result = await checkPermissions(client as never, "bob", [task]);
    expect(result.one).toEqual({ canUpdate: true, canDelete: false });
  });
});
