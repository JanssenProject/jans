import { readFile } from "node:fs/promises";

import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";
import { createConsoleReporter } from "../shared/presentation.mjs";
import { runCedarling } from "../shared/run-cedarling.mjs";

try {
  const archive = await readFile(
    new URL("../.build/policy-store.cjar", import.meta.url),
  );
  await runCedarling(initFromArchiveBytes, archive, createConsoleReporter());
} catch (error) {
  console.error("Cedarling example failed", error);
  process.exitCode = 1;
}
