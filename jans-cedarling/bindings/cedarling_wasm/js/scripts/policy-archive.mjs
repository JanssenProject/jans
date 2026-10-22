import { execFile } from "node:child_process";
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { promisify } from "node:util";

import { parse as parseYaml } from "yaml";

const execute = promisify(execFile);

// The Agama issuers of a YAML fixture as a v2 `trusted-issuers.json`: one
// entry per trusted token type, keyed by its entity type, which must be unique.
function trustedIssuersJson(issuers) {
  const entries = {};
  for (const issuer of Object.values(issuers)) {
    const configuration =
      issuer.openid_configuration_endpoint ?? issuer.configuration_endpoint;
    for (const token of Object.values(issuer.token_metadata ?? {})) {
      if (token.trusted === false) continue;
      const existing = entries[token.entity_type_name];
      if (existing) {
        throw new Error(
          `entity type '${token.entity_type_name}' is declared by issuers ` +
            `'${existing.issuer}' and '${issuer.name}'`,
        );
      }
      entries[token.entity_type_name] = {
        type: "jwt",
        issuer: issuer.name,
        configuration_endpoints: { configuration },
        ...(token.token_id && { token_id: token.token_id }),
        ...(token.required_claims && {
          required_claims: token.required_claims,
        }),
      };
    }
  }
  return entries;
}

// `specVersion` 0 writes the unversioned layout, for Cedarling releases that
// predate spec v2; 2 writes a `trusted-issuers.json`.
export async function createPolicyArchive(
  fixturePath,
  outputDirectory,
  name,
  { specVersion = 2 } = {},
) {
  if (specVersion !== 0 && specVersion !== 2) {
    throw new Error(`unsupported policy store spec version ${specVersion}`);
  }
  const fixture = parseYaml(await readFile(fixturePath, "utf8"));
  const [[storeId, store]] = Object.entries(fixture.policy_stores);
  const source = join(outputDirectory, name);
  const archive = join(outputDirectory, `${name}.cjar`);
  const files = {
    "metadata.json": JSON.stringify({
      ...(specVersion === 2 && { policy_store_spec_version: 2 }),
      cedar_version: fixture.cedar_version,
      policy_store: { id: storeId, name: store.name, version: "1.0.0" },
    }),
    "schema.cedarschema": store.schema.body,
  };
  for (const [id, policy] of Object.entries(store.policies)) {
    files[`policies/${id}.cedar`] = policy.policy_content.body;
  }
  // Only JWT issuers are converted; fail rather than drop custom ones.
  if (Object.keys(store.custom_issuers ?? {}).length > 0) {
    throw new Error(`${fixturePath}: custom_issuers are not supported`);
  }
  if (specVersion === 0) {
    for (const [id, issuer] of Object.entries(store.trusted_issuers ?? {})) {
      files[`trusted-issuers/${id}.json`] = JSON.stringify(issuer);
    }
  } else {
    const trustedIssuers = trustedIssuersJson(store.trusted_issuers ?? {});
    if (Object.keys(trustedIssuers).length > 0) {
      files["trusted-issuers.json"] = JSON.stringify(trustedIssuers);
    }
  }

  await rm(source, { force: true, recursive: true });
  await rm(archive, { force: true });
  for (const [file, contents] of Object.entries(files)) {
    const path = join(source, file);
    await mkdir(dirname(path), { recursive: true });
    await writeFile(path, contents);
  }
  await execute("zip", ["-q", "-X", archive, ...Object.keys(files)], {
    cwd: source,
  });
  return archive;
}
