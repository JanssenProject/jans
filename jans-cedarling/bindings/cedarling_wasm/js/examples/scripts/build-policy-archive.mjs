import { mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { createPolicyArchive } from "../../scripts/policy-archive.mjs";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const output = join(root, ".build");
await mkdir(output, { recursive: true });
await createPolicyArchive(
  join(root, "fixtures/policy-store.yml"),
  output,
  "policy-store",
);
