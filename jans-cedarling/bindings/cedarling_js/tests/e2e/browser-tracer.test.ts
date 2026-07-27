import { execFile } from "node:child_process";
import { createHmac } from "node:crypto";
import { createServer } from "node:http";
import {
  cp,
  copyFile,
  mkdir,
  mkdtemp,
  readFile,
  readdir,
  rm,
  writeFile,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import { extname, join, resolve, sep } from "node:path";
import { promisify } from "node:util";

import type QUnitApi from "qunit";
import { tracerPolicyStore } from "../fixtures/tracer-policy-store.js";

const execute = promisify(execFile);

/**
 * Verifies that a required host binary is available, failing fast with an
 * actionable diagnostic when it is not.
 *
 * The browser end-to-end test runs an actual Chromium binary; without this
 * probe the failure mode is an opaque `ENOENT` from `execFile`.
 */
async function requireHostBinary(name: string): Promise<void> {
  try {
    await execute("which", [name]);
  } catch {
    throw new Error(
      `Required host binary "${name}" is not on PATH. ` +
        `Install Chrome (or set the host environment) before running this test.`,
    );
  }
}

/** Encodes one UTF-8 string as unpadded URL-safe base64. */
function base64Url(value: string): string {
  return Buffer.from(value, "utf8").toString("base64url");
}

/** Creates a synthetic compact JWT for the browser issuer fixture. */
function signFixtureToken(claims: Readonly<Record<string, unknown>>): string {
  const header = base64Url(JSON.stringify({
    alg: "HS256",
    typ: "statuslist+jwt",
  }));
  const payload = base64Url(JSON.stringify(claims));
  const input = `${header}.${payload}`;
  const signature = createHmac(
    "sha256",
    "cedarling-js-browser-status-signing-key",
  )
    .update(input)
    .digest("base64url");
  return `${input}.${signature}`;
}

/** Executes npm with an isolated cache owned by the temporary consumer. */
async function npm(
  arguments_: readonly string[],
  cwd: string,
  cacheDirectory: string,
): Promise<void> {
  await execute("npm", arguments_, {
    cwd,
    env: {
      ...process.env,
      npm_config_cache: cacheDirectory,
    },
  });
}

/** Resolves the sole tarball created in a temporary artifact directory. */
async function singleTarball(directory: string): Promise<string> {
  const tarballs = (await readdir(directory)).filter((entry) =>
    entry.endsWith(".tgz"),
  );
  if (tarballs.length !== 1 || tarballs[0] === undefined) {
    throw new Error("Expected npm pack to create exactly one tarball.");
  }
  return join(directory, tarballs[0]);
}

/** Returns a browser-safe content type for one staged file. */
function contentType(path: string): string {
  switch (extname(path)) {
    case ".html":
      return "text/html; charset=utf-8";
    case ".js":
    case ".mjs":
      return "text/javascript; charset=utf-8";
    case ".wasm":
      return "application/wasm";
    default:
      return "application/octet-stream";
  }
}

/** Sub-result posted by the consumer's inline-policy flow. */
interface InlineFlowResult {
  readonly decision: boolean;
  readonly hasRequestId: boolean;
  readonly correlatedDecisionLog: boolean;
  readonly reasons: readonly string[];
}

/** Sub-result posted by the consumer's URL-policy flow. */
interface UrlFlowResult {
  readonly decision: boolean;
  readonly hasRequestId: boolean;
  readonly correlatedDecisionLog: boolean;
  readonly reasons: readonly string[];
}

/** Sub-result posted by the consumer's multi-issuer flow. */
interface MultiIssuerFlowResult {
  readonly decision: boolean;
  readonly hasRequestId: boolean;
  readonly issuerReady: boolean;
  readonly reasons: readonly string[];
}

/** Sub-result posted by the consumer's revoked-token flow. */
interface RevokedFlowResult {
  readonly ok: boolean;
  readonly code: string;
  readonly operation: string;
}

/** Sub-result posted by the consumer's context flow. */
interface ContextFlowResult {
  readonly before: boolean;
  readonly after: boolean;
}

/** Aggregated payload the consumer POSTs back to the test harness. */
interface BrowserE2EResult {
  readonly inline: InlineFlowResult;
  readonly url: UrlFlowResult;
  readonly multiIssuer: MultiIssuerFlowResult;
  readonly revoked: RevokedFlowResult;
  readonly context: ContextFlowResult;
}

/** Per-module state shared across the per-flow QUnit tests. */
interface BrowserE2ESetup {
  readonly result: BrowserE2EResult;
  readonly cleanup: () => Promise<void>;
}

/**
 * Stages an isolated consumer, packs the SDK and WASM, installs them, starts
 * the inline HTTP server, drives headless Chrome, and returns the consumer's
 * aggregated result along with a cleanup callback. Called once per QUnit
 * module by `hooks.before`.
 */
async function stageBrowserRun(): Promise<BrowserE2ESetup> {
  const packageDirectory = process.cwd();
  const wasmDirectory = resolve(packageDirectory, "../cedarling_wasm/pkg");
  const temporaryRoot = await mkdtemp(
    join(tmpdir(), "cedarling-js-browser-tracer-"),
  );
  const artifactsDirectory = join(temporaryRoot, "artifacts");
  const sdkArtifactsDirectory = join(artifactsDirectory, "sdk");
  const wasmArtifactsDirectory = join(artifactsDirectory, "wasm");
  const sdkStageDirectory = join(temporaryRoot, "sdk-stage");
  const consumerDirectory = join(temporaryRoot, "consumer");
  const npmCacheDirectory = join(temporaryRoot, "npm-cache");
  const chromeProfile = join(temporaryRoot, "chrome-profile");
  let server: ReturnType<typeof createServer> | undefined;
  let serverOrigin = "";
  let statusListToken = "";

  let browserResult: BrowserE2EResult | undefined;

  const cleanup = async (): Promise<void> => {
    if (server !== undefined) {
      await new Promise<void>((resolveClose) => {
        server?.close(() => resolveClose());
      });
    }
    await rm(temporaryRoot, { force: true, recursive: true });
  };

  try {
    await mkdir(sdkArtifactsDirectory, { recursive: true });
    await mkdir(wasmArtifactsDirectory);
    await mkdir(consumerDirectory);
    await mkdir(sdkStageDirectory);

    await npm(
      ["pack", "--json", "--pack-destination", wasmArtifactsDirectory],
      wasmDirectory,
      npmCacheDirectory,
    );
    const sourceManifest = JSON.parse(
      await readFile(join(packageDirectory, "package.json"), "utf8"),
    ) as {
      dependencies: Record<string, string>;
      readonly [key: string]: unknown;
    };
    const wasmManifest = JSON.parse(
      await readFile(join(wasmDirectory, "package.json"), "utf8"),
    ) as { readonly version: string };
    await writeFile(
      join(sdkStageDirectory, "package.json"),
      JSON.stringify(
        {
          ...sourceManifest,
          dependencies: {
            ...sourceManifest.dependencies,
            "@janssenproject/cedarling_wasm": wasmManifest.version,
          },
        },
        undefined,
        2,
      ),
    );
    await copyFile(
      join(packageDirectory, "README.md"),
      join(sdkStageDirectory, "README.md"),
    );
    await cp(
      join(packageDirectory, "dist"),
      join(sdkStageDirectory, "dist"),
      { recursive: true },
    );
    await npm(
      [
        "pack",
        "--ignore-scripts",
        "--json",
        "--pack-destination",
        sdkArtifactsDirectory,
      ],
      sdkStageDirectory,
      npmCacheDirectory,
    );

    const wasmTarball = await singleTarball(wasmArtifactsDirectory);
    const sdkTarball = await singleTarball(sdkArtifactsDirectory);
    await writeFile(
      join(consumerDirectory, "package.json"),
      JSON.stringify({
        name: "cedarling-js-browser-tracer-consumer",
        private: true,
        type: "module",
        dependencies: {
          "@janssenproject/cedarling": `file:${sdkTarball}`,
          "@janssenproject/cedarling_wasm": `file:${wasmTarball}`,
        },
      }),
    );
    await Promise.all([
      copyFile(
        join(packageDirectory, "tests/fixtures/browser-tracer.html"),
        join(consumerDirectory, "index.html"),
      ),
      copyFile(
        join(
          packageDirectory,
          "tests/fixtures/browser-tracer-consumer.mjs",
        ),
        join(consumerDirectory, "browser-tracer-consumer.mjs"),
      ),
    ]);
    await npm(
      [
        "install",
        "--ignore-scripts",
        "--no-audit",
        "--no-fund",
        "--no-package-lock",
        "--offline",
      ],
      consumerDirectory,
      npmCacheDirectory,
    );

    server = createServer(async (request, response) => {
      const requestUrl = new URL(
        request.url ?? "/",
        "http://browser.test",
      );
      if (request.method === "POST" && requestUrl.pathname === "/result") {
        const chunks: Uint8Array[] = [];
        for await (const chunk of request) {
          chunks.push(
            typeof chunk === "string"
              ? new TextEncoder().encode(chunk)
              : new Uint8Array(chunk),
          );
        }
        const size = chunks.reduce(
          (total, chunk) => total + chunk.byteLength,
          0,
        );
        const body = new Uint8Array(size);
        let offset = 0;
        for (const chunk of chunks) {
          body.set(chunk, offset);
          offset += chunk.byteLength;
        }
        browserResult = JSON.parse(
          new TextDecoder().decode(body),
        ) as BrowserE2EResult;
        response.writeHead(204).end();
        return;
      }
      if (requestUrl.pathname === "/policy") {
        response.writeHead(200, {
          "content-type": "application/json",
        });
        response.end(JSON.stringify(tracerPolicyStore));
        return;
      }
      if (
        requestUrl.pathname ===
        "/.well-known/openid-configuration"
      ) {
        response.writeHead(200, {
          "content-type": "application/json",
        });
        response.end(JSON.stringify({
          issuer: serverOrigin,
          jwks_uri: `${serverOrigin}/jwks`,
          status_list_endpoint: `${serverOrigin}/status-list`,
        }));
        return;
      }
      if (requestUrl.pathname === "/jwks") {
        response.writeHead(200, {
          "content-type": "application/json",
        });
        response.end(JSON.stringify({ keys: [] }));
        return;
      }
      if (requestUrl.pathname === "/status-list") {
        response.writeHead(200, {
          "content-type": "application/statuslist+jwt",
        });
        response.end(statusListToken);
        return;
      }

      const pathname =
        requestUrl.pathname === "/"
          ? "/index.html"
          : decodeURIComponent(requestUrl.pathname);
      const candidate = resolve(
        consumerDirectory,
        `.${pathname}`,
      );
      if (
        candidate !== consumerDirectory &&
        !candidate.startsWith(`${consumerDirectory}${sep}`)
      ) {
        response.writeHead(403).end();
        return;
      }
      try {
        const contents = await readFile(candidate);
        response.writeHead(200, {
          "content-type": contentType(candidate),
        });
        response.end(contents);
      } catch {
        if (!response.headersSent) {
          response.writeHead(404).end();
        }
      }
    });

    await new Promise<void>((resolveListen, rejectListen) => {
      server?.once("error", rejectListen);
      server?.listen(0, "127.0.0.1", () => {
        server?.off("error", rejectListen);
        resolveListen();
      });
    });
    const address = server.address();
    if (address === null || typeof address === "string") {
      throw new Error("The browser E2E server did not bind a port.");
    }
    serverOrigin = `http://127.0.0.1:${address.port}`;
    statusListToken = signFixtureToken({
      sub: `${serverOrigin}/status-list`,
      iss: serverOrigin,
      iat: 1_700_000_000,
      exp: 4_000_000_000,
      ttl: 300,
      status_list: {
        bits: 1,
        lst: "eNrbuRgAAhcBXQ",
      },
    });

    await requireHostBinary("google-chrome");
    await execute(
      "google-chrome",
      [
        "--headless=new",
        "--no-sandbox",
        "--disable-gpu",
        "--disable-dev-shm-usage",
        "--disable-background-networking",
        "--disable-extensions",
        `--user-data-dir=${chromeProfile}`,
        "--virtual-time-budget=15000",
        "--dump-dom",
        `http://127.0.0.1:${address.port}/`,
      ],
      { cwd: consumerDirectory },
    );

    if (browserResult === undefined) {
      throw new Error(
        "The consumer did not POST a result before Chrome exited. " +
          "This usually means the consumer page crashed or its module failed to load.",
      );
    }

    return { result: browserResult, cleanup };
  } catch (error) {
    await cleanup();
    throw error;
  }
}

/** Registers the clean packed-package browser end-to-end flows. */
export default function registerBrowserEndToEndTests(QUnit: QUnitApi): void {
  QUnit.module("browser-tracer", (hooks) => {
    let setup: BrowserE2ESetup | undefined;

    hooks.before(async (assert) => {
      assert.timeout(120_000);
      setup = await stageBrowserRun();
    });

    hooks.after(async () => {
      if (setup !== undefined) {
        await setup.cleanup();
        setup = undefined;
      }
    });

    QUnit.test("consumer posts a result", (assert) => {
      assert.ok(setup !== undefined, "the consumer posted a result before Chrome exited");
    });

    QUnit.test("inline-policy flow allows the tracer decision", (assert) => {
      assert.ok(setup !== undefined, "setup completed");
      assert.deepEqual(setup?.result.inline, {
        decision: true,
        hasRequestId: true,
        correlatedDecisionLog: true,
        reasons: ["allow"],
      });
    });

    QUnit.test("url-policy flow allows the tracer decision", (assert) => {
      assert.ok(setup !== undefined, "setup completed");
      assert.deepEqual(setup?.result.url, {
        decision: true,
        hasRequestId: true,
        correlatedDecisionLog: true,
        reasons: ["allow"],
      });
    });

    QUnit.test("multi-issuer flow allows the tracer decision", (assert) => {
      assert.ok(setup !== undefined, "setup completed");
      assert.deepEqual(setup?.result.multiIssuer, {
        decision: true,
        hasRequestId: true,
        issuerReady: true,
        reasons: ["token_present"],
      });
    });

    QUnit.test("revoked token is rejected with an authorization failure", (assert) => {
      assert.ok(setup !== undefined, "setup completed");
      assert.deepEqual(setup?.result.revoked, {
        ok: false,
        code: "AUTHORIZATION_FAILED",
        operation: "authorizeMultiIssuer",
      });
    });

    QUnit.test("context write becomes visible after authorize", (assert) => {
      assert.ok(setup !== undefined, "setup completed");
      assert.deepEqual(setup?.result.context, {
        before: false,
        after: true,
      });
    });
  });
}
