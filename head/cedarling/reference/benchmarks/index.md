# Cedarling Benchmarks

Cross-platform authorization benchmarks for the Cedarling bindings, regenerated on pushes to `main` that change `jans-cedarling/**`, or on a manual `workflow_dispatch` run against `main`.

*Last updated: 2026-08-24 18:00 UTC (commit f89b5ff).*

## Provenance

- Each binding is measured on its own GitHub-hosted `ubuntu-latest` runner.
- Warm-up iterations per scenario: **100**; measured (sample count) per scenario: **1000**.
- Exact host, OS, and compiler/runtime versions for this run are recorded in the immutable workflow logs: <https://github.com/JanssenProject/jans/actions/runs/32758054630/attempts/1>.
- Scenario definitions and fixtures: `jans-cedarling/bindings/benchmarks/fixtures/scenarios.json`.

## Cross-Platform Binding Benchmarks

### Mean (µs) per scenario x binding

| Scenario                | c         | go        | java      | python    | rust      | wasm      |
| ----------------------- | --------- | --------- | --------- | --------- | --------- | --------- |
| multi_issuer_2_tokens   | 189.7     | 248.3     | 287.6     | 151.2     | 178.1     | 470.8     |
| multi_issuer_3_tokens   | 249.7     | 311.0     | 397.0     | 210.5     | 237.7     | 540.7     |
| multi_issuer_batch_10   | 1,179.4   | 1,496.5   | 1,524.2   | 1,052.5   | 1,157.2   | 2,801.1   |
| multi_issuer_batch_25   | 2,800.9   | 3,591.5   | 3,720.9   | 2,533.1   | 2,750.0   | 6,589.6   |
| multi_issuer_sig_status | *skipped* | *skipped* | *skipped* | *skipped* | *skipped* | *skipped* |
| unsigned_batch_10       | 287.6     | 479.4     | 574.4     | 257.8     | 282.4     | 1,069.9   |
| unsigned_batch_25       | 705.1     | 1,143.0   | 1,138.4   | 640.3     | 701.9     | 2,555.1   |
| unsigned_deep_json      | 120.4     | 182.8     | 359.8     | 107.8     | 114.7     | 329.0     |
| unsigned_simple         | 33.4      | 73.5      | 137.7     | 30.0      | 31.3      | 183.7     |

> **Note:** `unsigned_batch_*` and `multi_issuer_batch_*` measure the full batch call, not per-item latency.

### Relative speed per scenario (x, lower = faster)

*Ratios are computed from full-precision `mean_ns`, so they may not match dividing the rounded µs values shown above.*

| Scenario                | c           | go           | java             | python     | rust       | wasm             |
| ----------------------- | ----------- | ------------ | ---------------- | ---------- | ---------- | ---------------- |
| multi_issuer_2_tokens   | 1.25x ████  | 1.64x █████  | 1.90x ██████     | 1.00x ███  | 1.18x ████ | 3.11x ██████████ |
| multi_issuer_3_tokens   | 1.19x █████ | 1.48x ██████ | 1.89x ███████    | 1.00x ████ | 1.13x ████ | 2.57x ██████████ |
| multi_issuer_batch_10   | 1.12x ████  | 1.42x █████  | 1.45x █████      | 1.00x ████ | 1.10x ████ | 2.66x ██████████ |
| multi_issuer_batch_25   | 1.11x ████  | 1.42x █████  | 1.47x ██████     | 1.00x ████ | 1.09x ████ | 2.60x ██████████ |
| multi_issuer_sig_status | *skipped*   | *skipped*    | *skipped*        | *skipped*  | *skipped*  | *skipped*        |
| unsigned_batch_10       | 1.12x ███   | 1.86x ████   | 2.23x █████      | 1.00x ██   | 1.10x ███  | 4.15x ██████████ |
| unsigned_batch_25       | 1.10x ███   | 1.79x ████   | 1.78x ████       | 1.00x ███  | 1.10x ███  | 3.99x ██████████ |
| unsigned_deep_json      | 1.12x ███   | 1.70x █████  | 3.34x ██████████ | 1.00x ███  | 1.06x ███  | 3.05x █████████  |
| unsigned_simple         | 1.11x ██    | 2.45x ████   | 4.59x ███████    | 1.00x ██   | 1.04x ██   | 6.13x ██████████ |

*Latency columns (Mean, p50, p95, p99, Min, Max) in the detail tables below are microseconds (µs).*

### c detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 189.7     | 183.1    | 213.8    | 275.3    | 177.7    | 322.4    | —         | ok                            |
| multi_issuer_3_tokens   | 249.7     | 243.8    | 268.6    | 296.9    | 236.2    | 421.2    | —         | ok                            |
| multi_issuer_batch_10   | 1,179.4   | 1,172.5  | 1,212.2  | 1,291.6  | 1,153.3  | 1,845.4  | —         | ok                            |
| multi_issuer_batch_25   | 2,800.9   | 2,770.4  | 2,884.0  | 3,695.6  | 2,723.6  | 5,284.5  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 287.6     | 283.6    | 300.8    | 326.5    | 278.7    | 374.1    | —         | ok                            |
| unsigned_batch_25       | 705.1     | 700.9    | 729.2    | 798.3    | 680.3    | 1,211.3  | —         | ok                            |
| unsigned_deep_json      | 120.4     | 114.1    | 154.1    | 207.7    | 111.3    | 259.2    | —         | ok                            |
| unsigned_simple         | 33.4      | 32.2     | 43.6     | 58.0     | 31.5     | 117.5    | —         | ok                            |

