// Check if the renderer and main bundles are built
import path from 'path';
import chalk from 'chalk';
import fs from 'fs';
import { TextEncoder, TextDecoder } from 'node:util';
import webpackPaths from '../configs/webpack.paths';

const mainPath = path.join(webpackPaths.distMainPath, 'main.js');
const rendererPath = path.join(webpackPaths.distRendererPath, 'renderer.js');

if (!fs.existsSync(mainPath)) {
  throw new Error(
    chalk.whiteBright.bgRed.bold(
      'The main process is not built yet. Build it by running "npm run build:main"',
    ),
  );
}

if (!fs.existsSync(rendererPath)) {
  throw new Error(
    chalk.whiteBright.bgRed.bold(
      'The renderer process is not built yet. Build it by running "npm run build:renderer"',
    ),
  );
}

// JSDOM does not implement TextEncoder and TextDecoder
if (!global.TextEncoder) {
  // Node's implementation is Web API compatible, but its declaration permits
  // Uint8Array values backed by ArrayBufferLike instead of only ArrayBuffer.
  global.TextEncoder =
    TextEncoder as unknown as typeof global.TextEncoder;
}
if (!global.TextDecoder) {
  global.TextDecoder =
    TextDecoder as unknown as typeof global.TextDecoder;
}
