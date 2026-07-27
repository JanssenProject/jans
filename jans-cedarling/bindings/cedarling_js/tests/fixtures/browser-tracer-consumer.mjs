import { createCedarling } from "@janssenproject/cedarling";

const policyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    tracer: {
      cedar_version: "v4.0.0",
      name: "Tracer",
      policies: {
        allow: {
          description: "allow the public tracer",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Tracer::Action::"Read", resource);',
          },
        },
      },
      schema: {
        encoding: "none",
        content_type: "cedar",
        body: [
          "namespace Tracer {",
          "entity User;",
          "entity Resource;",
          'action "Read" appliesTo {',
          "  principal: [User],",
          "  resource: [Resource],",
          "  context: {}",
          "};",
          "}",
        ].join("\n"),
      },
    },
  },
};

const contextPolicyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    context: {
      cedar_version: "v4.0.0",
      name: "Context",
      policies: {
        feature: {
          description: "allow when retained context enables the feature",
          creation_date: "2026-07-24T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Context::Action::"Use", resource) when { context has data && context.data has feature_enabled && context.data.feature_enabled };',
          },
        },
      },
      schema: {
        encoding: "none",
        content_type: "cedar",
        body: [
          "namespace Context {",
          "entity User;",
          "entity Resource;",
          'action "Use" appliesTo {',
          "  principal: [User],",
          "  resource: [Resource],",
          "  context: { data?: { feature_enabled?: Bool } }",
          "};",
          "}",
        ].join("\n"),
      },
    },
  },
};

function createMultiIssuerPolicyStore(openidConfigurationEndpoint) {
  return {
    cedar_version: "v4.0.0",
    policy_stores: {
      multi_issuer: {
        cedar_version: "v4.0.0",
        name: "Multi issuer",
        trusted_issuers: {
          TestIssuer: {
            name: "TestIssuer",
            description: "Synthetic packed-browser issuer",
            openid_configuration_endpoint: openidConfigurationEndpoint,
            token_metadata: {
              access_token: {
                entity_type_name: "Authorization::AccessToken",
              },
            },
          },
        },
        policies: {
          token_present: {
            description: "allow when the mapped access token is present",
            creation_date: "2026-07-23T00:00:00Z",
            policy_content: {
              encoding: "none",
              content_type: "cedar",
              body:
                'permit(principal, action == Authorization::Action::"Read", resource) when { context has tokens && context.tokens has testissuer_accesstoken };',
            },
          },
        },
      },
    },
  };
}

function base64Url(bytes) {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/u, "");
}

async function signToken(claims) {
  const encoder = new TextEncoder();
  const header = base64Url(
    encoder.encode(JSON.stringify({ alg: "HS256", typ: "JWT" })),
  );
  const payload = base64Url(encoder.encode(JSON.stringify(claims)));
  const input = `${header}.${payload}`;
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode("cedarling-js-browser-signing-key"),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(input),
  );
  return `${input}.${base64Url(new Uint8Array(signature))}`;
}

async function authorize(policyStoreSource, applicationName) {
  const created = await createCedarling({
    applicationName,
    logging: { type: "memory", level: "trace", ttlSeconds: 60 },
    policyStore: policyStoreSource,
  });
  if (!created.ok) {
    throw new Error(created.error.code);
  }

  try {
    const authorized = await created.value.authorizeUnsigned({
      principal: { type: "Tracer::User", id: "alice" },
      action: 'Tracer::Action::"Read"',
      resource: { type: "Tracer::Resource", id: "document" },
    });
    if (!authorized.ok) {
      throw new Error(authorized.error.code);
    }
    const retained = await created.value.logs.find({
      requestId: authorized.value.requestId,
      tag: "decision",
    });
    if (!retained.ok) {
      throw new Error(retained.error.code);
    }
    return {
      decision: authorized.value.decision,
      hasRequestId: authorized.value.requestId.length > 0,
      correlatedDecisionLog:
        retained.value.length > 0 &&
        retained.value.every(
          (entry) =>
            entry.requestId === authorized.value.requestId &&
            entry.kind === "decision",
        ),
      reasons: authorized.value.diagnostics.reasons,
    };
  } finally {
    const closed = await created.value.close();
    if (!closed.ok) {
      throw new Error(closed.error.code);
    }
  }
}

