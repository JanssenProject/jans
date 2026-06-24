import { unzipSync } from 'fflate';

/**
 * Web Worker that runs ZIP decompression off the main thread.
 * Receives the raw archive bytes and posts back the extracted entries
 * (or an error message). Entry buffers are transferred, not copied.
 */
self.onmessage = (e: MessageEvent<Uint8Array>) => {
  try {
    const result = unzipSync(e.data);
    const transfer = Object.values(result).map((bytes) => bytes.buffer);
    (self as unknown as Worker).postMessage({ ok: true, result }, transfer);
  } catch (err) {
    const error = err instanceof Error ? err.message : String(err);
    (self as unknown as Worker).postMessage({ ok: false, error });
  }
};
