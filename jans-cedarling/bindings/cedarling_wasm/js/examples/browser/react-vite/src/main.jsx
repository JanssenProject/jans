import { useState } from "react";
import { createRoot } from "react-dom/client";

import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";
import {
  decisionState,
  formatValue,
  logBrowserResult,
  resultStyles,
} from "../../../shared/presentation.mjs";
import { fetchArchive, runCedarling } from "../../../shared/run-cedarling.mjs";

const archiveUrl = new URL(
  "../../../.build/policy-store.cjar",
  import.meta.url,
);

function Results({ records }) {
  const sections = records.reduce((grouped, record) => {
    const items = grouped.get(record.section) ?? [];
    items.push(record);
    grouped.set(record.section, items);
    return grouped;
  }, new Map());
  return [...sections].map(([section, items]) => (
    <section key={section}>
      <h2>{section}</h2>
      {items.map((record) => (
        <details
          open={section !== "Logs"}
          data-result=""
          data-section={section}
          data-label={record.label}
          data-decision={decisionState(record.value)}
          key={record.label}
        >
          <summary>
            {record.label}
            {decisionState(record.value) &&
              ` — ${decisionState(record.value).toUpperCase()}`}
          </summary>
          <pre>{formatValue(record.value)}</pre>
        </details>
      ))}
    </section>
  ));
}

function App() {
  const [records, setRecords] = useState([]);
  const [state, setState] = useState({ status: "idle", message: "Ready" });

  async function run() {
    setRecords([]);
    setState({ status: "running", message: "Running Cedarling services…" });
    try {
      await runCedarling(
        initFromArchiveBytes,
        await fetchArchive(archiveUrl),
        (record) => {
          logBrowserResult(record);
          setRecords((current) => [...current, record]);
        },
      );
      setState({ status: "success", message: "All services completed" });
    } catch (error) {
      console.error("Cedarling example failed", error);
      setState({ status: "error", message: "Cedarling example failed" });
    }
  }

  return (
    <main>
      <style>{resultStyles}</style>
      <header>
        <h1>Cedarling services</h1>
        <p>
          Run authorization and inspect raw Cedarling results. Validation
          warnings are expected only for these offline test tokens.
        </p>
        <p>
          <a href="https://docs.jans.io/stable/cedarling/reference/cedarling-authz/">
            Authorization fields
          </a>{" "}
          ·{" "}
          <a href="https://docs.jans.io/stable/cedarling/reference/cedarling-logs/">
            Log fields
          </a>
        </p>
        <button
          id="authorize"
          type="button"
          disabled={state.status === "running"}
          onClick={run}
        >
          Run Cedarling example
        </button>
        <p id="result" aria-live="polite" data-state={state.status}>
          {state.message}
        </p>
      </header>
      <Results records={records} />
    </main>
  );
}

createRoot(document.getElementById("root")).render(<App />);
