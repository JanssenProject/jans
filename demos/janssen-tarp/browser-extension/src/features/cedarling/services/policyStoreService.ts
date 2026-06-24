import { unzipSync } from 'fflate';

/**
 * A single extracted entry from a policy store archive (.cjar).
 * `path` is the full POSIX-style path within the archive (e.g. "policies/allow.cedar").
 */
export interface PolicyStoreEntry {
  path: string;
  size: number;
  bytes: Uint8Array;
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
  let response: Response;
  try {
    response = await fetch(uri, { method: 'GET' });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(`Failed to download policy store from "${uri}": ${msg}`);
  }

  if (!response.ok) {
    throw new Error(`Failed to download policy store: HTTP ${response.status} ${response.statusText}`);
  }

  const buffer = new Uint8Array(await response.arrayBuffer());

  let extracted: Record<string, Uint8Array>;
  try {
    extracted = unzipSync(buffer);
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
