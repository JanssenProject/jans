import assert from "node:assert/strict";
import { execFile, spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { createServer } from "node:http";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

import { chromium } from "playwright";
import { preview } from "vite";

import { runCedarling } from "../shared/run-cedarling.mjs";

const execute = promisify(execFile);
const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const npm = process.platform === "win32" ? "npm.cmd" : "npm";

function report(error) {
  process.stderr.write(error.stderr ?? "");
  process.stdout.write(error.stdout ?? "");
  throw error;
}

async function run(command, arguments_) {
  return execute(command, arguments_, { cwd: root, timeout: 120_000 }).catch(
    report,
  );
}

const runScript = (name) => run(npm, ["run", "--silent", name]);

function valueOf(records, section, label) {
  const record = records.find(
    (item) => item.section === section && item.label === label,
  );
  assert.ok(record, `${section}: ${label} must be reported`);
  return record.value;
}

function assertRecords(records, label) {
  assert.equal(records.length, 34, `${label} must report every service result`);
  const get = (section, item) => valueOf(records, section, item);
  const decisionCases = [
    ["Unsigned authorization", "Allowed document", true],
    ["Unsigned authorization", "Denied document", false],
    ["Multi-issuer authorization", "Matching issuers", true],
    ["Multi-issuer authorization", "Acme token only", false],
    ["Multi-issuer authorization", "Dolphin token only", false],
    ["Batch authorization", "Unsigned item 1", true],
    ["Batch authorization", "Unsigned item 2", false],
    ["Batch authorization", "Multi-issuer item 1", true],
    ["Batch authorization", "Multi-issuer item 2", false],
  ];
  for (const [section, item, decision] of decisionCases) {
    const result = get(section, item);
    assert.equal(result.decision, decision, `${label}: ${item} decision`);
    const errors = result.response.diagnostics.errors;
    assert.equal(errors.length, 0, `${label}: ${item} errors`);
  }
  const unsignedAllow = get("Unsigned authorization", "Allowed document");
  assert.ok(unsignedAllow.request_id, `${label} needs a request ID`);
  const description = "Allows Alice to read document-1";
  const annotations = { "allow-alice": { description } };
  const exactCases = [
    ["Initialization", "Cedarling initialized", true],
    ["Policy annotations", "Merged annotations", { description }],
    ["Policy annotations", "Description values", [description]],
    ["Policy annotations", "Annotations by policy", annotations],
    ["Context data", "Stored value", { plan: "pro" }],
    ["Context data", "Entry removed", true],
    ["Context data", "Entries after clear", []],
    ["Trusted issuers", "Total issuers", 2],
    ["Trusted issuers", "Loaded issuer count", 2],
    ["Trusted issuers", "Failed issuer IDs", []],
    ["Trusted issuers", "Acme loaded by name", true],
    ["Trusted issuers", "Acme loaded by issuer URL", true],
    ["Shutdown", "Cedarling stopped", true],
  ];
  for (const [section, item, expected] of exactCases) {
    assert.deepEqual(get(section, item), expected, `${label}: ${item}`);
  }
  for (const kind of ["Unsigned", "Multi-issuer"]) {
    const batchId = get("Batch authorization", `${kind} batch ID`);
    assert.equal(typeof batchId, "string", `${label}: ${kind} batch ID type`);
    assert.ok(batchId, `${label}: ${kind} batch ID`);
  }
  const storedEntry = get("Context data", "Stored entry");
  assert.equal(storedEntry.key, "account:alice");
  assert.deepEqual(storedEntry.value, { plan: "pro" });
  assert.equal(storedEntry.data_type, "record");
  const entries = get("Context data", "All entries");
  const entryKeys = entries.map((entry) => entry.key).sort();
  assert.deepEqual(entryKeys, ["account:alice", "request:temporary"]);
  assert.equal(get("Context data", "Store statistics").entry_count, 2);
  const issuerIds = get("Trusted issuers", "Loaded issuer IDs").sort();
  assert.deepEqual(issuerIds, ["Acme", "Dolphin"]);
  const logIds = get("Logs", "Log IDs");
  assert.ok(logIds.length, `${label}: log IDs`);
  const firstLog = get("Logs", "First log by ID");
  assert.equal(firstLog.id, logIds[0], `${label}: first log lookup`);
  const logLabels = [
    "Decision logs",
    "Logs for allowed request",
    "Decision logs for allowed request",
    "Drained logs",
  ];
  assert.ok(logLabels.every((item) => get("Logs", item).length));
  const logs = get("Logs", "Drained logs");
  const warnings = logs.filter((entry) => entry.level === "WARN");
  assert.equal(warnings.length, 2, `${label}: unexpected warnings`);
  assert.ok(
    warnings.every((entry) => entry.msg.includes("validation is disabled")),
    `${label}: unexpected warning source`,
  );
  assert.ok(
    logs.every((entry) => entry.error_msg === undefined),
    `${label} must not log policy errors`,
  );
}

async function assertLifecycleCleanup() {
  const fail = (message) => Promise.reject(new Error(message));
  await assert.rejects(
    runCedarling(() => fail("initialization failed"), new Uint8Array()),
    /initialization failed/,
  );

  const cleanup = [];
  const client = {
    shutDown: async () => cleanup.push("shutdown"),
    free: () => cleanup.push("free"),
  };
  await assert.rejects(
    runCedarling(
      async () => client,
      new Uint8Array(),
      () => {
        throw new Error("reporting failed");
      },
    ),
    /reporting failed/,
  );
  assert.deepEqual(cleanup, ["shutdown", "free"]);
}

async function checkBrowser(browser, directory, label) {
  const fixture = await preview({
    build: { outDir: directory },
    preview: { host: "127.0.0.1", port: 0 },
  });
  const page = await browser.newPage();
  const diagnostics = [];
  page.on("pageerror", (error) => diagnostics.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") diagnostics.push(message.text());
  });
  try {
    const navigation = await page.goto(fixture.resolvedUrls.local[0]);
    assert.equal(navigation?.status(), 200, `${label} page must load`);
    try {
      await page.locator("#authorize").click();
      await page.locator('#result[data-state="success"]').waitFor();
    } catch (error) {
      throw new Error(
        `${label} failed: ${diagnostics.join("\n") || error.message}`,
        { cause: error },
      );
    }
    const records = await page.locator("[data-result]").evaluateAll((results) =>
      results.map((result) => ({
        section: result.dataset.section,
        label: result.dataset.label,
        value: JSON.parse(result.querySelector("pre").textContent),
      })),
    );
    assertRecords(records, label);
    assert.deepEqual(diagnostics, [], `${label} produced browser errors`);
  } finally {
    await page.close();
    await fixture.close();
  }
}

