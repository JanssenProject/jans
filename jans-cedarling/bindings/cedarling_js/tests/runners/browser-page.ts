import { readFile } from "node:fs/promises";
import { createServer, type Server } from "node:http";
import { resolve } from "node:path";
import { expect, test } from "@playwright/test";

const packageRoot = resolve(import.meta.dirname, "../..");

async function close(server: Server): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    server.close((error) => error === undefined ? resolve() : reject(error));
    server.closeAllConnections();
  });
}

test("portable contracts pass in a real browser", async ({ page }) => {
  const [script, wasm] = await Promise.all([
    readFile(resolve(packageRoot, ".test-dist/.build/run-browser.js")),
    readFile(resolve(
      packageRoot,
      "node_modules/@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm",
    )),
  ]);
  const diagnostics: string[] = [];
  page.on("pageerror", (error) => diagnostics.push(error.stack ?? error.message));
  page.on("console", (message) => {
    if (message.type() === "error") diagnostics.push(message.text());
  });
  const server = createServer((request, response) => {
    if (request.url === "/run-browser.js") {
      response.writeHead(200, { "content-type": "application/javascript" });
      response.end(script);
    } else if (request.url === "/cedarling_wasm_bg.wasm") {
      response.writeHead(200, { "content-type": "application/wasm" });
      response.end(wasm);
    } else if (request.url === "/") {
      response.writeHead(200, { "content-type": "text/html" });
      response.end('<!doctype html><script type="module" src="/run-browser.js"></script>');
    } else {
      response.writeHead(404, { "content-type": "text/plain" });
      response.end(`No fixture asset for ${request.url}`);
    }
  });

  await new Promise<void>((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      server.off("error", reject);
      resolve();
    });
  });
  try {
    const address = server.address();
    if (address === null || typeof address === "string") {
      throw new Error("The browser fixture server has no TCP address.");
    }
    await page.goto(`http://127.0.0.1:${address.port}`);
    const result = await page.waitForFunction(
      () => globalThis.cedarlingTestResult,
      undefined,
      { timeout: 90_000 },
    );
    const value = await result.jsonValue();
    expect(diagnostics, diagnostics.join("\n")).toEqual([]);
    if (value === undefined) {
      throw new Error("The browser contracts did not publish a result.");
    }
    expect(value.error).toBeUndefined();
    expect(value.failed).toBe(0);
  } finally {
    await close(server);
  }
});
