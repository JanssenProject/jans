import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const directory = path.dirname(fileURLToPath(import.meta.url));
const POLICY_NAMES = [
  "view-token",
  "create-token",
  "modify-token",
  "view-user",
  "create-user",
  "modify-user",
];

function readText(relativePath) {
  return readFileSync(path.join(directory, relativePath), "utf8").trim();
}

function policy(name) {
  return {
    description: `Task example policy: ${name}`,
    creation_date: "2026-07-24T00:00:00Z",
    policy_content: {
      encoding: "none",
      content_type: "cedar",
      body: readText(path.join("policies", `${name}.cedar`)),
    },
  };
}

// The trusted issuer URL must match the server's effective origin; generating
// the document at startup keeps token validation correct on any loopback port.
export function createPolicyStore(issuer) {
  return {
    cedar_version: "v4.0.0",
    policy_stores: {
      TaskApp: {
        cedar_version: "v4.0.0",
        name: "TaskApp",
        policies: Object.fromEntries(
          POLICY_NAMES.map((name) => [name, policy(name)]),
        ),
        schema: {
          encoding: "none",
          content_type: "cedar",
          body: readText("schema.cedarschema"),
        },
        trusted_issuers: {
          LocalMockIdP: {
            name: "LocalMockIdP",
            description: "Local mock OIDC provider",
            openid_configuration_endpoint: new URL(
              "/.well-known/openid-configuration",
              issuer,
            ).href,
            token_metadata: {
              userinfo_token: {
                entity_type_name: "LocalMockIdP::Userinfo_token",
              },
            },
          },
        },
      },
    },
  };
}
