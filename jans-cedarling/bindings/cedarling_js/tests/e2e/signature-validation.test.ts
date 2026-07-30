import type QUnitApi from "qunit";

import {
  createCedarling,
  type JwtAlgorithm,
} from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";
import {
  createSignedIssuer,
  type SignedIssuerFixture,
} from "../fixtures/signed-issuer.js";

/** Changes one signature character while preserving the JWT structure. */
function tamperSignature(token: string): string {
  const [header, payload, signature] = token.split(".");
  if (
    header === undefined ||
    payload === undefined ||
    signature === undefined ||
    signature.length === 0
  ) {
    throw new Error("The signed-issuer fixture returned an invalid JWT.");
  }
  const replacement = signature[0] === "A" ? "B" : "A";
  return `${header}.${payload}.${replacement}${signature.slice(1)}`;
}

/** Creates one signature-validating client for the local issuer fixture. */
async function createClient(
  issuer: SignedIssuerFixture,
  applicationName: string,
  allowedAlgorithms: readonly JwtAlgorithm[],
) {
  return await createCedarling({
    applicationName,
    authorization: {
      dangerouslyDisableSchemaValidation: true,
    },
    jwt: {
      allowedAlgorithms,
      dangerouslyDisableStatusValidation: true,
    },
    issuerLoading: {
      mode: "sync",
      workers: 1,
    },
    policyStore: {
      type: "inline",
      document: createMultiIssuerPolicyStore(
        issuer.openidConfigurationEndpoint,
      ),
    },
  });
}

/** Wraps one signed access token in the public multi-issuer request shape. */
function request(payload: string) {
  return {
    tokens: [{
      mapping: "Authorization::AccessToken",
      payload,
    }],
    action: 'Authorization::Action::"Read"',
    resource: {
      type: "Authorization::Resource",
      id: "document",
    },
  } as const;
}

/** Registers the real-WASM cryptographic signature-verification contract. */
export default function registerSignatureValidationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("signature-validation");

  QUnit.test("accepts a valid RS256 token and rejects its tampered signature", async (assert) => {
    assert.timeout(15_000);
    const issuer = await createSignedIssuer();

    try {
      const created = await createClient(
        issuer,
        "cedarling-js-signature-validation",
        ["RS256"],
      );

      assert.true(created.ok, "the issuer keys load during initialization");
      if (!created.ok) {
        return;
      }

      try {
        const token = await issuer.signToken();
        const valid = await created.value.authorizeMultiIssuer(
          request(token),
        );

        assert.true(valid.ok, "the valid signature is processed");
        if (valid.ok) {
          assert.true(valid.value.decision, "the valid signature authorizes");
        }

        const tampered = await created.value.authorizeMultiIssuer(
          request(tamperSignature(token)),
        );
        assert.true(
          !tampered.ok || !tampered.value.decision,
          "the tampered signature cannot authorize",
        );
      } finally {
        assert.true((await created.value.shutDown()).ok);
      }
    } finally {
      await issuer.close();
    }
  });

  QUnit.test("rejects signed tokens with expired or untrusted claims", async (assert) => {
    assert.timeout(15_000);
    const issuer = await createSignedIssuer();

    try {
      const created = await createClient(
        issuer,
        "cedarling-js-signed-claims-validation",
        ["RS256"],
      );

      assert.true(created.ok, "the issuer keys load during initialization");
      if (!created.ok) {
        return;
      }

      try {
        const expired = await created.value.authorizeMultiIssuer(
          request(await issuer.signToken({ exp: 1 })),
        );
        assert.false(expired.ok, "a signed expired token is rejected");
        if (!expired.ok) {
          assert.strictEqual(expired.error.code, "AUTHORIZATION_FAILED");
        }

        const untrustedIssuer = await created.value.authorizeMultiIssuer(
          request(await issuer.signToken({
            iss: "https://untrusted.example",
          })),
        );
        assert.false(
          untrustedIssuer.ok,
          "a signed token from an untrusted issuer is rejected",
        );
        if (!untrustedIssuer.ok) {
          assert.strictEqual(
            untrustedIssuer.error.code,
            "AUTHORIZATION_FAILED",
          );
        }
      } finally {
        assert.true((await created.value.shutDown()).ok);
      }
    } finally {
      await issuer.close();
    }
  });

  QUnit.test("rejects a signature algorithm outside the configured allowlist", async (assert) => {
    assert.timeout(15_000);
    const issuer = await createSignedIssuer();

    try {
      const created = await createClient(
        issuer,
        "cedarling-js-signature-algorithm-validation",
        ["ES256"],
      );

      assert.true(created.ok, "the client initializes with an ES256 allowlist");
      if (!created.ok) {
        return;
      }

      try {
        const disallowed = await created.value.authorizeMultiIssuer(
          request(await issuer.signToken()),
        );
        assert.false(
          disallowed.ok,
          "an RS256 token is rejected when only ES256 is allowed",
        );
        if (!disallowed.ok) {
          assert.strictEqual(
            disallowed.error.code,
            "AUTHORIZATION_FAILED",
          );
        }
      } finally {
        assert.true((await created.value.shutDown()).ok);
      }
    } finally {
      await issuer.close();
    }
  });
}
