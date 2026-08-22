# Cedarling Benchmarks

Cross-platform authorization benchmarks for the Cedarling bindings, regenerated on pushes to `main` that change `jans-cedarling/**`, or on a manual `workflow_dispatch` run against `main`.

*Last updated: 2026-08-20 10:27 UTC (commit 539ef1e).*

## Provenance

- Each binding is measured on its own GitHub-hosted `ubuntu-latest` runner.
- Warm-up iterations per scenario: **100**; measured (sample count) per scenario: **1000**.
- Exact host, OS, and compiler/runtime versions for this run are recorded in the immutable workflow logs: <https://github.com/JanssenProject/jans/actions/runs/32357494739/attempts/1>.
- Scenario definitions and fixtures: `jans-cedarling/bindings/benchmarks/fixtures/scenarios.json`.

## Cross-Platform Binding Benchmarks

### Mean (µs) per scenario x binding

| Scenario                | c         | go        | java      | python    | rust      | wasm      |
| ----------------------- | --------- | --------- | --------- | --------- | --------- | --------- |
| multi_issuer_2_tokens   | 219.6     | 253.2     | 279.2     | 140.4     | 178.4     | 451.7     |
| multi_issuer_3_tokens   | 278.5     | 319.0     | 341.5     | 186.1     | 237.5     | 541.0     |
| multi_issuer_batch_10   | 1,392.3   | 1,544.0   | 1,515.8   | 906.8     | 1,144.4   | 2,891.1   |
| multi_issuer_batch_25   | 3,329.2   | 3,707.5   | 3,596.2   | 2,138.3   | 2,722.4   | 6,843.7   |
| multi_issuer_sig_status | *skipped* | *skipped* | *skipped* | *skipped* | *skipped* | *skipped* |
| unsigned_batch_10       | 375.2     | 499.4     | 487.3     | 219.8     | 279.5     | 1,182.3   |
| unsigned_batch_25       | 899.5     | 1,202.5   | 1,118.1   | 541.6     | 689.0     | 2,883.8   |
| unsigned_deep_json      | 147.0     | 187.9     | 280.0     | 90.4      | 111.6     | 335.6     |
| unsigned_simple         | 47.2      | 74.3      | 101.9     | 24.8      | 31.0      | 195.5     |

> **Note:** `unsigned_batch_*` and `multi_issuer_batch_*` measure the full batch call, not per-item latency.

### Relative speed per scenario (x, lower = faster)

| Scenario                | c           | go           | java           | python    | rust       | wasm             |
| ----------------------- | ----------- | ------------ | -------------- | --------- | ---------- | ---------------- |
| multi_issuer_2_tokens   | 1.56x █████ | 1.80x ██████ | 1.99x ██████   | 1.00x ███ | 1.27x ████ | 3.22x ██████████ |
| multi_issuer_3_tokens   | 1.50x █████ | 1.71x ██████ | 1.84x ██████   | 1.00x ███ | 1.28x ████ | 2.91x ██████████ |
| multi_issuer_batch_10   | 1.54x █████ | 1.70x █████  | 1.67x █████    | 1.00x ███ | 1.26x ████ | 3.19x ██████████ |
| multi_issuer_batch_25   | 1.56x █████ | 1.73x █████  | 1.68x █████    | 1.00x ███ | 1.27x ████ | 3.20x ██████████ |
| multi_issuer_sig_status | *skipped*   | *skipped*    | *skipped*      | *skipped* | *skipped*  | *skipped*        |
| unsigned_batch_10       | 1.71x ███   | 2.27x ████   | 2.22x ████     | 1.00x ██  | 1.27x ██   | 5.38x ██████████ |
| unsigned_batch_25       | 1.66x ███   | 2.22x ████   | 2.06x ████     | 1.00x ██  | 1.27x ██   | 5.32x ██████████ |
| unsigned_deep_json      | 1.63x ████  | 2.08x ██████ | 3.10x ████████ | 1.00x ███ | 1.23x ███  | 3.71x ██████████ |
| unsigned_simple         | 1.91x ██    | 3.00x ████   | 4.12x █████    | 1.00x █   | 1.25x ██   | 7.89x ██████████ |

