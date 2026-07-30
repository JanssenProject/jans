import type QUnitApi from "qunit";

import { createCedarling } from "@janssenproject/cedarling";
import { createMultiIssuerPolicyStore } from "../fixtures/multi-issuer-policy-store.js";
import { createSignedIssuer } from "../fixtures/signed-issuer.js";

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

/** Registers the real-WASM cryptographic signature-verification contract. */
export default function registerSignatureValidationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("signature-validation");

  QUnit.test("accepts a valid RS256 token and rejects its tampered signature", async (assert) => {
    const issuer = await createSignedIssuer();

    try {
      const created = await createCedarling({
        applicationName: "cedarling-js-signature-validation",
        authorization: {
          dangerouslyDisableSchemaValidation: true,
        },
        jwt: {
          allowedAlgorithms: ["RS256"],
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

      assert.true(created.ok, "the issuer keys load during initialization");
      if (!created.ok) {
        return;
      }

      try {
        const token = await issuer.signToken();
        const request = {
          tokens: [{
            mapping: "Authorization::AccessToken",
            payload: token,
          }],
          action: 'Authorization::Action::"Read"',
          resource: {
            type: "Authorization::Resource",
            id: "document",
          },
        } as const;
        const valid = await created.value.authorizeMultiIssuer(request);

        assert.true(valid.ok, "the valid signature is processed");
        if (valid.ok) {
          assert.true(valid.value.decision, "the valid signature authorizes");
        }

        const tampered = await created.value.authorizeMultiIssuer({
          ...request,
          tokens: [{
            ...request.tokens[0],
            payload: tamperSignature(token),
          }],
        });
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
}
