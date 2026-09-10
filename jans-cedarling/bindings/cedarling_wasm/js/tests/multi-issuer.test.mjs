/** Test the multi-issuer authorization logic with various token scenarios */
import {
  multiIssuerConfig,
  multiIssuerRequest,
  testTokens,
} from "./fixtures/multi-issuer.mjs";

async function assertDecision(cedarling, tokens, orgId, expected) {
  const result = await cedarling.authorizeMultiIssuer(
    multiIssuerRequest(tokens, orgId),
  );
  try {
    if (
      JSON.parse(result.jsonString()).response.diagnostics.errors.length !== 0
    ) {
      throw new Error("Multi-issuer evaluation produced policy errors");
    }
    if (result.decision !== expected) {
      throw new Error(`Multi-issuer decision for ${orgId} must be ${expected}`);
    }
    if (
      typeof result.request_id !== "string" ||
      result.request_id.length === 0
    ) {
      throw new Error("Multi-issuer result omitted its request_id");
    }
  } finally {
    result.free();
  }
}

// Shared cases are registered by the installed Node and browser test runners.
export const multiIssuerCases = {
  "matching JWTs allow": (cedarling, tokens) =>
    assertDecision(cedarling, tokens, "example", true),
  "mismatched resource denies": (cedarling, tokens) =>
    assertDecision(cedarling, tokens, "different", false),
  "empty tokens reject": async (cedarling) => {
    let rejection;
    try {
      const result = await cedarling.authorizeMultiIssuer(
        multiIssuerRequest([]),
      );
      result.free();
    } catch (error) {
      rejection = error;
    }
    if (
      rejection instanceof TypeError ||
      !String(rejection).includes("Empty token array")
    ) {
      throw new Error(
        `Expected empty-token validation rejection, received: ${rejection}`,
      );
    }
  },
};

export async function runMultiIssuerTest(initFromArchiveBytes, name, archive) {
  const scenario = multiIssuerCases[name];
  if (!Object.hasOwn(multiIssuerCases, name))
    throw new Error(`Unknown multi-issuer test: ${name}`);
  const tokens = await testTokens();
  const cedarling = await initFromArchiveBytes(multiIssuerConfig, archive);
  try {
    if (typeof cedarling.authorizeMultiIssuer !== "function") {
      throw new Error("Installed client omitted authorizeMultiIssuer");
    }
    await scenario(cedarling, tokens);
  } finally {
    try {
      await cedarling.shutDown();
    } finally {
      cedarling.free();
    }
  }
}
