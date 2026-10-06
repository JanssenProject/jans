const { readFile } = require("node:fs/promises");
const { join } = require("node:path");

const { initFromArchiveBytes } = require("@janssenproject/cedarling_wasm");

async function main() {
  const { createConsoleReporter } = await import("../shared/presentation.mjs");
  const { runCedarling } = await import("../shared/run-cedarling.mjs");
  const archive = await readFile(
    join(__dirname, "../.build/policy-store.cjar"),
  );
  await runCedarling(initFromArchiveBytes, archive, createConsoleReporter());
}

main().catch((error) => {
  console.error("Cedarling example failed", error);
  process.exitCode = 1;
});
