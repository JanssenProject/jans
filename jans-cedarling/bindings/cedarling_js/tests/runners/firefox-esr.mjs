#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { createServer } from "node:http";
import { resolve } from "node:path";
import { spawn } from "node:child_process";

const delay = (milliseconds) =>
  new Promise((resolve) => setTimeout(resolve, milliseconds));
const driverOrigin = "http://127.0.0.1:4444";
const firefoxBinary = process.env.CEDARLING_FIREFOX_ESR_BINARY;
if (firefoxBinary === undefined || firefoxBinary.length === 0) {
  throw new Error("CEDARLING_FIREFOX_ESR_BINARY must name the Firefox ESR executable");
}
let driverLog = "";

async function webdriver(path, method = "GET", body) {
  const response = await fetch(driverOrigin + path, {
    method,
    headers: body === undefined ? undefined : { "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(30_000),
  });
  const envelope = await response.json();
  if (!response.ok || envelope.value?.error !== undefined) {
    throw new Error(`WebDriver ${method} ${path} failed: ${JSON.stringify(envelope)}`);
  }
  return envelope.value;
}

async function waitForDriver(driver) {
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    if (driver.exitCode !== null) {
      throw new Error(`geckodriver exited early:\n${driverLog}`);
    }
    try {
      const status = await webdriver("/status");
      if (status.ready === true) return;
    } catch {
      // geckodriver has not opened its loopback socket yet.
    }
    await delay(100);
  }
  throw new Error(`geckodriver did not become ready:\n${driverLog}`);
}

async function close(server) {
  await new Promise((resolve, reject) => {
    server.close((error) => error === undefined ? resolve() : reject(error));
    server.closeAllConnections();
  });
}

const [script, policyArchive] = await Promise.all([
  readFile(resolve(import.meta.dirname, "../../.build/browser/run-browser.js")),
  readFile(resolve(import.meta.dirname, "../fixtures/tracer-policy-store.cjar")),
]);
const requests = [];
const server = createServer((request, response) => {
  const path = new URL(request.url ?? "/", "http://fixture.invalid").pathname;
  requests.push(path);
  if (path === "/run-browser.js") {
    response.writeHead(200, { "content-type": "application/javascript" });
    response.end(script);
  } else if (path === "/tracer-policy-store.cjar") {
    response.writeHead(200, { "content-type": "application/octet-stream" });
    response.end(policyArchive);
  } else if (path === "/") {
    response.writeHead(200, { "content-type": "text/html" });
    response.end(`<!doctype html>
      <script>
        globalThis.cedarlingHarnessErrors = [];
        addEventListener("error", (event) => {
          cedarlingHarnessErrors.push(event.error?.stack ?? event.message);
        });
        addEventListener("unhandledrejection", (event) => {
          cedarlingHarnessErrors.push(String(event.reason?.stack ?? event.reason));
        });
      </script>
      <script type="module" src="/run-browser.js"></script>`);
  } else if (path === "/favicon.ico") {
    response.writeHead(204).end();
  } else {
    response.writeHead(404, { "content-type": "text/plain" });
    response.end(`No fixture asset for ${path}`);
  }
});

await new Promise((resolve, reject) => {
  server.once("error", reject);
  server.listen(0, "127.0.0.1", resolve);
});
const address = server.address();
if (address === null || typeof address === "string") {
  throw new Error("The Firefox ESR fixture server has no TCP address");
}

const driver = spawn("geckodriver", ["--host", "127.0.0.1", "--port", "4444"], {
  stdio: ["ignore", "pipe", "pipe"],
});
for (const stream of [driver.stdout, driver.stderr]) {
  stream.on("data", (chunk) => {
    driverLog += chunk;
  });
}

let sessionId;
try {
  await waitForDriver(driver);
  const session = await webdriver("/session", "POST", {
    capabilities: {
      alwaysMatch: {
        browserName: "firefox",
        "moz:firefoxOptions": {
          args: ["-headless"],
          binary: resolve(firefoxBinary),
        },
      },
    },
  });
  sessionId = session.sessionId;
  await webdriver(`/session/${sessionId}/url`, "POST", {
    url: `http://127.0.0.1:${address.port}/`,
  });

  const deadline = Date.now() + 150_000;
  let outcome;
  while (Date.now() < deadline) {
    outcome = await webdriver(`/session/${sessionId}/execute/sync`, "POST", {
      script: `return globalThis.cedarlingTestResult === undefined ? null : {
        result: globalThis.cedarlingTestResult,
        errors: globalThis.cedarlingHarnessErrors
      };`,
      args: [],
    });
    if (outcome !== null) break;
    await delay(200);
  }
  if (outcome === undefined || outcome === null) {
    throw new Error("Firefox ESR contracts timed out");
  }
  if (
    outcome.result?.failed !== 0 ||
    outcome.result?.error !== undefined ||
    outcome.errors?.length !== 0
  ) {
    throw new Error(`Firefox ESR contracts failed: ${JSON.stringify(outcome)}`);
  }
  if (requests.some((path) => path?.endsWith(".wasm"))) {
    throw new Error(`Firefox ESR requested a WASM asset: ${requests.join(", ")}`);
  }
  console.log(
    `Firefox ${session.capabilities?.browserVersion ?? "unknown"}: ` +
      "portable contracts passed without a WASM request",
  );
} finally {
  if (sessionId !== undefined) {
    await webdriver(`/session/${sessionId}`, "DELETE").catch(() => undefined);
  }
  driver.kill("SIGTERM");
  await close(server);
}
