/**
 * A single extracted entry from a policy store archive (.cjar).
 * `path` is the full POSIX-style path within the archive (e.g. "policies/allow.cedar").
 */
export interface PolicyStoreEntry {
  path: string;
  size: number;
  bytes: Uint8Array;
}

/** Maximum accepted archive download size (bytes). */
const MAX_ARCHIVE_BYTES = 50 * 1024 * 1024; // 50 MB
/** Maximum total uncompressed size after extraction (bytes) — zip-bomb guard. */
const MAX_UNCOMPRESSED_BYTES = 200 * 1024 * 1024; // 200 MB
/** Maximum number of entries in the archive — zip-bomb guard. */
const MAX_ENTRIES = 10_000;
/** Maximum time to wait for the download before aborting (ms). */
const DOWNLOAD_TIMEOUT_MS = 30_000;

function formatBytesLimit(bytes: number): string {
  return `${Math.round(bytes / (1024 * 1024))} MB`;
}

/**
 * Decompresses archive bytes in a Web Worker so the UI thread stays responsive.
 * The worker is bundled from extension origin to satisfy the MV3 CSP
 * (script-src 'self'), unlike fflate's blob-based async API.
 */
function unzipInWorker(bytes: Uint8Array): Promise<Record<string, Uint8Array>> {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('./unzip.worker.ts', import.meta.url));
    worker.onmessage = (e: MessageEvent<{ ok: boolean; result?: Record<string, Uint8Array>; error?: string }>) => {
      worker.terminate();
      if (e.data.ok && e.data.result) {
        resolve(e.data.result);
      } else {
        reject(new Error(e.data.error || 'Failed to decompress archive'));
      }
    };
    worker.onerror = (e) => {
      worker.terminate();
      reject(new Error(`Decompression worker failed: ${e.message}`));
    };
    // Transfer the buffer so it isn't copied into the worker.
    worker.postMessage(
      { bytes, maxEntries: MAX_ENTRIES, maxTotalBytes: MAX_UNCOMPRESSED_BYTES },
      [bytes.buffer],
    );
  });
}

/** A node in the rendered directory tree. */
export interface TreeNode {
  name: string;
  path: string;
  isDir: boolean;
  children: TreeNode[];
  entry?: PolicyStoreEntry;
}

const textDecoder = new TextDecoder('utf-8', { fatal: false });

/**
 * Reads the `CEDARLING_POLICY_STORE_URI` value from a bootstrap config object.
 * Returns an empty string when the key is absent or blank.
 */
export function getPolicyStoreUri(config: any): string {
  const uri = config?.CEDARLING_POLICY_STORE_URI;
  return typeof uri === 'string' ? uri.trim() : '';
}

/**
 * Downloads a `.cjar` (ZIP) archive from the given URL and extracts every entry.
 * Throws a descriptive Error on network or parse failure.
 */
export async function downloadAndExtractPolicyStore(uri: string): Promise<PolicyStoreEntry[]> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), DOWNLOAD_TIMEOUT_MS);

  let response: Response;
  try {
    response = await fetch(uri, { method: 'GET', signal: controller.signal });
  } catch (err) {
    if (controller.signal.aborted) {
      throw new Error(
        `Timed out downloading policy store after ${DOWNLOAD_TIMEOUT_MS / 1000}s. The archive may be too large or the server unreachable.`,
      );
    }
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(`Failed to download policy store from "${uri}": ${msg}`);
  } finally {
    clearTimeout(timeout);
  }

  if (!response.ok) {
    throw new Error(`Failed to download policy store: HTTP ${response.status} ${response.statusText}`);
  }

  // Reject oversized archives up front via the advertised length, before buffering.
  const declaredLength = Number(response.headers.get('content-length'));
  if (Number.isFinite(declaredLength) && declaredLength > MAX_ARCHIVE_BYTES) {
    throw new Error(
      `Policy store archive is too large (${formatBytesLimit(declaredLength)}); the limit is ${formatBytesLimit(MAX_ARCHIVE_BYTES)}.`,
    );
  }

  const arrayBuffer = await response.arrayBuffer();
  // Guard again on the actual size in case Content-Length was absent or wrong.
  if (arrayBuffer.byteLength > MAX_ARCHIVE_BYTES) {
    throw new Error(
      `Policy store archive is too large (${formatBytesLimit(arrayBuffer.byteLength)}); the limit is ${formatBytesLimit(MAX_ARCHIVE_BYTES)}.`,
    );
  }

  const buffer = new Uint8Array(arrayBuffer);

  let extracted: Record<string, Uint8Array>;
  try {
    extracted = await unzipInWorker(buffer);
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(`Could not read the policy store archive (expected a .cjar/ZIP file): ${msg}`);
  }

  return Object.entries(extracted)
    // fflate emits directory entries with trailing "/" and zero-length content; skip them.
    .filter(([path]) => !path.endsWith('/'))
    .map(([path, bytes]) => ({ path, size: bytes.length, bytes }))
    .sort((a, b) => a.path.localeCompare(b.path));
}

/**
 * Builds a nested directory tree from a flat list of archive entries.
 * Directories are listed before files and each level is sorted alphabetically.
 */
export function buildTree(entries: PolicyStoreEntry[]): TreeNode {
  const root: TreeNode = { name: '', path: '', isDir: true, children: [] };

  for (const entry of entries) {
    const parts = entry.path.split('/').filter(Boolean);
    let current = root;

    parts.forEach((part, index) => {
      const isLeaf = index === parts.length - 1;
      const path = parts.slice(0, index + 1).join('/');
      let child = current.children.find((c) => c.name === part);

      if (!child) {
        child = { name: part, path, isDir: !isLeaf, children: [] };
        current.children.push(child);
      }

      if (isLeaf) {
        child.isDir = false;
        child.entry = entry;
      }

      current = child;
    });
  }

  sortTree(root);
  return root;
}

function sortTree(node: TreeNode) {
  node.children.sort((a, b) => {
    if (a.isDir !== b.isDir) return a.isDir ? -1 : 1;
    return a.name.localeCompare(b.name);
  });
  node.children.forEach(sortTree);
}

/** Decodes an entry's bytes as UTF-8 text. */
export function decodeText(entry: PolicyStoreEntry): string {
  return textDecoder.decode(entry.bytes);
}

/** Returns true when the entry looks like UTF-8 text safe to display inline. */
export function isTextEntry(entry: PolicyStoreEntry): boolean {
  const sample = entry.bytes.subarray(0, Math.min(entry.bytes.length, 4096));
  for (const byte of sample) {
    // Allow common whitespace control chars; reject other control bytes (binary).
    if (byte === 0) return false;
    if (byte < 9 || (byte > 13 && byte < 32)) return false;
  }
  return true;
}

/** Human-readable byte size. */
export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
