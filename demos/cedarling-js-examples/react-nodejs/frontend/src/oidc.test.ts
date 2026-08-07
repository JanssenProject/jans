import { describe, expect, it } from "vitest";

import { createPkce, validatedOrigin } from "./oidc";

describe("OIDC browser security helpers", () => {
  it("creates an S256 verifier and challenge", async () => {
    const value = await createPkce();
    expect(value.verifier.length).toBeGreaterThanOrEqual(43);
    expect(value.challenge).toMatch(/^[A-Za-z0-9_-]+$/);
    expect(value.challenge).not.toBe(value.verifier);
  });

  it("permits loopback HTTP but rejects remote HTTP", () => {
    expect(validatedOrigin("http://localhost:9090")).toBe("http://localhost:9090");
    expect(() => validatedOrigin("http://idp.example")).toThrow(/HTTPS origin/);
  });
});
