import {
  copyFile,
  cp,
  mkdir,
  mkdtemp,
  readFile,
  rm,
  writeFile,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { createServer } from "node:http";

import type { IsolatedSdkEntry, RuntimeFixtures } from "../run.js";
import { tracerPolicyStore } from "./tracer-policy-store.js";

const packageRoot = resolve(import.meta.dirname, "..", "..");
const generatedMain = fileURLToPath(
  import.meta.resolve("@janssenproject/cedarling_wasm"),
);
const generatedRoot = dirname(generatedMain);

/**
 * Copies only the runtime files needed to resolve the SDK and generated
 * package from an isolated consumer.
 */
async function prepareIsolatedStage(
  stage: string,
  includeWasmAsset: boolean,
): Promise<{
  readonly consumerEntry: string;
  readonly stagedWasmAsset: string;
}> {
  const sdkRoot = join(
    stage,
    "node_modules",
    "@janssenproject",
    "cedarling",
  );
  const wasmRoot = join(
    stage,
    "node_modules",
    "@janssenproject",
    "cedarling_wasm",
  );
  await mkdir(sdkRoot, { recursive: true });
  await mkdir(wasmRoot, { recursive: true });
  await cp(join(packageRoot, "dist"), join(sdkRoot, "dist"), {
    recursive: true,
  });
  await copyFile(
    join(packageRoot, "package.json"),
    join(sdkRoot, "package.json"),
  );
  await copyFile(
    join(generatedRoot, "package.json"),
    join(wasmRoot, "package.json"),
  );
  await copyFile(generatedMain, join(wasmRoot, "cedarling_wasm.js"));
  const stagedWasmAsset = join(wasmRoot, "cedarling_wasm_bg.wasm");
  if (includeWasmAsset) {
    await copyFile(
      join(generatedRoot, "cedarling_wasm_bg.wasm"),
      stagedWasmAsset,
    );
  }

  const consumerEntry = join(stage, "consumer.mjs");
  await writeFile(
    consumerEntry,
    'export { createCedarling } from "@janssenproject/cedarling";\n',
    "utf8",
  );

  return {
    consumerEntry,
    stagedWasmAsset,
  };
}

/** Imports the public entry from one isolated consumer stage. */
async function importIsolatedSdk(consumerEntry: string): Promise<IsolatedSdkEntry> {
  return (await import(
    `${pathToFileURL(consumerEntry).href}?stage=${encodeURIComponent(
      dirname(consumerEntry),
    )}`
  )) as IsolatedSdkEntry;
}

/** Node-compatible capability fixtures injected into the shared QUnit contract. */
export const nodeRuntimeFixtures: RuntimeFixtures = {
  runtime: "node",

  async loadTracerArchive(): Promise<Uint8Array> {
    return new Uint8Array(
      await readFile(
        join(packageRoot, "tests", "fixtures", "tracer-policy-store.cjar"),
      ),
    );
  },

  async withMissingWasmAsset(run): Promise<void> {
    const stage = await mkdtemp(
      join(tmpdir(), "cedarling-js-missing-wasm-"),
    );

    try {
      const { consumerEntry, stagedWasmAsset } =
        await prepareIsolatedStage(stage, false);
      const sdk = await importIsolatedSdk(consumerEntry);

      await run({
        sdk,
        async restoreWasmAsset(): Promise<void> {
          await copyFile(
            join(generatedRoot, "cedarling_wasm_bg.wasm"),
            stagedWasmAsset,
          );
        },
      });
    } finally {
      await rm(stage, { recursive: true, force: true });
    }
  },

  async withMissingWebAssembly(run): Promise<void> {
    const stage = await mkdtemp(
      join(tmpdir(), "cedarling-js-missing-webassembly-"),
    );
    const descriptor = Object.getOwnPropertyDescriptor(
      globalThis,
      "WebAssembly",
    );

    try {
      const { consumerEntry } = await prepareIsolatedStage(stage, true);
      const sdk = await importIsolatedSdk(consumerEntry);
      Object.defineProperty(globalThis, "WebAssembly", {
        configurable: true,
        enumerable: false,
        value: undefined,
        writable: true,
      });
      await run(sdk);
    } finally {
      if (descriptor === undefined) {
        Reflect.deleteProperty(globalThis, "WebAssembly");
      } else {
        Object.defineProperty(globalThis, "WebAssembly", descriptor);
      }
      await rm(stage, { recursive: true, force: true });
    }
  },

  async withPolicyServer(run): Promise<void> {
    let requests = 0;
    let jsonStatus = 200;
    let jsonBody = JSON.stringify(tracerPolicyStore);
    const archive = await readFile(
      join(packageRoot, "tests", "fixtures", "tracer-policy-store.cjar"),
    );
    const server = createServer((request, response) => {
      requests += 1;
      const pathname = new URL(request.url ?? "/", "http://fixture").pathname;
      if (pathname === "/archive-policy") {
        response.writeHead(200, {
          "content-type": "application/octet-stream",
        });
        response.end(archive);
        return;
      }
      if (pathname !== "/policy-store") {
        response.writeHead(404).end();
        return;
      }
      response.writeHead(jsonStatus, {
        "content-type": "application/json",
      });
      response.end(jsonBody);
    });

    await new Promise<void>((resolveListen, rejectListen) => {
      server.once("error", rejectListen);
      server.listen(0, "127.0.0.1", () => {
        server.off("error", rejectListen);
        resolveListen();
      });
    });
    const address = server.address();
    if (address === null || typeof address === "string") {
      server.close();
      throw new Error("The policy fixture did not bind a TCP port.");
    }

    try {
      await run({
        jsonUrl: `http://127.0.0.1:${address.port}/policy-store`,
        archiveUrl: `http://127.0.0.1:${address.port}/archive-policy`,
        requestCount: () => requests,
        setJsonResponse(status, body): void {
          jsonStatus = status;
          jsonBody = body;
        },
        async waitForRequestCount(count): Promise<void> {
          const deadline = Date.now() + 8_000;
          while (requests < count) {
            if (Date.now() >= deadline) {
              throw new Error("Timed out waiting for policy fixture requests.");
            }
            await new Promise<void>((resolveWait) => {
              setTimeout(resolveWait, 25);
            });
          }
        },
      });
    } finally {
      await new Promise<void>((resolveClose, rejectClose) => {
        server.close((error) => {
          if (error === undefined) {
            resolveClose();
          } else {
            rejectClose(error);
          }
        });
      });
    }
  },
};