*Latency columns (Mean, p50, p95, p99, Min, Max) in the detail tables below are microseconds (µs).*

### c detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 219.6     | 208.7    | 263.1    | 331.9    | 202.2    | 378.7    | —         | ok                            |
| multi_issuer_3_tokens   | 278.5     | 268.9    | 312.3    | 385.4    | 259.5    | 488.2    | —         | ok                            |
| multi_issuer_batch_10   | 1,392.3   | 1,388.1  | 1,438.5  | 1,495.5  | 1,355.5  | 1,692.1  | —         | ok                            |
| multi_issuer_batch_25   | 3,329.2   | 3,319.4  | 3,409.3  | 3,472.1  | 3,269.6  | 3,799.4  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 375.2     | 361.1    | 422.4    | 534.7    | 350.2    | 842.2    | —         | ok                            |
| unsigned_batch_25       | 899.5     | 895.4    | 932.2    | 992.5    | 857.4    | 1,155.6  | —         | ok                            |
| unsigned_deep_json      | 147.0     | 139.8    | 181.4    | 203.8    | 136.5    | 243.2    | —         | ok                            |
| unsigned_simple         | 47.2      | 45.5     | 62.8     | 73.9     | 44.3     | 96.8     | —         | ok                            |

### go detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 253.2     | 246.6    | 285.5    | 324.7    | 232.1    | 444.3    | 29.033    | ok                            |
| multi_issuer_3_tokens   | 319.0     | 310.7    | 350.6    | 411.4    | 292.7    | 771.3    | 31.096    | ok                            |
| multi_issuer_batch_10   | 1,544.0   | 1,528.3  | 1,638.0  | 1,891.7  | 1,462.1  | 2,717.4  | 356.207   | ok                            |
| multi_issuer_batch_25   | 3,707.5   | 3,621.0  | 3,895.1  | 4,482.4  | 3,497.7  | 22,728.5 | 881.602   | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 499.4     | 493.3    | 550.7    | 699.3    | 452.6    | 1,209.7  | 355.245   | ok                            |
| unsigned_batch_25       | 1,202.5   | 1,167.3  | 1,296.1  | 1,795.1  | 1,114.6  | 7,436.9  | 865.583   | ok                            |
| unsigned_deep_json      | 187.9     | 179.9    | 218.2    | 253.5    | 171.2    | 624.6    | 68.068    | ok                            |
| unsigned_simple         | 74.3      | 70.1     | 99.3     | 118.4    | 67.0     | 291.3    | 37.009    | ok                            |

### java detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 279.2     | 271.6    | 308.1    | 337.8    | 261.0    | 406.3    | —         | ok                            |
| multi_issuer_3_tokens   | 341.5     | 331.9    | 368.5    | 495.2    | 318.2    | 512.2    | —         | ok                            |
| multi_issuer_batch_10   | 1,515.8   | 1,509.3  | 1,581.9  | 1,659.2  | 1,457.0  | 1,880.6  | —         | ok                            |
| multi_issuer_batch_25   | 3,596.2   | 3,508.8  | 3,665.2  | 4,900.5  | 3,409.6  | 57,942.4 | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 487.3     | 484.2    | 530.4    | 675.3    | 450.3    | 838.5    | —         | ok                            |
| unsigned_batch_25       | 1,118.1   | 1,111.8  | 1,164.7  | 1,232.3  | 1,078.0  | 1,494.1  | —         | ok                            |
| unsigned_deep_json      | 280.0     | 216.4    | 329.2    | 350.9    | 188.3    | 43,308.3 | —         | ok                            |
| unsigned_simple         | 101.9     | 98.0     | 127.4    | 147.5    | 92.9     | 214.1    | —         | ok                            |

