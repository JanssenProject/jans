import type { CedarlingEngine } from "../../dist/engine/engine.js";

export function createTestEngine(
  overrides: Partial<CedarlingEngine> = {},
): CedarlingEngine {
  return {
    async authorizeUnsigned() {
      throw new Error("unsigned authorization is outside this test");
    },
    async authorizeMultiIssuer() {
      throw new Error("multi-issuer authorization is outside this test");
    },
    async shutDown() {},
    ...overrides,
  };
}
