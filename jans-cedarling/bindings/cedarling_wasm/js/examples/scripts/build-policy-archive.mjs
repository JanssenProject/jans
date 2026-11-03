import { mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { createPolicyArchive } from "../../scripts/policy-archive.mjs";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const output = join(root, ".build");
await mkdir(output, { recursive: true });
// The examples run against the published package, which may predate spec v2.
await createPolicyArchive(
  join(root, "fixtures/policy-store.yml"),
  output,
  "policy-store",
  { specVersion: 0 },
);
