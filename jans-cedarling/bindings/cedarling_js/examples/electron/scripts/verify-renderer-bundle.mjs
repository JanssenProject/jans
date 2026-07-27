import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const bundleUrl = new URL(
  '../release/app/dist/renderer/renderer.js',
  import.meta.url,
);
const sourceMapUrl = new URL(
  '../release/app/dist/renderer/renderer.js.map',
  import.meta.url,
);
const bundle = await readFile(bundleUrl, 'utf8');
const sourceMap = JSON.parse(await readFile(sourceMapUrl, 'utf8'));
const sources = Array.isArray(sourceMap.sources) ? sourceMap.sources : [];

assert.equal(
  sources.some((source) => source.includes('/cedarling_js/dist/cjs/')),
  false,
  'Electron renderer bundle must not resolve the Cedarling CommonJS entry',
);
assert.equal(
  sources.some((source) => source.includes('/cedarling_js/dist/entries/node')),
  false,
  'Electron renderer bundle must not resolve the Cedarling Node entry',
);
assert.equal(
  sources.some((source) => (
    source.endsWith('/cedarling_js/dist/index.js')
    || source.endsWith('/cedarling_js/src/index.ts')
  )),
  true,
  'Electron renderer bundle must resolve the Cedarling browser entry',
);
assert.doesNotMatch(
  bundle,
  /Object\.defineProperty\(exports,\s*["']__esModule["']/,
  'Electron renderer bundle must not contain TypeScript-injected free exports',
);

console.log('Electron renderer bundle uses unmodified Cedarling browser ESM.');
