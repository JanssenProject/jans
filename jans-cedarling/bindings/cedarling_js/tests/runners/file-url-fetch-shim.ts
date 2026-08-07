import { readFile } from "node:fs/promises";

/**
 * Installs a `file:`-URL asset-delivery fetch hook.
 *
 * The generated `cedarling_wasm` web glue loads its adjacent `.wasm` asset
 * through the standard `fetch` API. Node, Bun, Deno, and Electron each
 * provide a host `fetch` that does not understand `file:` URLs. This shim
 * detects `file:` inputs and serves them from the local filesystem,
 * delegating every other request to the host implementation.
 *
 * The function is intentionally runtime-neutral: it relies only on the
 * standard `fetch`, `URL`, and `Response` globals, plus the Node-compatible
 * `node:fs/promises` import which each of the supported runtimes supplies.
 */
export function installFileUrlFetchShim(): void {
  const hostFetch = globalThis.fetch.bind(globalThis);

  globalThis.fetch = async (
    input: string | URL | Request,
    init?: RequestInit,
  ): Promise<Response> => {
    let url: URL | undefined;
    try {
      url =
        input instanceof URL
          ? input
          : new URL(typeof input === "string" ? input : input.url);
    } catch {
      // Delegate non-URL inputs to the host implementation unchanged.
    }

    if (url?.protocol === "file:") {
      return new Response(await readFile(url), {
        headers: { "content-type": "application/wasm" },
      });
    }
    return hostFetch(input, init);
  };
}
