# Cedarling Electron example

This minimal Electron + React application demonstrates Cedarling authorization
in both Electron security contexts:

- Main owns OIDC tokens, task state, signed authorization, and final
  enforcement.
- The sandboxed renderer runs a separate browser-compatible Cedarling client
  for unsigned UI permission previews.
- Preload exposes only typed task, session, and authorization methods. It never
  exposes `ipcRenderer` or Node.js APIs.

The login uses dynamic client registration, Authorization Code + PKCE, state
and nonce validation, a loopback callback, the system browser, and signed
UserInfo. Tokens remain in main memory and are never sent to React.

## Project structure

The application follows electron-vite's standard discovery layout:

```text
src/
├── main/
│   ├── cedarling/       # authoritative Cedarling client
│   ├── index.ts         # Electron lifecycle and secure BrowserWindow
│   ├── ipc.ts           # validated IPC and authorization enforcement
│   └── oidc.ts          # external IdP login
├── preload/
│   └── index.ts         # narrow context-isolated bridge
├── renderer/
│   ├── index.html
│   └── src/             # React UI and renderer Cedarling client
└── shared/
    └── contracts.ts     # IPC types and validation limits
```

`electron.vite.config.ts` uses electron-vite's default main, preload, renderer,
and `out/` locations. Its only build-specific settings enable React, relative
renderer assets, full preload bundling required by Electron sandboxing, and
preservation of the generated SDK-to-WASM relative URL during development.
There is no application packager because this example only needs development,
build, and production-preview commands.

## Run with repository packages

Until `@janssenproject/cedarling` and its WASM dependency are published, use the
repository launcher from `demos/cedarling-js-examples`:

```bash
npm --prefix electron run start:docker
```

Docker builds `cedarling_wasm` and `cedarling_js` from the current checkout,
exports coordinated npm tarballs, and starts the external IdP on host loopback.
The launcher installs the application and local tarballs on the host, starts
electron-vite's native production preview (which builds first), and
cleans up the IdP and temporary artifacts after Electron exits. Docker never
receives the host display, Docker socket, or source directory as a mount.

After both packages are published, replace the temporary tarball installation
with a normal reproducible `npm ci`. The native `npm start` command and the
external IdP remain unchanged.

## Develop manually

Start the shared external IdP in one terminal with the renderer development
origin allowed by CORS:

```bash
cd ../common
FRONTEND_ORIGIN=http://localhost:5173 npm start
```

Then run the Electron development process:

```bash
npm run install:sdk:local
npm run dev
```

For an explicit production build, then a native preview:

```bash
npm run build
npm start
```

`electron-vite preview` rebuilds before launch by default, so `npm start` is
also sufficient when a separate build-verification step is unnecessary.

`OIDC_ISSUER` defaults to `http://localhost:9090`. Remote issuers must use
HTTPS; insecure HTTP is accepted only on loopback. The renderer uses context
isolation, sandboxing, no Node integration, denied permissions, denied new
windows and navigation, and a runtime Content Security Policy limited to self
and the validated issuer. Development alone permits Vite's inline React-refresh
and CSS-HMR injections; the built application has no inline-script or
inline-style allowance. Both Cedarling clients fail closed without retrying
through the WASM timer that Electron cannot provide. Production preview
disables DevTools.

## Verify

```bash
npm run lint
npm run typecheck
npm test -- --runInBand
npm run test:docker-workflow
npm run test:build
```

The build verifier checks the default electron-vite main, preload, and renderer
outputs and confirms that the renderer emits and references Cedarling WASM.
