# Cedarling Benchmarks

Cross-platform authorization benchmarks for the Cedarling bindings, regenerated on pushes to `main` that change `jans-cedarling/**`, or on a manual `workflow_dispatch` run against `main`.

*Last updated: 2026-08-19 17:57 UTC (commit 3ab88c0).*

## Provenance

- Each binding is measured on its own GitHub-hosted `ubuntu-latest` runner.
- Warm-up iterations per scenario: **100**; measured (sample count) per scenario: **1000**.
- Exact host, OS, and compiler/runtime versions for this run are recorded in the immutable workflow logs: <https://github.com/JanssenProject/jans/actions/runs/32283061270/attempts/1>.
- Scenario definitions and fixtures: `jans-cedarling/bindings/benchmarks/fixtures/scenarios.json`.

## Cross-Platform Binding Benchmarks

### Mean (µs) per scenario x binding

| Scenario                | c         | go        | java      | python    | rust      | wasm      |
| ----------------------- | --------- | --------- | --------- | --------- | --------- | --------- |
| multi_issuer_2_tokens   | 193.0     | 242.4     | 293.8     | 184.7     | 180.7     | 360.7     |
| multi_issuer_3_tokens   | 305.1     | 304.7     | 323.3     | 241.5     | 242.0     | 422.8     |
| multi_issuer_batch_10   | 1,181.8   | 1,346.0   | 1,328.3   | 1,176.6   | 1,169.5   | 2,133.8   |
| multi_issuer_batch_25   | 2,820.5   | 3,123.5   | 3,037.6   | 2,762.8   | 2,789.2   | 5,081.7   |
| multi_issuer_sig_status | *skipped* | *skipped* | *skipped* | *skipped* | *skipped* | *skipped* |
| unsigned_batch_10       | 300.1     | 388.6     | 395.6     | 278.4     | 276.2     | 857.8     |
| unsigned_batch_25       | 715.5     | 939.9     | 989.6     | 688.1     | 695.8     | 2,028.9   |
| unsigned_deep_json      | 116.8     | 165.1     | 206.8     | 117.1     | 112.8     | 258.6     |
| unsigned_simple         | 32.9      | 50.6      | 82.5      | 31.7      | 31.1      | 139.9     |

> **Note:** `unsigned_batch_*` and `multi_issuer_batch_*` measure the full batch call, not per-item latency.

### Relative speed per scenario (x, lower = faster)

| Scenario                | c             | go            | java           | python       | rust         | wasm             |
| ----------------------- | ------------- | ------------- | -------------- | ------------ | ------------ | ---------------- |
| multi_issuer_2_tokens   | 1.07x █████   | 1.34x ███████ | 1.63x ████████ | 1.02x █████  | 1.00x █████  | 2.00x ██████████ |
| multi_issuer_3_tokens   | 1.26x ███████ | 1.26x ███████ | 1.34x ████████ | 1.00x ██████ | 1.00x ██████ | 1.75x ██████████ |
| multi_issuer_batch_10   | 1.01x ██████  | 1.15x ██████  | 1.14x ██████   | 1.01x ██████ | 1.00x █████  | 1.82x ██████████ |
| multi_issuer_batch_25   | 1.02x ██████  | 1.13x ██████  | 1.10x ██████   | 1.00x █████  | 1.01x █████  | 1.84x ██████████ |
| multi_issuer_sig_status | *skipped*     | *skipped*     | *skipped*      | *skipped*    | *skipped*    | *skipped*        |
| unsigned_batch_10       | 1.09x ███     | 1.41x █████   | 1.43x █████    | 1.01x ███    | 1.00x ███    | 3.11x ██████████ |
| unsigned_batch_25       | 1.04x ████    | 1.37x █████   | 1.44x █████    | 1.00x ███    | 1.01x ███    | 2.95x ██████████ |
| unsigned_deep_json      | 1.04x █████   | 1.46x ██████  | 1.83x ████████ | 1.04x █████  | 1.00x ████   | 2.29x ██████████ |
| unsigned_simple         | 1.06x ██      | 1.63x ████    | 2.65x ██████   | 1.02x ██     | 1.00x ██     | 4.49x ██████████ |

*Latency columns (Mean, p50, p95, p99, Min, Max) in the detail tables below are microseconds (µs).*

### c detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 193.0     | 183.1    | 245.3    | 288.9    | 176.9    | 389.2    | —         | ok                            |
| multi_issuer_3_tokens   | 305.1     | 277.6    | 395.0    | 407.1    | 234.3    | 447.4    | —         | ok                            |
| multi_issuer_batch_10   | 1,181.8   | 1,176.2  | 1,209.6  | 1,300.4  | 1,156.6  | 1,732.5  | —         | ok                            |
| multi_issuer_batch_25   | 2,820.5   | 2,801.9  | 2,901.4  | 3,105.9  | 2,754.4  | 4,553.0  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 300.1     | 285.3    | 426.3    | 469.9    | 279.5    | 515.8    | —         | ok                            |
| unsigned_batch_25       | 715.5     | 709.8    | 738.2    | 832.7    | 687.5    | 1,509.0  | —         | ok                            |
| unsigned_deep_json      | 116.8     | 113.8    | 132.5    | 152.1    | 111.1    | 184.3    | —         | ok                            |
| unsigned_simple         | 32.9      | 32.1     | 35.1     | 52.8     | 31.3     | 74.5     | —         | ok                            |

