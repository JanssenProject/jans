import { readFileSync } from "node:fs";
import { createServer } from "node:http";
import { expect, test } from "@playwright/test";

test("passes tests in browser", async ({ page }) => {
  const script = readFileSync("./.test-dist/.build/run-browser.js", "utf-8");
  const wasmAsset = readFileSync(
    "./node_modules/@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm",
  );
  const tracerArchive = readFileSync(
    "./tests/fixtures/tracer-policy-store.cjar",
  );
  const server = createServer((req, res) => {
    if (req.url === "/run-browser.js") {
      res.writeHead(200, { "Content-Type": "application/javascript" });
      res.end(script);
    } else if (req.url === "/cedarling_wasm_bg.wasm") {
      res.writeHead(200, { "Content-Type": "application/wasm" });
      res.end(wasmAsset);
    } else if (req.url === "/tracer-policy-store.cjar") {
      res.writeHead(200, { "Content-Type": "application/octet-stream" });
      res.end(tracerArchive);
    } else {
      res.writeHead(200, { "Content-Type": "text/html" });
      res.end(
        '<!DOCTYPE html><html><head></head><body><script type="module" src="/run-browser.js"></script></body></html>',
      );
    }
  });

  await new Promise<void>((resolve) => server.listen(0, resolve));
  const port = (server.address() as import("node:net").AddressInfo).port;

  await page.goto(`http://localhost:${port}`);

  const stats = await page.waitForFunction(
    () => (globalThis as unknown as { stats: { failed: number } | undefined }).stats,
    undefined,
    { timeout: 60_000 },
  );
  const statsValue = await stats.jsonValue();

  server.close();
  expect(statsValue).toBeDefined();
  expect(statsValue?.failed).toBe(0);
});
