# Cedarling Cross-Platform Benchmarks

Single source of truth for the cross-platform Cedarling bench contract.

| File | Purpose |
|---|---|
| [`CONTRACT.md`](./CONTRACT.md) | Fixture + output schema, iteration policy, skip semantics. |
| [`fixtures/scenarios.json`](./fixtures/scenarios.json) | The five canonical scenarios every binding runs. |

Every binding loads the same manifest, runs the same iteration policy (100 warmup / 1000 measure), and emits JSONL to stdout. The summary script [`scripts/parse_binding_benchmarks.py`](../../scripts/parse_binding_benchmarks.py) pivots one or more output files into a cross-binding comparison table.

## Running locally

| Binding | Cd to | Invocation |
|---|---|---|
| **Java / UniFFI** | `bindings/cedarling_uniffi/javaApp` | `mvn -q compile exec:java -Dexec.mainClass=org.example.Benchmark` |
| **Go** | `bindings/cedarling_go` | `LD_LIBRARY_PATH="$(pwd)" go run ./benchmarks/` |
| **Python** | `bindings/cedarling_python` | `tox -e benchmark` *(or)* `python3 benchmarks/benchmark_cedarling.py` |
| **WASM** | `bindings/cedarling_wasm` | `node benchmarks/benchmark.mjs` *(after `wasm-pack build --target nodejs --release`)* |
| **C** | `bindings/cedarling_c/benchmarks` | `make && ./benchmark_cedarling` |

All harnesses honour `CEDARLING_REPO_ROOT` for resolving the manifest and policy stores; default is three levels up from the bench script.

### Cross-binding pivot

```sh
python3 scripts/parse_binding_benchmarks.py --format jsonl \
    java.jsonl go.jsonl python.jsonl wasm.jsonl c.jsonl
```

## Per-binding notes

- **C** uses `gen_scenarios.py` (run by the Makefile) to pre-bake the manifest into a generated header — avoids a runtime JSON parser dep.
- **WASM** runs a Python subprocess to convert each scenario's YAML policy store into the JSON the WASM binding accepts inline.
- **Go** is the only binding currently reporting a real `allocs_per_op`, measured via `runtime.MemStats` diff outside the timed loop. The other four emit `null` — language-native allocs counters are a follow-up subtask.
- **`multi_issuer_sig_status`** needs a per-binding mock OP; until each binding wires one up it emits `{"status":"skipped","reason":"mock_op_unavailable"}`.
