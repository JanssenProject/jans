# trace_ingest_bench.sh

Runs the TRACE ingest benchmark (`TraceIngestBenchmark` in `lock-server/service`) against a live SQL database and prints records per second and latency for each number of parallel chains. Signed records go through the real ingest pipeline over `OrmTraceStore`. The benchmark uses a throw-away domain and deletes it afterwards.

```
trace_ingest_bench.sh [-c confdir] [-n records] [-k chains] [-p preload] [-m minRate] [-s maxSlowdown] [-o] [-l logfile] [-h]
```

| Option | Description | Default |
|---|---|---|
| `-c <dir>` | Database profile folder that contains `jans-sql.properties` and `salt`. | `$LOCK_BENCH_PROFILE`, else `jans-orm/integration-test/profiles/jans-dev.jans.info/conf` |
| `-n <num>` | Records ingested per chain. | `1000` |
| `-k <list>` | Comma-separated numbers of parallel chains, one scenario each, for example `1,4`. | `1,4` |
| `-p <num>` | Committed receipts inserted into the domain before the run. Shows work that grows with the number of receipts. Run `ANALYZE` on the receipt table after a big preload. | `0` |
| `-m <rate>` | Fail when the one-chain rate is lower than `<rate>` records/s. | off |
| `-s <ratio>` | Fail when the last quarter of a one-chain run is more than `<ratio>` times slower than the first quarter. | off |
| `-o` | Run Maven offline. | off |
| `-l <file>` | Keep the full Maven output in `<file>`. | temporary file |
| `-h` | Show help and exit. | |

Example:

```
./trace_ingest_bench.sh -o -n 500 -k 1 -p 30000
```

Results are also appended to `service/target/benchmark/trace-ingest.csv`.
