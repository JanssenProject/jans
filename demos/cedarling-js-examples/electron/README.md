# Cedarling Electron example

This desktop TaskApp intentionally runs Cedarling in both processes. The
sandboxed renderer previews explicit unsigned permissions. Electron main owns
OIDC tokens, task state, signed authorization, and final enforcement.

The preload exposes explicit typed methods rather than a generic IPC invoke.
Main validates every IPC payload at runtime and constructs resources from its
own task store. A signed main-process session always takes precedence until
logout, so renderer input cannot downgrade enforcement.

OIDC uses DCR, Code + S256 PKCE, random state and nonce, a loopback callback,
system-browser login, requested-subject matching, and mandatory signed
UserInfo. Tokens never enter the renderer.

## Cedarling code tour

- `src/main/cedarling/*` owns signed authorization and the authoritative client.
- `src/main/ipc.ts` validates requests, constructs trusted resources, and
  enforces decisions before task mutation.
- `src/main/preload.ts` exposes a narrow typed bridge.
- `src/renderer/cedarling/init.ts` owns the independent browser-compatible
  client.
- `src/renderer/cedarling/permissions.ts` computes UI-only unsigned previews and
  delegates signed previews to main.
- `.erb/configs/webpack.config.*` show the different Node and browser WASM
  packaging requirements.

## Run and verify

Start the [shared IdP](../common/README.md), then:

```bash
npm run install:sdk:local
npm run lint
npm run typecheck
npm test -- --runInBand
npm run test:renderer-bundle
npm run test:renderer-dev-bundle
npm run build
npm start
```

`OIDC_ISSUER` defaults to `http://localhost:9090`. Remote issuers require HTTPS;
insecure OIDC client support is enabled only for loopback HTTP.

The build has one main configuration that emits both main and preload bundles,
plus focused renderer development and production configurations. It has no DLL,
bundle analyzer, Sass, SVG, font, or image pipeline because the application does
not use them.

The renderer uses context isolation, sandboxing, no Node integration, a strict
CSP, and denied navigation/window creation. Packaged builds disable DevTools.
OIDC URLs are opened only by validated main-process login code. Sessions and
tasks are in memory and reset when Electron exits.