### python detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 140.4     | 136.9    | 157.0    | 175.8    | 132.8    | 206.5    | —         | ok                            |
| multi_issuer_3_tokens   | 186.1     | 182.3    | 202.1    | 209.7    | 176.8    | 248.5    | —         | ok                            |
| multi_issuer_batch_10   | 906.8     | 904.2    | 924.0    | 997.9    | 873.9    | 1,156.2  | —         | ok                            |
| multi_issuer_batch_25   | 2,138.3   | 2,131.8  | 2,196.7  | 2,245.1  | 2,104.5  | 2,343.2  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 219.8     | 217.1    | 230.6    | 237.0    | 212.8    | 280.7    | —         | ok                            |
| unsigned_batch_25       | 541.6     | 541.2    | 551.7    | 599.4    | 527.2    | 716.7    | —         | ok                            |
| unsigned_deep_json      | 90.4      | 88.3     | 103.1    | 113.0    | 86.5     | 161.8    | —         | ok                            |
| unsigned_simple         | 24.8      | 24.3     | 26.0     | 36.4     | 23.8     | 45.6     | —         | ok                            |

### rust detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 178.4     | 173.9    | 197.9    | 208.3    | 168.4    | 309.3    | —         | ok                            |
| multi_issuer_3_tokens   | 237.5     | 232.4    | 256.4    | 283.0    | 223.9    | 365.9    | —         | ok                            |
| multi_issuer_batch_10   | 1,144.4   | 1,139.6  | 1,168.0  | 1,250.0  | 1,118.7  | 1,415.3  | —         | ok                            |
| multi_issuer_batch_25   | 2,722.4   | 2,712.6  | 2,795.0  | 2,951.4  | 2,656.9  | 4,397.8  | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 279.5     | 275.5    | 292.3    | 329.4    | 269.8    | 459.8    | —         | ok                            |
| unsigned_batch_25       | 689.0     | 687.7    | 699.6    | 785.6    | 670.4    | 1,059.2  | —         | ok                            |
| unsigned_deep_json      | 111.6     | 108.9    | 125.5    | 134.0    | 106.3    | 352.8    | —         | ok                            |
| unsigned_simple         | 31.0      | 30.5     | 32.3     | 43.2     | 29.7     | 70.1     | —         | ok                            |

### wasm detail

| Scenario                | Mean (µs) | p50 (µs) | p95 (µs) | p99 (µs) | Min (µs) | Max (µs) | Allocs/op | Status                        |
| ----------------------- | --------- | -------- | -------- | -------- | -------- | -------- | --------- | ----------------------------- |
| multi_issuer_2_tokens   | 451.7     | 436.8    | 548.2    | 792.5    | 404.7    | 1,310.1  | —         | ok                            |
| multi_issuer_3_tokens   | 541.0     | 533.7    | 610.7    | 803.4    | 505.0    | 999.9    | —         | ok                            |
| multi_issuer_batch_10   | 2,891.1   | 2,761.5  | 3,591.1  | 4,368.0  | 2,683.1  | 6,552.0  | —         | ok                            |
| multi_issuer_batch_25   | 6,843.7   | 6,600.4  | 8,514.7  | 9,683.3  | 6,453.5  | 10,273.8 | —         | ok                            |
| multi_issuer_sig_status | —         | —        | —        | —        | —        | —        | —         | skipped (mock_op_unavailable) |
| unsigned_batch_10       | 1,182.3   | 1,159.8  | 1,277.6  | 1,917.2  | 1,122.6  | 2,152.5  | —         | ok                            |
| unsigned_batch_25       | 2,883.8   | 2,771.7  | 3,968.9  | 4,237.4  | 2,677.6  | 7,677.5  | —         | ok                            |
| unsigned_deep_json      | 335.6     | 319.8    | 481.3    | 561.5    | 297.1    | 690.0    | —         | ok                            |
| unsigned_simple         | 195.5     | 171.1    | 286.9    | 429.5    | 150.1    | 835.6    | —         | ok                            |
