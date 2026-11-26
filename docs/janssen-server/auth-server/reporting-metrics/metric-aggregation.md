---
tags:
  - administration
  - reporting
  - metric
  - aggregation
---

# Metric aggregation

The metric reporter of each Janssen application writes raw rows to the `jansMetric` table every
`metricReporterInterval` seconds. Metric aggregation reads those rows and writes summary rows to the
`jansMetricAggregation` table, one row per application type, metric type and metric subtype for each
period. The periods are `HOURLY`, `DAILY`, `WEEKLY` and `MONTHLY`. The Config API returns these rows
from `GET /jans-config-api/metric/aggregations`.

Raw counters are cumulative since the start of the JVM. Aggregation therefore sums the change per
reporting interval, never the raw values, so restarts do not distort the totals.

## When aggregation runs

Aggregation runs on a node only when both conditions are true:

- the metric reporter of the application is enabled;
- `metricAggregationConfiguration.enabled` is `true` (the default).

The metric reporter switch is different for each application:

| Application | Metric reporter switch |
|---|---|
| jans-auth | the `metric` value in the `featureFlags` list. The `metricReporterEnabled` key that appears in the jans-auth configuration template is not read by jans-auth. |
| jans-lock and FIDO2 | `metricReporterEnabled` |

Turning the reporter on is therefore enough to turn aggregation on.

`fido2MetricsAggregationEnabled` is a different feature. It controls the passkey telemetry
aggregation described in [Passkey Telemetry & Metrics](../../fido/passkey-telemetry.md) and does
not affect the aggregation described here.

jans-lock starts its metric reporter before scheduling aggregation when `metricReporterEnabled` is
true. This aggregation flow is available in jans-auth, jans-lock and FIDO2 on SQL. With both switches enabled, FIDO2 writes these rows to
`jansMetricAggregation`. Its separate passkey telemetry scheduler writes to `jansFido2MetricsAggregation`.

## One node per slot

Every node runs the job on a timer, but only one node does the work for each application type and
each time slot. A node first inserts a claim row into the `jansClusterLock` table. The insert fails
if another node already holds the claim, and then the node skips the slot. Each acquisition has a
unique token. Heartbeats, release, state saves and aggregate writes validate that token and the
unexpired lease while holding the claim row lock in the same SQL transaction as the write.
Running claims expire after their lease; released claims are retained for one day. Each run purges
expired claims of its task. A run reports completion only after release is confirmed.
Each node that loses a claim insert causes one database server error line (a duplicate key on
`jansClusterLock`) per slot. This is expected.

Each application type is aggregated separately: jans-auth, jans-lock and FIDO2 each have their own
claims and their own progress record.

## Properties

The settings are in the `metricAggregationConfiguration` object of the dynamic configuration of
jans-auth, jans-lock and FIDO2. Every property is optional. A missing property, or a numeric
value of 0 or less, gives the default. An upgraded installation without the object works with the
defaults.

| Property | Default | Description |
|---|---|---|
| `enabled` | `true` | Switch for aggregation. |
| `runIntervalSeconds` | `300` | Seconds between two runs on each node. |
| `maxBucketsPerRun` | `48` | Maximum number of periods of one level that one run computes. A longer backlog is processed over several runs. |
| `recomputeHourly` | `2` | Number of the latest closed `HOURLY` periods that each run computes again. |
| `recomputeDaily` | `1` | Same for `DAILY`. |
| `recomputeWeekly` | `1` | Same for `WEEKLY`. |
| `recomputeMonthly` | `1` | Same for `MONTHLY`. |
| `keepHourlyDays` | `90` | Days to keep `HOURLY` rows. |
| `keepDailyDays` | `730` | Days to keep `DAILY` rows. |
| `keepWeeklyDays` | `1825` | Days to keep `WEEKLY` rows. |
| `keepMonthlyDays` | `3650` | Days to keep `MONTHLY` rows. |
| `leaseSeconds` | `600` | Seconds after its last heartbeat for which a running claim counts as alive. |

Example:

```json
"metricAggregationConfiguration": {
  "enabled": true,
  "runIntervalSeconds": 300,
  "maxBucketsPerRun": 48,
  "recomputeHourly": 2,
  "recomputeDaily": 1,
  "recomputeWeekly": 1,
  "recomputeMonthly": 1,
  "keepHourlyDays": 90,
  "keepDailyDays": 730,
  "keepWeeklyDays": 1825,
  "keepMonthlyDays": 3650,
  "leaseSeconds": 600
}
```

## How the rows are built

- All times are UTC. A week starts on Monday at 00:00 UTC.
- A raw row belongs to the period that contains its `jansEndDate`.
- `DAILY` rows are built from `HOURLY` rows. `WEEKLY` and `MONTHLY` rows are built from `DAILY` rows.
- A period closes after its end plus a grace time of two reporter intervals and 60 seconds. Each run
  computes the latest closed periods again (`recompute*` properties), so rows that arrive late are
  still counted.
