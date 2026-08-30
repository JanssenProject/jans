import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createServer } from "node:http";
import test from "node:test";

import { chromium, firefox, webkit } from "@playwright/test";

const bundlePath = process.env.CEDARLING_BROWSER_BUNDLE;
const archivePath = process.env.CEDARLING_BROWSER_ARCHIVE;
const wasmPath = process.env.CEDARLING_BROWSER_WASM;
const expectsWasmRequest = process.env.CEDARLING_BROWSER_EXPECT_WASM_REQUEST === "true";
if (bundlePath === undefined || archivePath === undefined) {
  throw new Error(
    "CEDARLING_BROWSER_BUNDLE and CEDARLING_BROWSER_ARCHIVE are required",
  );
}

async function createFixtureServer() {
  const [bundle, archive, wasm] = await Promise.all([
    readFile(bundlePath),
    readFile(archivePath),
    wasmPath === undefined ? undefined : readFile(wasmPath),
  ]);
  const requests = [];
  const server = createServer((request, response) => {
    const path = new URL(
      request.url ?? "/",
      "http://fixture.invalid",
    ).pathname;
    requests.push(path);
    if (path === "/consumer.js") {
      response.writeHead(200, { "content-type": "application/javascript" });
      response.end(bundle);
    } else if (path === "/policy.cjar") {
      response.writeHead(200, { "content-type": "application/octet-stream" });
      response.end(archive);
    } else if (wasm !== undefined && path === `/${wasmPath.split("/").at(-1)}`) {
      response.writeHead(200, { "content-type": "application/wasm" });
      response.end(wasm);
    } else if (path === "/") {
      response.writeHead(200, { "content-type": "text/html" });
      response.end(
        '<!doctype html><script type="module" src="/consumer.js"></script>',
      );
    } else if (path === "/favicon.ico") {
      response.writeHead(204);
      response.end();
    } else {
      response.writeHead(404, { "content-type": "text/plain" });
      response.end("Missing fixture asset");
    }
  });
  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      server.off("error", reject);
      resolve();
    });
  });
  const address = server.address();
  if (address === null || typeof address === "string") {
    throw new Error("Browser fixture server has no TCP address");
  }
  return {
    requests,
    server,
    url: "http://127.0.0.1:" + String(address.port) + "/",
  };
}

async function close(server) {
  await new Promise((resolve, reject) => {
    server.close((error) => error === undefined ? resolve() : reject(error));
    server.closeAllConnections();
  });
}

for (const [name, browserType] of [
  ["chromium", chromium],
  ["firefox", firefox],
  ["webkit", webkit],
]) {
  test(
    name + " executes the packed browser consumer with the expected WASM loading",
    { concurrency: false, timeout: 150_000 },
    async () => {
      const fixture = await createFixtureServer();
      const browser = await browserType.launch({ headless: true });
      try {
        const page = await browser.newPage();
        const diagnostics = [];
        page.on("pageerror", (error) => {
          diagnostics.push(error.stack ?? error.message);
        });
        page.on("console", (message) => {
          if (message.type() === "error") diagnostics.push(message.text());
        });
        await page.goto(fixture.url);
        const result = await page.waitForFunction(
          () => globalThis.cedarlingTestResult,
          undefined,
          { timeout: 120_000 },
        );
        const value = await result.jsonValue();
        assert.deepEqual(diagnostics, []);
        assert.deepEqual(value, { ok: true });
        assert.equal(
          fixture.requests.some((path) => path.endsWith(".wasm")),
          expectsWasmRequest,
        );
      } finally {
        await browser.close();
        await close(fixture.server);
      }
    },
  );
}
