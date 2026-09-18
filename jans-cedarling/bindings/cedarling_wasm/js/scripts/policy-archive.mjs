import { execFile } from "node:child_process";
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { promisify } from "node:util";

import { parse as parseYaml } from "yaml";

const execute = promisify(execFile);

export async function createPolicyArchive(fixturePath, outputDirectory, name) {
  const fixture = parseYaml(await readFile(fixturePath, "utf8"));
  const [[storeId, store]] = Object.entries(fixture.policy_stores);
  const source = join(outputDirectory, name);
  const archive = join(outputDirectory, `${name}.cjar`);
  const files = {
    "metadata.json": JSON.stringify({
      cedar_version: fixture.cedar_version,
      policy_store: { id: storeId, name: store.name, version: "1.0.0" },
    }),
    "schema.cedarschema": store.schema.body,
  };
  for (const [id, policy] of Object.entries(store.policies)) {
    files[`policies/${id}.cedar`] = policy.policy_content.body;
  }
  for (const [id, issuer] of Object.entries(store.trusted_issuers ?? {})) {
    files[`trusted-issuers/${id}.json`] = JSON.stringify(issuer);
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
