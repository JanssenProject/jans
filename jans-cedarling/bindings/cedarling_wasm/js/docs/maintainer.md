# Maintaining the JavaScript package

This guide is for contributors who generate, build, validate, and release the
publishable `@janssenproject/cedarling_wasm` package. Application developers
should use the [consumer guide](../README.md); direct wasm-pack users should
use the [binding guide](../../README.md).

## Generated input and package output

Run this command from `bindings/cedarling_wasm` after changing an exported Rust
binding:

```sh
wasm-pack build --release --locked --target web --scope janssenproject
```

It compiles the Rust WebAssembly binding and emits the ignored `../pkg/` input:
the generated JavaScript glue, declarations, and WebAssembly binary. `js/`
contains the handwritten package assembly. Its build reads that input and emits
the ignored, publishable distribution:

```text
dist/{browser/,esm/,cjs/,edge/,manual/,types/{esm/,cjs/},wasm/cedarling_wasm_bg.wasm}
```

The package ships one raw WebAssembly asset. Browser, Node.js, CommonJS, edge,
and manual entry points load that same asset through their supported mechanism.
Never edit `pkg/` or `dist/`; regenerate them instead.

## Build and qualify

From `bindings/cedarling_wasm`, install the locked package dependencies and
browser engines:

```sh
cd js
npm ci --ignore-scripts
npx playwright install chromium firefox webkit
```

From `js/`, run:

| Command                  | Purpose                                                                                                                             |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| `npm run format`         | Format handwritten package source, scripts, tests, configuration, and documentation.                                                |
| `npm run format:check`   | Check formatting without changing files.                                                                                            |
| `npm run build`          | Assemble the shippable distribution from `../pkg/`.                                                                                 |
| `npm run package:verify` | Build, pack, type-check, and execute installed ESM/CommonJS consumers; qualify edge, automatic browser, and manual browser loading. |
| `npm run check`          | Check formatting, run runtime tests, and perform full packed-artifact verification.                                                 |

Prettier is pinned locally for consistent CLI and editor formatting. Generated
output and the npm-managed lockfile are excluded; embedded examples are preserved.

`package:verify` is the release-quality gate: it verifies the installed tarball,
not only the source checkout. The CI job regenerates `pkg/`, installs package
dependencies from the lockfile, installs Playwright engines, then runs this
same command.

## Public API boundary

The ESM-only `./manual` entry re-exports wasm-bindgen runtime values and
declarations directly. The root and `./edge` entries expose portable
initialization functions and generated types; the handwritten code adds only
portable WebAssembly loading for `init` and `initFromArchiveBytes`.

Callable JavaScript APIs use camelCase, including `jsonString()`. Generated
request JSON, bootstrap-property keys, result/data fields, and serialization
retain their canonical snake_case forms. `initWasm`, `initSync`, and `free()`
remain public generated APIs for compatibility and explicit resource control.

Use only package export-map paths in consumers: the root, `./edge`, `./manual`,
and `./wasm`. Files below `dist/` are package implementation details.