function reservePort() {
  return new Promise((resolvePort, reject) => {
    const server = createServer();
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      const { port } = server.address();
      server.close((error) => (error ? reject(error) : resolvePort(port)));
    });
  });
}

async function checkWorker() {
  const port = await reservePort();
  const output = [];
  const worker = spawn(
    process.execPath,
    [
      resolve(root, "node_modules/wrangler/bin/wrangler.js"),
      "dev",
      "--config",
      "edge/cloudflare/wrangler.jsonc",
      "--ip",
      "127.0.0.1",
      "--port",
      String(port),
      "--local",
    ],
    { cwd: root, stdio: ["ignore", "pipe", "pipe"] },
  );
  worker.stdout.on("data", (chunk) => output.push(chunk));
  worker.stderr.on("data", (chunk) => output.push(chunk));
  try {
    const deadline = Date.now() + 30_000;
    let response;
    while (Date.now() < deadline) {
      if (worker.exitCode !== null) break;
      try {
        response = await fetch(`http://127.0.0.1:${port}/authorize`, {
          signal: AbortSignal.timeout(1_000),
        });
        break;
      } catch {
        await new Promise((resolveWait) => setTimeout(resolveWait, 200));
      }
    }
    if (!response) {
      throw new Error(
        `Cloudflare Worker did not start:\n${Buffer.concat(output).toString().slice(-4_000)}`,
      );
    }
    assert.equal(response.status, 200, "Cloudflare Worker must authorize");
    assertRecords(await response.json(), "Cloudflare Worker");
  } finally {
    worker.kill("SIGTERM");
    await new Promise((resolveExit) => {
      if (worker.exitCode !== null) resolveExit();
      else worker.once("exit", resolveExit);
    });
  }
}

await assertLifecycleCleanup();
await runScript("build:policy-archive");
const archive = await readFile(resolve(root, ".build/policy-store.cjar"));
const esmPackage = await import("@janssenproject/cedarling_wasm");
const require = createRequire(import.meta.url);
const commonJsPackage = require("@janssenproject/cedarling_wasm");
assertRecords(
  await runCedarling(esmPackage.initFromArchiveBytes, archive),
  "Node ESM",
);
assertRecords(
  await runCedarling(commonJsPackage.initFromArchiveBytes, archive),
  "Node CommonJS",
);
await run(process.execPath, ["node/esm.mjs"]);
await run(process.execPath, ["node/commonjs.cjs"]);
await runScript("build:vite");
await runScript("build:webpack");

const browser = await chromium.launch({ headless: true });
try {
  await checkBrowser(browser, resolve(root, ".build/vite"), "React/Vite");
  await checkBrowser(browser, resolve(root, ".build/webpack"), "webpack");
} finally {
  await browser.close();
}
await checkWorker();
console.log("Cedarling examples passed");
