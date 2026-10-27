export function formatValue(value) {
  if (value === undefined) return "undefined";
  return JSON.stringify(
    value,
    (_, item) => (typeof item === "bigint" ? item.toString() : item),
    2,
  );
}

export function createConsoleReporter() {
  let section;
  console.log(
    "Raw results; offline test tokens intentionally produce warnings.",
  );
  console.log(
    "Authorization fields: https://docs.jans.io/stable/cedarling/reference/cedarling-authz/",
  );
  console.log(
    "Log fields: https://docs.jans.io/stable/cedarling/reference/cedarling-logs/",
  );
  return (record) => {
    if (record.section !== section) {
      section = record.section;
      console.log(`\n=== ${section} ===`);
    }
    console.log(`\n${record.label}`);
    console.log(formatValue(record.value));
  };
}

export function logBrowserResult(record) {
  console.groupCollapsed(`${record.section}: ${record.label}`);
  console.log(record.value);
  console.groupEnd();
}

export function decisionState(value) {
  if (value?.decision === true) return "allow";
  if (value?.decision === false) return "deny";
  return undefined;
}

export function createBrowserReporter(container) {
  const sections = new Map();
  if (!document.querySelector("[data-cedarling-example-styles]")) {
    const style = document.createElement("style");
    style.dataset.cedarlingExampleStyles = "";
    style.textContent = resultStyles;
    document.head.append(style);
  }

  return (record) => {
    logBrowserResult(record);
    let section = sections.get(record.section);
    if (!section) {
      section = document.createElement("section");
      const heading = document.createElement("h2");
      heading.textContent = record.section;
      section.append(heading);
      sections.set(record.section, section);
      container.append(section);
    }
    const result = document.createElement("details");
    result.open = record.section !== "Logs";
    result.dataset.result = "";
    result.dataset.section = record.section;
    result.dataset.label = record.label;
    const decision = decisionState(record.value);
    if (decision) result.dataset.decision = decision;
    const heading = document.createElement("summary");
    heading.textContent = decision
      ? `${record.label} — ${decision.toUpperCase()}`
      : record.label;
    const output = document.createElement("pre");
    output.textContent = formatValue(record.value);
    result.append(heading, output);
    section.append(result);
  };
}

export const resultStyles = `
  :root { font-family: system-ui, sans-serif; } body { margin: 0; background: #f4f7f6; color: #17211d; } main { width: min(960px, calc(100% - 2rem)); margin: 2rem auto; } header { margin-bottom: 1.5rem; } button { padding: .7rem 1rem; border: 0; border-radius: .4rem; background: #176b52; color: white; font-weight: 700; cursor: pointer; } button:disabled { cursor: wait; opacity: .65; } section { margin: 1.5rem 0; } details { margin: .75rem 0; padding: 1rem; border: 1px solid #cbd8d2; border-radius: .5rem; background: white; } h1, h2, summary { line-height: 1.25; } summary { font-size: 1rem; font-weight: 700; cursor: pointer; } pre { margin-bottom: 0; padding: .8rem; overflow: auto; border-radius: .35rem; background: #10251e; color: #e5fff5; font-size: .82rem; } [data-decision="allow"] { border-left: .35rem solid #198754; } [data-decision="deny"] { border-left: .35rem solid #b42318; } [data-state="error"] { color: #b42318; }
`;
