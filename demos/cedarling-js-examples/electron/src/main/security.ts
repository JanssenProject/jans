import { readFile } from "node:fs/promises";
import path from "node:path";

import type { WebContents } from "electron";

type RendererWebContents = Pick<WebContents, "id" | "session">;
type ResponseHeaders = Record<string, string[]>;

const rendererOrigin = "app://renderer";

const contentTypes: Readonly<Record<string, string>> = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".map": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".wasm": "application/wasm",
};

export function rendererContentSecurityPolicy(
  issuerOrigin: string,
  { development = false }: { development?: boolean } = {},
): string {
  const issuer = new URL(issuerOrigin);
  if (!["http:", "https:"].includes(issuer.protocol) || issuer.origin !== issuerOrigin) {
    throw new Error("The renderer CSP requires an exact HTTP(S) issuer origin");
  }
  return [
    "default-src 'none'",
    "base-uri 'none'",
    "form-action 'none'",
    "frame-ancestors 'none'",
    "object-src 'none'",
    `script-src 'self' 'wasm-unsafe-eval'${development ? " 'unsafe-inline'" : ""}`,
    `style-src 'self'${development ? " 'unsafe-inline'" : ""}`,
    "img-src 'self' data:",
    "font-src 'self'",
    `connect-src 'self' ${issuer.origin}`,
  ].join("; ");
}

export function withRendererContentSecurityPolicy(
  headers: ResponseHeaders | undefined,
  policy: string,
): ResponseHeaders {
  const withoutExistingPolicy = Object.fromEntries(
    Object.entries(headers ?? {}).filter(
      ([name]) => name.toLowerCase() !== "content-security-policy",
    ),
  );
  return {
    ...withoutExistingPolicy,
    "Content-Security-Policy": [policy],
  };
}

export function installRendererContentSecurityPolicy(
  webContents: RendererWebContents,
  rendererUrl: string,
  issuerOrigin: string,
): void {
  const expectedRendererUrl = new URL(rendererUrl).href;
  // Vite's React-refresh preamble and CSS HMR are inline in development. This
  // relaxation applies only to the local dev-server document; built assets use
  // rendererAssetResponse and retain the strict no-inline policy.
  const policy = rendererContentSecurityPolicy(issuerOrigin, {
    development: true,
  });
  webContents.session.webRequest.onHeadersReceived(
    { urls: ["<all_urls>"], types: ["mainFrame"] },
    (details, callback) => {
      if (
        details.webContentsId !== webContents.id ||
        new URL(details.url).href !== expectedRendererUrl
      ) {
        callback({});
        return;
      }
      callback({
        responseHeaders: withRendererContentSecurityPolicy(
          details.responseHeaders,
          policy,
        ),
      });
    },
  );
}

export function resolveRendererAssetPath(
  rendererRoot: string,
  requestUrl: string,
): string | null {
  let url: URL;
  try {
    url = new URL(requestUrl);
  } catch {
    return null;
  }
  if (`${url.protocol}//${url.host}` !== rendererOrigin || url.search || url.hash) {
    return null;
  }

  let requestedPath: string;
  try {
    requestedPath = decodeURIComponent(url.pathname);
  } catch {
    return null;
  }
  const relativePath = requestedPath === "/"
    ? "index.html"
    : requestedPath.replace(/^\/+/, "");
  const root = path.resolve(rendererRoot);
  const candidate = path.resolve(root, relativePath);
  const relative = path.relative(root, candidate);
  if (relative.startsWith("..") || path.isAbsolute(relative)) return null;
  return candidate;
}

export async function rendererAssetResponse(
  rendererRoot: string,
  requestUrl: string,
  issuerOrigin: string,
): Promise<Response> {
  const assetPath = resolveRendererAssetPath(rendererRoot, requestUrl);
  if (!assetPath) return new Response("Not found", { status: 404 });

  let content: ArrayBuffer;
  try {
    const file = await readFile(assetPath);
    content = file.buffer.slice(
      file.byteOffset,
      file.byteOffset + file.byteLength,
    ) as ArrayBuffer;
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") {
      return new Response("Not found", { status: 404 });
    }
    throw error;
  }

  return new Response(content, {
    headers: {
      "Cache-Control": "no-store",
      "Content-Security-Policy": rendererContentSecurityPolicy(issuerOrigin),
      "Content-Type": contentTypes[path.extname(assetPath)] ?? "application/octet-stream",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
