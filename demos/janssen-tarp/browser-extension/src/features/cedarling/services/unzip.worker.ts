import { unzipSync } from 'fflate';

interface UnzipRequest {
  bytes: Uint8Array;
  maxEntries: number;
  maxTotalBytes: number;
}

/**
 * Web Worker that runs ZIP decompression off the main thread.
 *
 * Enforces a decompression-time budget (entry count + cumulative uncompressed
 * size) via fflate's `filter`, which inspects each entry's declared size BEFORE
 * inflating it. This defends against decompression bombs whose compressed
 * download is small but whose inflated contents would OOM the worker.
 *
 * Posts back the extracted entries, or an error message on failure / budget breach.
 */
self.onmessage = (e: MessageEvent<UnzipRequest>) => {
  const { bytes, maxEntries, maxTotalBytes } = e.data;
  try {
    let entryCount = 0;
    let totalBytes = 0;

    const result = unzipSync(bytes, {
      filter: (file) => {
        entryCount += 1;
        if (entryCount > maxEntries) {
          throw new Error(`Archive has too many entries (limit ${maxEntries}).`);
        }
        // `originalSize` is the uncompressed size declared in the ZIP header.
        totalBytes += file.originalSize;
        if (totalBytes > maxTotalBytes) {
          throw new Error(
            `Archive decompresses to too much data (limit ${Math.round(maxTotalBytes / (1024 * 1024))} MB).`,
          );
        }
        return true;
      },
    });

    const transfer = Object.values(result).map((b) => b.buffer);
    (self as unknown as Worker).postMessage({ ok: true, result }, transfer);
  } catch (err) {
    const error = err instanceof Error ? err.message : String(err);
    (self as unknown as Worker).postMessage({ ok: false, error });
  }
};