### go detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 248.3     | 243.1    | 275.1    | 325.8    | 229.3    | 522.7    | 29.023    | ok                            |
| multi_issuer_3_tokens   | 311.0     | 302.4    | 334.8    | 371.1    | 289.0    | 1,160.7  | 31.082    | ok                            |
| multi_issuer_batch_10   | 1,496.5   | 1,484.4  | 1,552.4  | 1,875.9  | 1,436.2  | 2,789.2  | 356.259   | ok                            |
| multi_issuer_batch_25   | 3,591.5   | 3,505.9  | 3,685.7  | 4,170.3  | 3,411.4  | 20,140.6 | 881.513   | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 479.4     | 474.5    | 521.2    | 682.5    | 445.2    | 1,031.2  | 355.22    | ok                            |
| unsigned_batch_25       | 1,143.0   | 1,121.2  | 1,190.8  | 1,590.3  | 1,091.3  | 7,237.8  | 865.586   | ok                            |
| unsigned_deep_json      | 182.8     | 176.5    | 205.2    | 220.7    | 170.1    | 401.9    | 68.079    | ok                            |
| unsigned_simple         | 73.5      | 69.0     | 96.9     | 116.5    | 66.3     | 220.1    | 37.029    | ok                            |

### java detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 287.6     | 277.9    | 323.4    | 396.2    | 266.2    | 479.1    | —         | ok                            |
| multi_issuer_3_tokens   | 397.0     | 343.1    | 500.9    | 532.8    | 322.5    | 31,765.7 | —         | ok                            |
| multi_issuer_batch_10   | 1,524.2   | 1,518.7  | 1,584.6  | 1,643.2  | 1,464.0  | 2,035.7  | —         | ok                            |
| multi_issuer_batch_25   | 3,720.9   | 3,616.6  | 3,778.2  | 5,156.2  | 3,517.1  | 64,203.8 | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 574.4     | 509.1    | 697.3    | 764.5    | 461.1    | 47,341.4 | —         | ok                            |
| unsigned_batch_25       | 1,138.4   | 1,131.5  | 1,191.7  | 1,248.0  | 1,090.7  | 1,625.9  | —         | ok                            |
| unsigned_deep_json      | 359.8     | 259.3    | 371.5    | 429.6    | 192.4    | 60,201.5 | —         | ok                            |
| unsigned_simple         | 137.7     | 103.4    | 156.4    | 192.6    | 95.6     | 19,617.5 | —         | ok                            |

### python detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 151.2     | 147.9    | 171.0    | 187.0    | 138.4    | 246.2    | —         | ok                            |
| multi_issuer_3_tokens   | 210.5     | 208.1    | 231.7    | 253.9    | 194.4    | 281.4    | —         | ok                            |
| multi_issuer_batch_10   | 1,052.5   | 1,042.4  | 1,097.4  | 1,184.4  | 1,016.6  | 1,774.7  | —         | ok                            |
| multi_issuer_batch_25   | 2,533.1   | 2,504.4  | 2,652.4  | 3,291.3  | 2,425.6  | 4,898.1  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 257.8     | 252.4    | 275.6    | 299.0    | 247.2    | 384.8    | —         | ok                            |
| unsigned_batch_25       | 640.3     | 637.9    | 671.2    | 701.8    | 615.6    | 954.1    | —         | ok                            |
| unsigned_deep_json      | 107.8     | 103.6    | 129.1    | 166.9    | 98.8     | 213.8    | —         | ok                            |
| unsigned_simple         | 30.0      | 27.9     | 45.0     | 58.1     | 26.7     | 72.0     | —         | ok                            |

### rust detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 178.1     | 172.7    | 198.9    | 228.7    | 167.8    | 265.8    | —         | ok                            |
| multi_issuer_3_tokens   | 237.7     | 232.3    | 257.9    | 266.7    | 226.2    | 331.0    | —         | ok                            |
| multi_issuer_batch_10   | 1,157.2   | 1,150.0  | 1,189.5  | 1,278.6  | 1,130.8  | 1,509.0  | —         | ok                            |
| multi_issuer_batch_25   | 2,750.0   | 2,739.5  | 2,829.3  | 2,971.6  | 2,690.7  | 3,894.3  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 282.4     | 278.8    | 294.9    | 303.7    | 273.6    | 405.1    | —         | ok                            |
| unsigned_batch_25       | 701.9     | 702.9    | 712.9    | 741.4    | 680.8    | 828.3    | —         | ok                            |
| unsigned_deep_json      | 114.7     | 108.7    | 147.8    | 189.9    | 106.3    | 255.2    | —         | ok                            |
| unsigned_simple         | 31.3      | 30.7     | 32.5     | 44.1     | 30.1     | 52.3     | —         | ok                            |

### wasm detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 470.8     | 448.9    | 664.6    | 715.8    | 405.5    | 958.8    | —         | ok                            |
| multi_issuer_3_tokens   | 540.7     | 532.5    | 590.7    | 865.2    | 500.9    | 1,087.3  | —         | ok                            |
| multi_issuer_batch_10   | 2,801.1   | 2,694.1  | 3,638.1  | 4,239.4  | 2,602.2  | 4,671.7  | —         | ok                            |
| multi_issuer_batch_25   | 6,589.6   | 6,395.1  | 8,171.0  | 8,837.7  | 6,271.1  | 9,733.5  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 1,069.9   | 1,043.2  | 1,205.0  | 1,692.6  | 1,000.6  | 2,085.7  | —         | ok                            |
| unsigned_batch_25       | 2,555.1   | 2,462.5  | 3,219.4  | 4,020.5  | 2,380.9  | 7,862.4  | —         | ok                            |
| unsigned_deep_json      | 329.0     | 319.1    | 399.5    | 508.9    | 293.1    | 782.5    | —         | ok                            |
| unsigned_simple         | 183.7     | 162.2    | 266.1    | 358.8    | 143.8    | 799.2    | —         | ok                            |
