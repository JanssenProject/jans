# JavaScript package maintenance

`js/` is the hand-written package. `../pkg/` is ignored output from:

```sh
wasm-pack build --release --locked --target web --scope janssenproject
```

It is the only generated input. `.build/` and `dist/` are ignored outputs.

```text
dist/{browser/,esm/,cjs/,edge/,manual/,types/{esm/,cjs/},wasm/cedarling_wasm_bg.wasm}
```

The final npm tarball contains one raw WASM file. Browser, Node, and edge
entries resolve it with their runtime-specific loading strategy.

## Public API boundary

The package preserves wasm-bindgen client and result classes directly. It adds
only portable runtime initialization:

- `init(...)` and `initFromArchiveBytes(...)` initialize automatically.
- The default `initWasm(input?)`, named `initWasm(input?)`, and
  `initSync(input)` exports remain available for generated-package
  compatibility in browser and Node entries.
- The edge entry supports automatic initialization and no-argument
  `initWasm()`; caller-supplied initialization input would violate static-WASM
  edge deployment requirements.
- `free()`, generated result classes, snake_case result helpers and data,
  inputs, errors, and Cedarling-defined data remain generated behavior.
- `./manual` and `./wasm` are the stable ESM-only escape hatch for bundlers
  that need to emit the one packaged WASM file themselves.

## Browser-test prerequisite

Install the three Playwright browser engines once before package verification:

```sh
npx playwright install chromium firefox webkit
```

## Commands

| Command | Purpose |
| --- | --- |
| `npm run build` | Builds the shippable distribution from `../pkg/`. |
| `npm run package:verify` | Builds, packs, type-checks and executes installed ESM/CommonJS consumers, bundles edge loading, and runs automatic and manual packed browser consumers in Chromium, Firefox, and WebKit. |
| `npm run check` | Runs focused runtime tests followed by package verification. |

Regenerate `../pkg/` after changing exported Rust bindings. Keep it ignored:
the published package is assembled solely from `dist/`.
