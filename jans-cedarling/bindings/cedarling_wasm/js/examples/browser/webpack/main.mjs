import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";
import { createBrowserReporter } from "../../shared/presentation.mjs";
import { fetchArchive, runCedarling } from "../../shared/run-cedarling.mjs";

const archiveUrl = new URL("../../.build/policy-store.cjar", import.meta.url);
const button = document.getElementById("authorize");
const output = document.getElementById("result");
const results = document.getElementById("results");
let report = createBrowserReporter(results);

button.addEventListener("click", async () => {
  button.disabled = true;
  results.replaceChildren();
  report = createBrowserReporter(results);
  output.textContent = "Running Cedarling services…";
  output.dataset.state = "running";
  try {
    await runCedarling(
      initFromArchiveBytes,
      await fetchArchive(archiveUrl),
      report,
    );
    output.textContent = "All services completed";
    output.dataset.state = "success";
  } catch (error) {
    console.error("Cedarling example failed", error);
    output.textContent = "Cedarling example failed";
    output.dataset.state = "error";
  } finally {
    button.disabled = false;
  }
});
