import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const bundleUrl = new URL(
  '../release/app/dist/renderer/renderer.dev.js',
  import.meta.url,
);
const bundle = await readFile(bundleUrl, 'utf8');
const sdkMarker = 'cedarling_js/dist/index.js":';
const markerIndex = bundle.indexOf(sdkMarker);

assert.notEqual(
  markerIndex,
  -1,
  'Development renderer bundle must contain the Cedarling browser entry',
);

const moduleStart = bundle.lastIndexOf('/***/', markerIndex);
const moduleEnd = bundle.indexOf('/***/ }),', markerIndex);
assert.notEqual(moduleStart, -1, 'Cedarling module start must be identifiable');
assert.notEqual(moduleEnd, -1, 'Cedarling module end must be identifiable');

const sdkModule = bundle.slice(moduleStart, moduleEnd);
assert.match(
  sdkModule,
  /__webpack_exports__/,
  'Development renderer must treat Cedarling as ESM',
);
assert.doesNotMatch(
  sdkModule,
  /Object\.defineProperty\(exports,\s*["']__esModule["']/,
  'Development renderer must not rewrite Cedarling ESM as CommonJS',
);

console.log('Development renderer uses unmodified Cedarling browser ESM.');
