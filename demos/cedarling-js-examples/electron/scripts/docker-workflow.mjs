import { readdir } from "node:fs/promises";

export const ELECTRON_IDP_ISSUER = "http://localhost:9090";

const SDK_PREFIX = "janssenproject-cedarling-";
const WASM_PREFIX = "janssenproject-cedarling_wasm-";

export function composeArguments(examplesRoot, composeFile, ...arguments_) {
  return [
    "compose",
    "--project-directory",
    examplesRoot,
    "--file",
    composeFile,
    "--profile",
    "electron",
    ...arguments_,
  ];
}

export async function validatePackageArtifacts(
  directory,
  readDirectory = readdir,
) {
  const tarballs = (await readDirectory(directory)).filter((entry) =>
    entry.endsWith(".tgz"),
  );
  const sdk = tarballs.filter((entry) => entry.startsWith(SDK_PREFIX));
  const wasm = tarballs.filter((entry) => entry.startsWith(WASM_PREFIX));
  if (tarballs.length !== 2 || sdk.length !== 1 || wasm.length !== 1) {
    throw new Error(
      "Docker must export exactly one Cedarling SDK and one WASM tarball.",
    );
  }

  const sdkVersion = sdk[0].slice(SDK_PREFIX.length, -".tgz".length);
  const wasmVersion = wasm[0].slice(WASM_PREFIX.length, -".tgz".length);
  if (!sdkVersion || sdkVersion !== wasmVersion) {
    throw new Error("Docker exported mismatched Cedarling package versions.");
  }
  return { sdk: sdk[0], version: sdkVersion, wasm: wasm[0] };
}

export function nativeElectronEnvironment(environment = process.env) {
  const result = {
    ...environment,
    OIDC_ISSUER: ELECTRON_IDP_ISSUER,
  };
  // This flag makes Electron behave like plain Node and prevents a GUI launch.
  delete result.ELECTRON_RUN_AS_NODE;
  return result;
}

export function signalExitCode(signal) {
  if (signal === "SIGINT") return 130;
  if (signal === "SIGTERM") return 143;
  return 1;
}

export async function waitForDiscovery({
  fetchImplementation = fetch,
  issuer = ELECTRON_IDP_ISSUER,
  retryDelayMs = 250,
  sleep = (milliseconds) =>
    new Promise((resolve) => setTimeout(resolve, milliseconds)),
  timeoutMs = 30_000,
} = {}) {
  const discoveryUrl = `${issuer}/.well-known/openid-configuration`;
  const deadline = Date.now() + timeoutMs;
  let lastError;

  do {
    try {
      const response = await fetchImplementation(discoveryUrl, {
        signal: AbortSignal.timeout(Math.min(2_000, timeoutMs)),
      });
      if (!response.ok) {
        throw new Error(`discovery returned HTTP ${response.status}`);
      }
      const document = await response.json();
      if (document.issuer !== issuer) {
        throw new Error(`discovery issuer is ${String(document.issuer)}`);
      }
      return document;
    } catch (error) {
      lastError = error;
      if (Date.now() >= deadline) break;
      await sleep(retryDelayMs);
    }
  } while (Date.now() < deadline);

  throw new Error(
    `Electron IdP did not become ready at ${discoveryUrl}: ${
      lastError instanceof Error ? lastError.message : "unknown error"
    }`,
  );
}
