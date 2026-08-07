# Integration-test infrastructure

One Postgres instance backs **every** module's env-gated SQL integration tests. Its
`init-scripts/` provision the tables each module's ITs expect (all under `public`, disjoint tables):

| Script | Provisions | Used by |
|---|---|---|
| `00-trustrelationship-init.sql` | `jansTrustRelationship` | `trust-adapters` |
| `01-activation-init.sql` | `jansTrustActivationWorkItem` / `…Lease` / `…Worker` / `…Episode` | `trust-adapters` |
| `02-staged-file-init.sql` | `jansStagedFile` | `file-staging-adapters` |

These are also the **storage-visible names the jans setup must provision** for production (object
classes / branches / columns) — the scripts are the canonical reference DDL.

## Run the whole IT suite

```bash
# 1. start the database (init scripts run once, on an empty data dir)
docker compose -f docker/docker-compose.yaml up -d

# 2. run every module's tests, pointing each module's gate at the same DB
mvn -o test \
  -Dtrust.it.sql.uri=jdbc:postgresql://localhost:5432/jansdb \
  -Dtrust.it.sql.schema=public -Dtrust.it.sql.user=jans \
  -Dtrust.it.sql.password='VWSAG/ixu14S7EDjDNH4cQ==' \
  -Dstaging.it.sql.uri=jdbc:postgresql://localhost:5432/jansdb \
  -Dstaging.it.sql.schema=public -Dstaging.it.sql.user=jans \
  -Dstaging.it.sql.password='VWSAG/ixu14S7EDjDNH4cQ=='
```

Without the `*.it.sql.uri` properties the ITs are skipped, so the ordinary offline build stays green.

> After changing any DDL, recreate the container so the init scripts re-run (they only run on an empty
> data dir): `docker compose -f docker/docker-compose.yaml down -v && … up -d`.