### go detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 242.4     | 231.1    | 306.0    | 405.3    | 212.6    | 775.7    | 29.06     | ok                            |
| multi_issuer_3_tokens   | 304.7     | 294.8    | 361.5    | 478.2    | 272.6    | 1,260.1  | 31.079    | ok                            |
| multi_issuer_batch_10   | 1,346.0   | 1,332.4  | 1,429.0  | 1,673.3  | 1,275.3  | 2,574.8  | 356.256   | ok                            |
| multi_issuer_batch_25   | 3,123.5   | 3,089.7  | 3,305.5  | 3,721.1  | 2,967.1  | 10,411.1 | 881.746   | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 388.6     | 381.3    | 429.8    | 576.6    | 354.5    | 926.1    | 355.248   | ok                            |
| unsigned_batch_25       | 939.9     | 915.6    | 1,027.0  | 1,474.6  | 875.2    | 5,078.4  | 865.576   | ok                            |
| unsigned_deep_json      | 165.1     | 153.4    | 227.1    | 324.0    | 142.1    | 394.1    | 68.048    | ok                            |
| unsigned_simple         | 50.6      | 47.0     | 68.7     | 102.1    | 44.7     | 239.5    | 37.03     | ok                            |

### java detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 293.8     | 255.6    | 387.3    | 467.8    | 242.8    | 20,888.9 | —         | ok                            |
| multi_issuer_3_tokens   | 323.3     | 318.0    | 346.4    | 368.1    | 303.6    | 427.5    | —         | ok                            |
| multi_issuer_batch_10   | 1,328.3   | 1,320.8  | 1,380.5  | 1,454.5  | 1,265.0  | 1,812.5  | —         | ok                            |
| multi_issuer_batch_25   | 3,037.6   | 3,018.1  | 3,138.2  | 3,473.0  | 2,958.6  | 4,281.3  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 395.6     | 391.4    | 425.1    | 474.8    | 367.2    | 716.8    | —         | ok                            |
| unsigned_batch_25       | 989.6     | 905.0    | 1,275.5  | 1,416.8  | 853.9    | 54,563.2 | —         | ok                            |
| unsigned_deep_json      | 206.8     | 177.8    | 277.9    | 318.5    | 160.5    | 19,105.7 | —         | ok                            |
| unsigned_simple         | 82.5      | 77.4     | 104.4    | 169.9    | 72.7     | 193.2    | —         | ok                            |

### python detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 184.7     | 178.1    | 210.6    | 246.1    | 171.9    | 284.9    | —         | ok                            |
| multi_issuer_3_tokens   | 241.5     | 235.7    | 264.1    | 286.5    | 228.5    | 325.3    | —         | ok                            |
| multi_issuer_batch_10   | 1,176.6   | 1,166.7  | 1,245.7  | 1,292.2  | 1,142.5  | 1,454.5  | —         | ok                            |
| multi_issuer_batch_25   | 2,762.8   | 2,746.5  | 2,857.8  | 2,939.4  | 2,692.3  | 3,553.7  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 278.4     | 274.1    | 292.4    | 319.3    | 269.1    | 370.5    | —         | ok                            |
| unsigned_batch_25       | 688.1     | 687.1    | 711.0    | 761.1    | 666.0    | 879.2    | —         | ok                            |
| unsigned_deep_json      | 117.1     | 113.3    | 134.8    | 161.6    | 110.9    | 168.4    | —         | ok                            |
| unsigned_simple         | 31.7      | 31.0     | 34.6     | 46.1     | 30.3     | 74.7     | —         | ok                            |

### rust detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 180.7     | 174.8    | 202.4    | 233.6    | 169.8    | 327.1    | —         | ok                            |
| multi_issuer_3_tokens   | 242.0     | 235.5    | 263.3    | 299.1    | 228.2    | 348.3    | —         | ok                            |
| multi_issuer_batch_10   | 1,169.5   | 1,159.2  | 1,213.6  | 1,323.9  | 1,138.1  | 2,125.7  | —         | ok                            |
| multi_issuer_batch_25   | 2,789.2   | 2,779.7  | 2,871.3  | 2,972.0  | 2,720.0  | 3,138.3  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 276.2     | 272.6    | 287.6    | 305.5    | 268.2    | 382.2    | —         | ok                            |
| unsigned_batch_25       | 695.8     | 694.5    | 719.5    | 771.7    | 676.3    | 1,018.6  | —         | ok                            |
| unsigned_deep_json      | 112.8     | 110.3    | 126.9    | 142.2    | 107.8    | 172.4    | —         | ok                            |
| unsigned_simple         | 31.1      | 30.7     | 32.3     | 42.8     | 30.0     | 68.8     | —         | ok                            |

### wasm detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 360.7     | 342.1    | 501.1    | 550.7    | 314.9    | 1,513.1  | —         | ok                            |
| multi_issuer_3_tokens   | 422.8     | 410.5    | 514.3    | 662.0    | 389.0    | 952.8    | —         | ok                            |
| multi_issuer_batch_10   | 2,133.8   | 2,053.8  | 2,690.0  | 3,518.7  | 1,996.1  | 3,956.5  | —         | ok                            |
| multi_issuer_batch_25   | 5,081.7   | 4,909.5  | 6,538.3  | 6,814.9  | 4,763.0  | 7,774.8  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 857.8     | 812.9    | 1,091.8  | 1,773.1  | 763.6    | 2,699.0  | —         | ok                            |
| unsigned_batch_25       | 2,028.9   | 1,905.4  | 2,703.1  | 3,564.0  | 1,826.6  | 7,649.7  | —         | ok                            |
| unsigned_deep_json      | 258.6     | 245.2    | 368.3    | 401.8    | 223.6    | 586.3    | —         | ok                            |
| unsigned_simple         | 139.9     | 124.6    | 201.8    | 322.6    | 111.4    | 788.2    | —         | ok                            |
