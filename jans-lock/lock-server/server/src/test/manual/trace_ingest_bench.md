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

## Per-call store timing

`-Dlock.benchmark.timing=true` wraps the store in `TimingTraceStore` (test sources) and prints, after each scenario, one row per `TraceStore` method: type (read or write), calls, calls per record, total ms, mean ms and share of thread wall time (elapsed time times the number of chains). Below the table it prints store calls per record, writes per record and reads per record. The same three totals are appended to the CSV as the last columns (empty when timing is off).

The script has no option for it yet. Run Maven directly:

```
cd jans-lock/lock-server
mvn -o -P benchmark -pl model,service -am -Ddownload.plugin.skip=true test \
    -Dlock.benchmark.profile=/path/to/profile/conf -Dlock.benchmark.records=2000 \
    -Dlock.benchmark.chains=1,1,1,4,4,4 -Dlock.benchmark.timing=true
```

The first scenario is a warm-up. Repeat a chain count (`1,1,1`) to see run-to-run noise.

A store call is one `TraceStore` method call. It is not one SQL round trip: one `insertReceipt` is a `contains` read plus an insert in the ORM.

## Database-side view (PostgreSQL)

Run these on the database host, as a user which can read `pg_stat_*` (for example `su - postgres -c "psql -d jansdb"`), during a benchmark run. Use an otherwise idle database: other sessions add commits and wait events.

**Wait events.** Sample about every 0.5 s (`\watch 0.5` in `psql`). `IO:WALSync` and `LWLock:WALWrite` mean sessions wait for the WAL flush of a commit; `Client:ClientRead` means PostgreSQL waits for the next statement from Lock.

```sql
select state, wait_event_type, wait_event, count(*)
from pg_stat_activity
where datname = current_database() and backend_type = 'client backend' and pid <> pg_backend_pid()
group by 1, 2, 3 order by 4 desc;
```

The filter on `pg_backend_pid()` leaves out the sampling session itself. A row with empty wait columns and state `active` is a session running on a CPU. A session which waits for the next statement from the client is normally in state `idle` (or `idle in transaction`) with `Client:ClientRead`, so this query does not filter on `state`. Idle connections of the Lock pool and of other services also show up as `idle` / `Client:ClientRead`; read the `active` rows for commit waits and compare the `idle` rows against a sample taken before the run.

**Statements.** `pg_stat_statements` gives the top statements by `total_exec_time`. First check that it is installed:

```sql
select * from pg_extension;
```

If `pg_stat_statements` is not in the list, skip this step. Do not install it on a shared stand without asking the owner: it needs `shared_preload_libraries` and a restart.

```sql
select calls, round(total_exec_time::numeric, 1) as total_ms, round(mean_exec_time::numeric, 3) as mean_ms, left(query, 80)
from pg_stat_statements order by total_exec_time desc limit 15;
```

**Commits per ingested record.** Read the counters before and after the run:

```sql
select xact_commit, xact_rollback from pg_stat_database where datname = current_database();
```

`xact_commit` counts every transaction, also each read-only statement that runs in autocommit mode, so on this stack it is about 35 per record, not 3. For the write transactions use the next xid, which only transactions that write (and so get a transaction ID) advance, and the WAL flush counter:

```sql
select txid_snapshot_xmax(txid_current_snapshot()) as next_xid;
select wal_sync from pg_stat_wal;
```

Transactions with an ID per record = (`next_xid` after - `next_xid` before) / records ingested. This is an estimate of write commits: a write transaction which rolls back also takes an ID. Read `xact_rollback` before and after the run too; when it did not change, the xid delta equals the write commits. The "3 commits per record" figure is tested by this number.

WAL flushes per record = (`wal_sync` after - before) / records; it is below the commit count when parallel commits share a flush (group commit). `pg_stat_wal.wal_sync` counts flushes only when `fsync` is `on` and `wal_sync_method` is `fdatasync`, `fsync` or `fsync_writethrough`. With `open_datasync`, `open_sync` or `fsync = off` it stays zero, which does not mean that no WAL was flushed. Check with `show fsync; show wal_sync_method;` before you use it. Other sessions and the sampling session also commit. Measure the idle rate first (the same two counters over 30 s with no run), then subtract `idle rate x run seconds` from the delta. Report the idle rate with the result. Take the counters per scenario, not per Maven run: preload, chain registration and cleanup add commits to a whole-run delta.
