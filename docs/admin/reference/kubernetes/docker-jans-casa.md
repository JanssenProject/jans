---
tags:
- administration
- reference
- kubernetes
- docker image
- casa
---

## Overview

Docker assets for Casa

## Versions

See [Releases](https://github.com/GluuFederation/docker-casa/releases) for stable versions.
For bleeding-edge/unstable version, use `gluufederation/casa:5.0.0_dev`.

## Environment Variables

The following environment variables are supported by the container:

- `CN_CONFIG_ADAPTER`: The config backend adapter, can be `consul` (default) or `kubernetes`.
- `CN_CONFIG_CONSUL_HOST`: hostname or IP of Consul (default to `localhost`).
- `CN_CONFIG_CONSUL_PORT`: port of Consul (default to `8500`).
- `CN_CONFIG_CONSUL_CONSISTENCY`: Consul consistency mode (choose one of `default`, `consistent`, or `stale`). Default to `stale` mode.
- `CN_CONFIG_CONSUL_SCHEME`: supported Consul scheme (`http` or `https`).
- `CN_CONFIG_CONSUL_VERIFY`: whether to verify cert or not (default to `false`).
- `CN_CONFIG_CONSUL_CACERT_FILE`: path to Consul CA cert file (default to `/etc/certs/consul_ca.crt`). This file will be used if it exists and `CN_CONFIG_CONSUL_VERIFY` set to `true`.
- `CN_CONFIG_CONSUL_CERT_FILE`: path to Consul cert file (default to `/etc/certs/consul_client.crt`).
- `CN_CONFIG_CONSUL_KEY_FILE`: path to Consul key file (default to `/etc/certs/consul_client.key`).
- `CN_CONFIG_CONSUL_TOKEN_FILE`: path to file contains ACL token (default to `/etc/certs/consul_token`).
- `CN_CONFIG_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `CN_CONFIG_KUBERNETES_CONFIGMAP`: Kubernetes configmaps name (default to `jans`).
- `CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `CN_SECRET_ADAPTER`: The secrets' adapter, can be `vault` or `kubernetes`.
- `CN_SECRET_VAULT_SCHEME`: supported Vault scheme (`http` or `https`).
- `CN_SECRET_VAULT_HOST`: hostname or IP of Vault (default to `localhost`).
- `CN_SECRET_VAULT_PORT`: port of Vault (default to `8200`).
- `CN_SECRET_VAULT_VERIFY`: whether to verify cert or not (default to `false`).
- `CN_SECRET_VAULT_ROLE_ID_FILE`: path to file contains Vault AppRole role ID (default to `/etc/certs/vault_role_id`).
- `CN_SECRET_VAULT_SECRET_ID_FILE`: path to file contains Vault AppRole secret ID (default to `/etc/certs/vault_secret_id`).
- `CN_SECRET_VAULT_CERT_FILE`: path to Vault cert file (default to `/etc/certs/vault_client.crt`).
- `CN_SECRET_VAULT_KEY_FILE`: path to Vault key file (default to `/etc/certs/vault_client.key`).
- `CN_SECRET_VAULT_CACERT_FILE`: path to Vault CA cert file (default to `/etc/certs/vault_ca.crt`). This file will be used if it exists and `CN_SECRET_VAULT_VERIFY` set to `true`.
- `CN_SECRET_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `CN_SECRET_KUBERNETES_CONFIGMAP`: Kubernetes secrets name (default to `jans`).
- `CN_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `CN_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `CN_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `CN_MAX_RAM_PERCENTAGE`: Value passed to Java option `-XX:MaxRAMPercentage`.
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, or `hybrid`; default to `ldap`).
- `CN_HYBRID_MAPPING`: Specify data mapping for each persistence (default to `"{}"`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`. See [hybrid mapping](#hybrid-mapping) section for details.
- `CN_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`); required if `CN_PERSISTENCE_TYPE` is set to `ldap` or `hybrid`.
- `CN_LDAP_USE_SSL`: Whether to use SSL connection to LDAP server (default to `true`).
- `CN_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_USER`: Username of Couchbase server (default to `admin`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_CONN_TIMEOUT`: Connect timeout used when a bucket is opened (default to `10000` milliseconds).
- `CN_COUCHBASE_CONN_MAX_WAIT`: Maximum time to wait before retrying connection (default to `20000` milliseconds).
- `CN_COUCHBASE_SCAN_CONSISTENCY`: Default scan consistency; one of `not_bounded`, `request_plus`, or `statement_plus` (default to `not_bounded`).
- `CN_COUCHBASE_BUCKET_PREFIX`: Prefix for Couchbase buckets (default to `jans`).
- `CN_COUCHBASE_TRUSTSTORE_ENABLE`: Enable truststore for encrypted Couchbase connection (default to `true`).
- `CN_COUCHBASE_KEEPALIVE_INTERVAL`: Keep-alive interval for Couchbase connection (default to `30000` milliseconds).
- `CN_COUCHBASE_KEEPALIVE_TIMEOUT`: Keep-alive timeout for Couchbase connection (default to `2500` milliseconds).
- `CN_JAVA_OPTIONS`: Java options passed to entrypoint, i.e. `-Xmx1024m` (default to empty-string).
- `CN_DOCUMENT_STORE_TYPE`: Document store type (one of `DB`, `LOCAL` or `JCA`; default to `LOCAL`).
- `CN_JACKRABBIT_URL`: URL to remote repository (default to `http://localhost:8080`).
- `CN_JACKRABBIT_SYNC_INTERVAL`: Interval between files sync (default to `300` seconds).
- `CN_JACKRABBIT_ADMIN_ID`: Admin username (default to `admin`).
- `CN_JACKRABBIT_ADMIN_PASSWORD_FILE`: Absolute path to file contains password for admin user (default to `/etc/jans/conf/jackrabbit_admin_password`).
- `CN_SSL_CERT_FROM_SECRETS`: Determine whether to get SSL cert from secrets backend (default to `false`). Note that the flag will take effect only if there's no mounted `/etc/certs/web_https.crt` file.
- `CN_SQL_DB_DIALECT`: Dialect name of SQL backend (one of `mysql`, `pgsql`; default to `mysql`).
- `CN_SQL_DB_HOST`: Host of SQL backend (default to `localhost`).
- `CN_SQL_DB_PORT`: Port of SQL backend (default to `3306`).
- `CN_SQL_DB_NAME`: Database name (default to `jans`)
- `CN_SQL_DB_USER`: Username to interact with SQL backend (default to `jans`).
- `CN_GOOGLE_SPANNER_INSTANCE_ID`: Instance ID of Google Spanner (default to empty string).
- `CN_GOOGLE_SPANNER_DATABASE_ID`: Database ID of Google Spanner (default to empty string).
- `GOOGLE_APPLICATION_CREDENTIALS`: JSON file (contains Google credentials) that should be injected into container.
- `GOOGLE_PROJECT_ID`: ID of Google project.
- `CN_GOOGLE_SECRET_VERSION_ID`: Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
- `CN_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
- `GLUU_CASA_APP_LOGGERS`: Custom logging configuration in JSON-string format with hash type (see [Configure app loggers](#configure-app-loggers) section for details).
- `CN_CASA_ADMIN_LOCK_FILE`: Path to lock file to enable/disable administration feature (default to `/opt/jans/jetty/jans-casa/.administrable`). If file is not exist, the feature is disabled.
- `CN_PROMETHEUS_PORT`: Port used by Prometheus JMX agent (default to empty string). To enable Prometheus JMX agent, set the value to a number. See [Exposing metrics](#exposing-metrics) for details.

### Configure app loggers

App loggers can be configured to define where the logs will be redirected and what is the level the logs should be displayed.

Supported redirect target:

- `STDOUT`
- `FILE`

Supported level:

- `OFF`
- `FATAL`
- `ERROR`
- `WARN`
- `INFO`
- `DEBUG`
- `TRACE`

The following key-value pairs are the defaults:

```json
{
    "casa_log_target": "STDOUT",
    "casa_log_level": "INFO",
    "timer_log_target": "FILE",
    "timer_log_level": "INFO"
}
```

To enable prefix on `STDOUT` logging, set the `enable_stdout_log_prefix` key. Example:

```
{"casa_log_target":"STDOUT","timer_log_target":"STDOUT","enable_stdout_log_prefix":true}
```

### Exposing metrics

As per v1.0.1, certain metrics can be exposed via Prometheus JMX exporter.
To expose the metrics, set the `CN_PROMETHEUS_PORT` environment variable, i.e. `CN_PROMETHEUS_PORT=9093`.
Afterwards, metrics can be scraped by Prometheus or accessed manually by making request to `/metrics` URL,
i.e. `http://container:9093/metrics`.

Note that Prometheus JMX exporter uses pre-defined config file (see `conf/prometheus-config.yaml`).
To customize the config, mount custom config file to `/opt/prometheus/prometheus-config.yaml` inside the container.

### Hybrid mapping

Hybrid persistence supports all available persistence types. To configure hybrid persistence and its data mapping, follow steps below:

1. Set `CN_PERSISTENCE_TYPE` environment variable to `hybrid`

2. Set `CN_HYBRID_MAPPING` with the following format:

    ```
    {
        "default": "<couchbase|ldap|spanner|sql>",
        "user": "<couchbase|ldap|spanner|sql>",
        "site": "<couchbase|ldap|spanner|sql>",
        "cache": "<couchbase|ldap|spanner|sql>",
        "token": "<couchbase|ldap|spanner|sql>",
        "session": "<couchbase|ldap|spanner|sql>",
    }
    ```

    Example:

    ```
    {
        "default": "sql",
        "user": "spanner",
        "site": "ldap",
        "cache": "sql",
        "token": "couchbase",
        "session": "spanner",
    }
    ```