async function authorizeMultiIssuer({ revoked }) {
  const origin = location.origin;
  const created = await createCedarling({
    applicationName: revoked
      ? "packed-browser-revoked"
      : "packed-browser-multi-issuer",
    authorization: {
      dangerouslyDisableSchemaValidation: true,
    },
    jwt: {
      dangerouslyDisableSignatureValidation: true,
      ...(revoked
        ? {}
        : { dangerouslyDisableStatusValidation: true }),
    },
    issuerLoading: { mode: "async", workers: 1 },
    policyStore: {
      type: "inline",
      document: createMultiIssuerPolicyStore(
        `${origin}/.well-known/openid-configuration`,
      ),
    },
  });
  if (!created.ok) {
    throw new Error(created.error.code);
  }

  try {
    const issuerDeadline = Date.now() + 5_000;
    let issuerReady = await created.value.issuers.isLoaded({
      id: "TestIssuer",
    });
    while (
      issuerReady.ok &&
      !issuerReady.value &&
      Date.now() < issuerDeadline
    ) {
      await new Promise((resolve) => setTimeout(resolve, 25));
      issuerReady = await created.value.issuers.isLoaded({
        id: "TestIssuer",
      });
    }
    if (!issuerReady.ok || !issuerReady.value) {
      throw new Error(
        issuerReady.ok ? "ISSUER_NOT_READY" : issuerReady.error.code,
      );
    }
    const token = await signToken({
      iss: origin,
      sub: "alice",
      jti: revoked ? "revoked-token" : "valid-token",
      iat: 1_700_000_000,
      exp: 4_000_000_000,
      ...(revoked
        ? {
            status: {
              status_list: {
                idx: 0,
                uri: `${origin}/status-list`,
              },
            },
          }
        : {}),
    });
    const authorized = await created.value.authorizeMultiIssuer({
      tokens: [
        {
          mapping: "Authorization::AccessToken",
          payload: token,
        },
      ],
      action: 'Authorization::Action::"Read"',
      resource: {
        type: "Authorization::Resource",
        id: "document",
      },
    });

    if (revoked) {
      return authorized.ok
        ? { ok: true, decision: authorized.value.decision }
        : {
            ok: false,
            code: authorized.error.code,
            operation: authorized.error.operation,
          };
    }
    if (!authorized.ok) {
      throw new Error(authorized.error.code);
    }
    return {
      decision: authorized.value.decision,
      hasRequestId: authorized.value.requestId.length > 0,
      issuerReady: issuerReady.value,
      reasons: authorized.value.diagnostics.reasons,
    };
  } finally {
    const closed = await created.value.close();
    if (!closed.ok) {
      throw new Error(closed.error.code);
    }
  }
}

async function authorizeWithContext() {
  const created = await createCedarling({
    applicationName: "packed-browser-context",
    policyStore: { type: "inline", document: contextPolicyStore },
  });
  if (!created.ok) {
    throw new Error(created.error.code);
  }
  const request = {
    principal: { type: "Context::User", id: "alice" },
    action: 'Context::Action::"Use"',
    resource: { type: "Context::Resource", id: "feature" },
  };

  try {
    const before = await created.value.authorizeUnsigned(request);
    if (!before.ok) {
      throw new Error(before.error.code);
    }
    const stored = await created.value.context.set(
      "feature_enabled",
      true,
    );
    if (!stored.ok) {
      throw new Error(stored.error.code);
    }
    const after = await created.value.authorizeUnsigned(request);
    if (!after.ok) {
      throw new Error(after.error.code);
    }
    return {
      before: before.value.decision,
      after: after.value.decision,
    };
  } finally {
    const closed = await created.value.close();
    if (!closed.ok) {
      throw new Error(closed.error.code);
    }
  }
}

try {
  const result = {
    inline: await authorize(
      { type: "inline", document: policyStore },
      "packed-browser-inline",
    ),
    url: await authorize(
      { type: "url", url: new URL("/policy", location.origin) },
      "packed-browser-url",
    ),
    multiIssuer: await authorizeMultiIssuer({ revoked: false }),
    revoked: await authorizeMultiIssuer({ revoked: true }),
    context: await authorizeWithContext(),
  };
  await fetch("/result", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(result),
  });
  document.body.textContent = "passed";
} catch (error) {
  await fetch("/result", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      error: error instanceof Error ? error.message : "unknown",
    }),
  });
  document.body.textContent = "failed";
}
