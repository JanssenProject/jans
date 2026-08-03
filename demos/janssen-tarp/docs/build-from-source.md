# Building Janssen Tarp from Source

This tutorial walks through cloning the Janssen Project repository and building the Janssen Tarp browser extension yourself, instead of installing a pre-built release.

## 1. Prerequisites

- **Node.js** ≥ v18.15.0 (check with `node -v`)
- **npm** (bundled with Node.js)
- **git**

## 2. Clone the repository

Janssen Tarp lives inside the main `jans` monorepo, under `demos/janssen-tarp`.

```bash
git clone https://github.com/JanssenProject/jans.git
cd jans
```

If you need a specific branch (for example, a feature branch that hasn't merged yet):

```bash
git checkout jans-tarp-14557
```

Otherwise, stay on `main` for the latest stable source.

## 3. Install dependencies

The actual extension project is in the `browser-extension` subfolder:

```bash
cd demos/janssen-tarp/browser-extension
npm install
```

This pulls in the build tooling (Webpack, TypeScript, Tailwind CSS) and runtime dependencies (React, the Cedarling WASM bindings, the Anthropic and OpenAI SDKs used by the AI Agent tab, etc.), as declared in `package.json`.

## 4. Build the extension

```bash
npm run build
```

This runs Webpack in production mode and outputs two ready-to-load builds:

- `dist/chrome` — Chrome/Chromium build
- `dist/firefox` — Firefox build

**Other useful scripts:**

| Command | Purpose |
|---|---|
| `npm run build-dev` | Development-mode build (unminified, easier to debug) |
| `npm run watch` | Watches source files and rebuilds automatically on change (development config) |
| `npm run pack` | Packages the `dist` builds into distributable zip/xpi files under `release/` |

## 5. Load the unpacked extension into your browser

### Chrome

1. Open `chrome://extensions`.
2. Enable **Developer mode** (top right toggle).
3. Click **Load unpacked**.
4. Select the `demos/janssen-tarp/browser-extension/dist/chrome` folder.

### Firefox

Firefox requires a packed `.xpi` for the standard install flow, or a temporary load for development:

1. Open `about:debugging#/runtime/this-firefox`.
2. Click **Load Temporary Add-on...**.
3. Select any file inside `dist/firefox` (e.g. `manifest.json`).

> A temporary add-on is removed when Firefox restarts. For a persistent install, run `npm run pack` and install the generated `.xpi` from `release/` via `about:addons` (see the end-user tutorial for that flow).

## 6. Verify the build

Open the extension from your browser toolbar. You should see the **Authentication**, **Cedarling**, and **AI Agent** tabs — the same interface covered in the [end-user tutorial](../README.md). If the popup fails to load or shows a blank screen, check the browser's extension error console (`chrome://extensions` → the extension's **Errors** button, or Firefox's `about:debugging` → **Inspect**) for build/runtime errors.

## 7. Iterating on the source

If you're modifying the extension:

1. Run `npm run watch` to rebuild automatically on file changes.
2. In Chrome, click the reload icon on the extension card in `chrome://extensions` after each rebuild (Chrome doesn't auto-reload unpacked extensions).
3. In Firefox, click **Reload** next to the temporary add-on in `about:debugging`.

## 8. Troubleshooting

- **`npm install` fails on native/WASM deps** — ensure your Node.js version meets the ≥ 18.15.0 requirement; older versions can fail to install the Cedarling WASM package.
- **Build succeeds but extension icon is missing/broken** — re-run `npm run build` (not `build-dev`) for a clean production output, since `dist/` is cleaned on each build.
- **Changes not reflected in browser** — remember unpacked extensions must be manually reloaded after each rebuild (Chrome) or are removed on restart (Firefox temporary add-ons).
