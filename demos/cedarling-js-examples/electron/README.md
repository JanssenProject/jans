# Cedarling Electron example

This desktop TaskApp demonstrates Cedarling in both Electron processes while
keeping OIDC tokens and signed authorization inside the trusted main process.
The UI follows the React reference application but uses a desktop window,
native application menu, and Electron process boundaries.

## What it demonstrates

- `@janssenproject/cedarling` initialized directly in the sandboxed renderer
  for unsigned UI permission checks
- a separate Cedarling client in the main process for signed checks and
  enforcement of task mutations
- a narrow, typed `contextBridge` API instead of renderer Node.js access
- OIDC discovery, Dynamic Client Registration, Authorization Code + PKCE,
  state, nonce, a loopback callback, and system-browser login
- tokens retained in main-process memory rather than renderer storage
- production packaging of Cedarling and the shared policy/configuration files

## Architecture

```text
+-----------------------------+       contextBridge / IPC       +-----------------------------+
| Renderer                    | <------------------------------> | Main process                |
| React UI                    |                                  | Task store + enforcement    |
| Cedarling browser adapter   |                                  | Cedarling Node adapter      |
| unsigned permission checks  |                                  | signed token authorization  |
+-----------------------------+                                  | DCR + Code/PKCE session     |
                                                                 +---------------+-------------+
                                                                                 |
                                                                                 v
                                                                 +-----------------------------+
                                                                 | Shared development IdP      |
                                                                 | http://localhost:9090       |
                                                                 +-----------------------------+
```

The renderer never receives OIDC tokens. IPC returns only the session state and
authorization/task results needed by the UI. `contextIsolation`, renderer
sandboxing, and disabled Node integration remain enabled.

## Requirements

- Node.js 20.19 or newer
- npm 10 or newer
- the [shared development IdP](../common/README.md) running on port `9090`
- published `@janssenproject/cedarling@1.0.0` and
  `@janssenproject/cedarling_wasm@1.0.0` packages, or the local staged-package workflow
  described in the [examples overview](../README.md#sdk-package-availability)

## Run

Start the shared IdP first:

```bash
cd ../common
npm install
npm start
```

Then start Electron:

```bash
cd ../electron
npm run install:sdk:local
npm start
```

The development command uses Electron React Boilerplate's renderer server,
main/preload watchers, and `electronmon`.

### Linux development sandbox note

The local development launcher passes Electron `--no-sandbox` because an
unprivileged source checkout normally cannot install Electron's
`chrome-sandbox` helper as root with mode `4755`. This flag applies only to the
development Electron process. The application window still requests renderer
sandboxing, and packaged builds do not add the command-line flag.

## Exercise authorization

1. Use unsigned mode and switch between Bob, Alice, and Charlie.
2. Create, complete, and delete tasks; main-process authorization is the final
   enforcement boundary.
3. Switch to signed mode and choose a user.
4. Complete the local IdP login in the system browser with the selected
   username and any password.
5. Return to the app and repeat the task operations using the signed session.

## Project structure

```text
src/
  main/
    main.ts                 window lifecycle and navigation hardening
    preload.ts              contextBridge API
    ipc.ts                  task, authorization, and OIDC handlers
    tasks.ts                in-memory task store
    cedarling/              main-process Cedarling adapter
  renderer/
    App.tsx                 desktop TaskApp UI
    cedarling/              renderer Cedarling adapter and checks
scripts/
  verify-renderer-*.mjs     production/development bundle guards
release/app/                packaged runtime manifest
```

## Verify

```bash
npm test -- --runInBand
npm run typecheck
npm run test:renderer-bundle
npm run test:renderer-dev-bundle
npm run build
```

Build a distributable package with:

```bash
npm run package
```

## Known limitations

> [!NOTE]
> Manual renderer DevTools toggling is deferred. The native menu is preserved
> and DevTools do not open automatically, but the menu action and the usual
> Ctrl/Cmd+Shift+I or F12 shortcuts are currently unreliable in this example.
> Main-process logs remain visible in the terminal; renderer-console access
> requires resolving this follow-up.

The IdP, sessions, clients, and tasks are development-only in-memory fixtures.
Restarting either process clears the corresponding state.