- **Rows that arrive after that recompute window are not counted.**
- Timer percentiles (median, 75th, 95th, 98th, 99th and 99.9th) are weighted averages of the
  percentiles of the raw rows. They are approximate, and the row says so with `"approximate": true`.
  Minimum, maximum and mean are also reservoir-based estimates: the source snapshots can contain
  events from before the reporting interval. All duration statistics are normalized to milliseconds.
  Counts use reporting interval counts, with cumulative deltas for legacy rows.
- The `jansEndDate` of an aggregation row is the end of its period minus 1 millisecond, the same
  convention as raw rows. For example, a `DAILY` row of 2026-11-24 ends at `2026-11-24T23:59:59.999Z`.
- Periods without raw rows produce no row. An unreadable child makes its key's rollup incomplete.
- Hourly raw rows are processed in bounded pages, carrying per-series cumulative counts between
  pages. The watermark advances only after every page and aggregate write for a bucket succeeds.
- A bucket is skipped if its source retention no longer covers the whole period. Hours require raw
  rows, days require hourly rows, and weeks and months require daily rows.
- Reporter rows are persisted in one atomic SQL batch. A failed batch leaves interval baselines
  unchanged and its inserts are rolled back before retry. Stores without atomic batch support save
  raw report rows individually; earlier inserts can survive a later failure.

The `exp` of a row is set from the `keep*Days` property of its level. Expired rows are removed by
the periodic data cleaner of the installation.

## Limits

- **SQL is the only supported backend.** LDAP, Spanner and Couchbase are not supported. Claims,
  state and aggregates must use the same transactional SQL entry manager; otherwise work is refused.
  Unsupported claim backends produce a `WARN` message at most once per hour.
- If the `jansClusterLock` table is missing, aggregation also stays idle. The server logs a `WARN`
  message that the claim insert failed, and the rest of the application is not affected.

## Upgrading a VM installation

New VM installations and container (cloud-native) installations create the `jansClusterLock` table
automatically. The persistence loader of a container deployment creates missing tables when it starts.

`install.py -upgrade` of a VM installation only replaces the application files and does not create
new tables. On an existing VM installation, create the table once, by hand, before the new version
starts. Until then, aggregation stays idle with a `WARN` message.

The statements below are what the setup script generates for the `jansClusterLock` object class.
Replace the schema `public` and the owner `jans` (PostgreSQL), or the database name `jansdb`
(MySQL), with the values of your installation.

=== "PostgreSQL"

    ```sql
    BEGIN;
    CREATE TABLE public."jansClusterLock" (doc_id VARCHAR(64) NOT NULL UNIQUE, "objectClass" VARCHAR(48),
      dn VARCHAR(128), "jansId" VARCHAR(128), "jansType" VARCHAR(64), "lockKey" VARCHAR(64),
      "jansHost" VARCHAR(64), "jansNum" INTEGER, "creationDate" TIMESTAMP, "jansLastUpd" TIMESTAMP,
      "exp" TIMESTAMP, "del" BOOLEAN, "jansStatus" VARCHAR(16), "jansData" TEXT, PRIMARY KEY (doc_id));
    ALTER TABLE public."jansClusterLock" OWNER TO jans;
    CREATE INDEX ON public."jansClusterLock" ("jansType");
    CREATE INDEX ON public."jansClusterLock" ("del", "exp");
    COMMIT;
    ```

=== "MySQL"

    ```sql
    CREATE TABLE `jansdb`.`jansClusterLock` (`doc_id` VARCHAR(64) NOT NULL UNIQUE, `objectClass` VARCHAR(48),
      dn VARCHAR(128),
      `jansId` VARCHAR(128) COMMENT "Identifier",
      `jansType` VARCHAR(64) COMMENT "Type of the entry",
      `lockKey` VARCHAR(64) COMMENT "Entry lock key",
      `jansHost` VARCHAR(64) COMMENT "jans host",
      `jansNum` INT COMMENT "Numeric identifier",
      `creationDate` DATETIME(3) COMMENT "Creation Date used for password reset requests",
      `jansLastUpd` DATETIME(3) COMMENT "Monitors last time the server was able to connect to  the monitoring system.",
      `exp` DATETIME(3) COMMENT "jans Exp",
      `del` SMALLINT COMMENT "del",
      `jansStatus` VARCHAR(16) COMMENT "Status of the entry, used by many objectclasses",
      `jansData` TEXT COMMENT "OX data",
      PRIMARY KEY (`doc_id`));
    ALTER TABLE jansdb.jansClusterLock ADD INDEX `jansClusterLock_jansType` (`jansType`);
    ALTER TABLE jansdb.jansClusterLock ADD INDEX `jansClusterLock_CustomIdx1` (`del`, `exp`);
    ```

To confirm that the table exists:

=== "PostgreSQL"

    ```sql
    \d public."jansClusterLock"
    ```

=== "MySQL"

    ```sql
    DESCRIBE jansClusterLock;
    ```

The data cleaner of a VM installation reads its table list when it is installed. An upgraded
installation keeps the old list and does not clean `jansClusterLock`. This is harmless, because the
aggregation job purges its own expired claims.

## Related

- [Reporting and Metrics](README.md)
- [Janssen Authorization Server properties](../../reference/json/properties/janssenauthserver-properties.md#metricaggregationconfiguration)
- [Janssen Lock properties](../../reference/json/properties/lock-properties.md#metricaggregationconfiguration)
