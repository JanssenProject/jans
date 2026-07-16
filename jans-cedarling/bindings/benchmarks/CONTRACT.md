# Cedarling Cross-Platform Benchmark Contract

Single source of truth for how every binding bench (Java/UniFFI, Go, Python, WASM, C) must behave so the results are directly comparable.

## Fixtures (`fixtures/scenarios.json`)

Every binding loads the same manifest and dispatches scenarios by `id`. No inline JWT, principal, action, resource or context literals in the bench code.

| `id` | `kind` | tokens | sig | status | purpose |
|---|---|---|---|---|---|
| `unsigned_simple` | unsigned | 0 | off | off | baseline |
| `unsigned_deep_json` | unsigned | 0 | off | off | nested context — exercises `build_context` / `Value` clone |
| `multi_issuer_2_tokens` | multi_issuer | 2 | off | off | matches criterion `authz_authorize_multi_issuer` |
| `multi_issuer_3_tokens` | multi_issuer | 3 | off | off | matches the prior Java/Python/WASM/C bench shape |
| `multi_issuer_sig_status` | multi_issuer | runtime-generated | on | on | JWT decode + sig verify + status-list + mock OP |
| `unsigned_batch_10` | unsigned_batch | 0 | off | off | batch API, N=10 items; **`mean_ns` is per whole batch call** |
| `unsigned_batch_25` | unsigned_batch | 0 | off | off | batch API, N=25 items; per-batch timing |
| `multi_issuer_batch_10` | multi_issuer_batch | 2 | off | off | batch API, N=10 items; per-batch timing |
| `multi_issuer_batch_25` | multi_issuer_batch | 2 | off | off | batch API, N=25 items; per-batch timing |

`multi_issuer_sig_status` requires a per-binding mock OP. Bindings without one emit `{"status":"skipped","reason":"mock_op_unavailable"}`.

### Batch scenarios

`unsigned_batch` / `multi_issuer_batch` reuse the single-item fixture shape (`principal` or `tokens`, one `action`, one `resource`, one `context`) plus an `item_count: N` field. On every iteration the harness constructs a batch request from the shared principal / tokens and **N `BatchItem`s**, each with the fixture's `action` and `context` and the fixture's `resource` cloned with the entity id suffixed `-0..-N-1`. `mean_ns` measures the whole batch call — divide by `item_count` for a per-item comparison against the single-item scenarios.

Divide-by-N against `unsigned_simple` / `multi_issuer_2_tokens` gives the setup-amortization delta: the per-item batch cost should be lower than the corresponding single-item cost, with the gap widening at larger N.

### Fixture schema

```jsonc
{
  "_schema_version": 1,
  "iteration_policy": { "warmup_iters": 100, "measure_iters": 1000 },
  "scenarios": [
    {
      "id": "unsigned_simple",
      "kind": "unsigned" | "multi_issuer" | "unsigned_batch" | "multi_issuer_batch",
      "item_count": 10,                                  // batch kinds only; ignored otherwise
      "policy_store_fn": "test_files/...yaml",
      "config_overrides": { "CEDARLING_...": <string | array | bool | number> },
      "principal": { ... } | null,
      "action": "Jans::Action::\"...\"",
      "resource": { ... },
      "context": "{...}",
      "tokens": [ { "mapping": "...", "payload": "..." } ],
      "mock_op_required": false
    }
  ]
}
```

## Iteration policy

`warmup_iters = 100`, `measure_iters = 1000` in every binding; read from the manifest's `iteration_policy`, not hardcoded.

## Output schema — JSON Lines

One JSON object per scenario × binding, one per line on stdout. Example:

```json
{"binding":"java","scenario":"unsigned_simple","iter":1000,"mean_ns":441200,"p50_ns":124500,"p95_ns":225100,"p99_ns":302500,"min_ns":114700,"max_ns":21538900,"allocs_per_op":null,"status":"ok"}
```

### Required fields

| field | type | notes |
|---|---|---|
| `binding` | string | `"java"`, `"go"`, `"python"`, `"wasm"`, `"c"` |
| `scenario` | string | matches `id` from the manifest |
| `iter` | number | `measure_iters` executed |
| `mean_ns` / `p50_ns` / `p95_ns` / `p99_ns` / `min_ns` / `max_ns` | number | wall-clock per call, nanoseconds |
| `allocs_per_op` | number \| null | Go reports `runtime.MemStats` diff; others emit `null` (language-native allocs are a follow-up) |
| `status` | string | `"ok"` or `"skipped"` |
| `reason` | string | required when `status == "skipped"` |

Integer nanoseconds keeps the unit uniform; JSON Lines means one crashing scenario doesn't poison the file.

## Reproducing locally

Each binding's README links here. Generic shape:

```sh
# 1. Build the binding (see its own README)
# 2. Run its bench, capturing JSONL
... > out.jsonl
# 3. Render the cross-binding summary
python3 scripts/parse_binding_benchmarks.py --format jsonl out.jsonl [more.jsonl ...]
```
