---
tags:
- administration
- reference
- kubernetes
- docker image
---

# Overview

Docker image packaging for config-api.

## Versions

See [Packages](https://github.com/orgs/JanssenProject/packages/container/package/jans%2Fconfig-api) for available versions.

## Environment Variables

The following environment variables are supported by the container:

- `CN_CONFIG_ADAPTER`: The config backend adapter, can be `consul` (default), `kubernetes`, `google`, or `aws`.
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
- `CN_SECRET_ADAPTER`: The secrets' adapter, can be `vault`, `kubernetes`, `google`, or `aws`.
- `CN_SECRET_VAULT_VERIFY`: whether to verify cert or not (default to `false`).
- `CN_SECRET_VAULT_ROLE_ID_FILE`: path to file contains Vault AppRole role ID (default to `/etc/certs/vault_role_id`).
- `CN_SECRET_VAULT_SECRET_ID_FILE`: path to file contains Vault AppRole secret ID (default to `/etc/certs/vault_secret_id`).
- `CN_SECRET_VAULT_CERT_FILE`: path to Vault cert file (default to `/etc/certs/vault_client.crt`).
- `CN_SECRET_VAULT_KEY_FILE`: path to Vault key file (default to `/etc/certs/vault_client.key`).
- `CN_SECRET_VAULT_CACERT_FILE`: path to Vault CA cert file (default to `/etc/certs/vault_ca.crt`). This file will be used if it exists and `CN_SECRET_VAULT_VERIFY` set to `true`.
- `CN_SECRET_VAULT_ADDR`: URL of Vault (default to `http://localhost:8200`).
- `CN_SECRET_VAULT_NAMESPACE`: Namespace used to access secrets (default to empty string).
- `CN_SECRET_VAULT_KV_PATH`: Path to KV secrets engine (default to `secret`).
- `CN_SECRET_VAULT_PREFIX`: Base prefix name used to build secret path (default to `jans`).
- `CN_SECRET_VAULT_APPROLE_PATH`: Path to AppRole (default to `approle`).
- `CN_SECRET_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `CN_SECRET_KUBERNETES_SECRET`: Kubernetes secrets name (default to `jans`).
- `CN_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `CN_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `CN_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `CN_MAX_RAM_PERCENTAGE`: Value passed to Java option `-XX:MaxRAMPercentage`.
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, `sql`, `spanner`, or `hybrid`; default to `ldap`).
- `CN_HYBRID_MAPPING`: Specify data mapping for each persistence (default to `"{}"`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`. See [hybrid mapping](#hybrid-mapping) section for details.
- `CN_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`).
- `CN_LDAP_USE_SSL`: Whether to use SSL connection to LDAP server (default to `true`).
- `CN_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`).
- `CN_COUCHBASE_USER`: Username of Couchbase server (default to `admin`).
- `CN_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`).
- `CN_COUCHBASE_PASSWORD_FILE`: Path to file contains Couchbase password (default to `/etc/jans/conf/couchbase_password`).
- `CN_COUCHBASE_CONN_TIMEOUT`: Connect timeout used when a bucket is opened (default to `10000` milliseconds).
- `CN_COUCHBASE_CONN_MAX_WAIT`: Maximum time to wait before retrying connection (default to `20000` milliseconds).
- `CN_COUCHBASE_SCAN_CONSISTENCY`: Default scan consistency; one of `not_bounded`, `request_plus`, or `statement_plus` (default to `not_bounded`).
- `CN_COUCHBASE_BUCKET_PREFIX`: Prefix for Couchbase buckets (default to `jans`).
- `CN_COUCHBASE_TRUSTSTORE_ENABLE`: Enable truststore for encrypted Couchbase connection (default to `true`).
- `CN_COUCHBASE_KEEPALIVE_INTERVAL`: Keep-alive interval for Couchbase connection (default to `30000` milliseconds).
- `CN_COUCHBASE_KEEPALIVE_TIMEOUT`: Keep-alive timeout for Couchbase connection (default to `2500` milliseconds).
- `CN_CONFIG_API_JAVA_OPTIONS`: Java options passed to entrypoint, i.e. `-Xmx1024m` (default to empty-string).
- `CN_CONFIG_API_LOG_LEVEL`: Log level for config api. Options include `OFF`, `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`.  and `ALL`. This defaults to `INFO`
- `CN_AUTH_SERVER_URL`: Base URL of Janssen Auth server, i.e. `auth-server:8080` (default to empty string).
- `GOOGLE_APPLICATION_CREDENTIALS`: Optional JSON file (contains Google credentials) that can be injected into container for authentication. Refer to https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to for supported credentials.
- `GOOGLE_PROJECT_ID`: ID of Google project.
- `CN_GOOGLE_SECRET_VERSION_ID`: Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
- `CN_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
- `CN_GOOGLE_SPANNER_INSTANCE_ID`: Google Spanner instance ID.
- `CN_GOOGLE_SPANNER_DATABASE_ID`: Google Spanner database ID.
- `CN_CONFIG_API_APP_LOGGERS`: Custom logging configuration in JSON-string format with hash type (see [Configure app loggers](#configure-app-loggers) section for details).
- `CN_CONFIG_API_PLUGINS`: Comma-separated plugin names that should be enabled (available plugins are `admin-ui`, `scim`, `fido2`, `user-mgt`, `jans-link`, `kc-saml`, `kc-link`, `lock`). Note that unknown plugin name will be ignored.
- `CN_TOKEN_SERVER_CERT_FILE`: Path to token server certificate (default to `/etc/certs/token_server.crt`).
- `CN_TOKEN_SERVER_BASE_HOSTNAME`: Hostname of token server (default to empty string).
- `CN_ADMIN_UI_PLUGIN_LOGGERS`: Custom logging configuration for AdminUI plugin in JSON-string format with hash type (see [Configure plugin loggers](#configure-plugin-loggers) section for details).
- `CN_PROMETHEUS_PORT`: Port used by Prometheus JMX agent (default to empty string). To enable Prometheus JMX agent, set the value to a number. See [Exposing metrics](#exposing-metrics) for details.
- `CN_SQL_DB_HOST`: Hostname of the SQL database (default to `localhost`).
- `CN_SQL_DB_PORT`: Port of the SQL database (default to `3306` for MySQL).
- `CN_SQL_DB_NAME`: SQL database name (default to `jans`).
- `CN_SQL_DB_USER`: User name to access the SQL database (default to `jans`).
- `CN_SQL_DB_DIALECT`: Dialect name of the SQL (`mysql` for MySQL  or `pgsql` for PostgreSQL; default to `mysql`).
- `CN_SQL_DB_TIMEZONE`: Timezone used by the SQL database (default to `UTC`).
- `CN_SQL_DB_SCHEMA`: Schema name used by SQL database (default to empty-string; if using MySQL, the schema name will be resolved as the database name, whereas in PostgreSQL the schema name will be resolved as `"public"`).
- `CN_AWS_SECRETS_ENDPOINT_URL`: The URL of AWS secretsmanager service (if omitted, will use the one in specified region).
- `CN_AWS_SECRETS_PREFIX`: The prefix name of the secrets (default to `jans`).
- `CN_AWS_SECRETS_REPLICA_FILE`: The location of file contains replica regions definition (if any). This file is mostly used in primary region. Example of contents of the file: `[{"Region": "us-west-1"}]`.
- `AWS_DEFAULT_REGION`: The default AWS Region to use, for example, `us-west-1` or `us-west-2`.
- `AWS_SHARED_CREDENTIALS_FILE`: The location of the shared credentials file used by the client (see https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).
- `AWS_CONFIG_FILE`: The location of the config file used by the client (see https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).
- `AWS_PROFILE`: The default profile to use, if any.

### Configure app loggers

App loggers can be configured to define where the logs will be redirected and what is the level the logs should be displayed.

Supported redirect target:

- `STDOUT`
- `FILE`

Supported level:

- `FATAL`
- `ERROR`
- `WARN`
- `INFO`
- `DEBUG`
- `TRACE`

The following key-value pairs are the defaults:

```json
{
    "config_api_log_target": "STDOUT",
    "config_api_log_level": "INFO",
    "persistence_log_target": "FILE",
    "persistence_log_level": "INFO",
    "persistence_duration_log_target": "FILE",
    "persistence_duration_log_level": "INFO",
    "ldap_stats_log_target": "FILE",
    "ldap_stats_log_level": "INFO",
    "script_log_target": "FILE",
    "script_log_level": "INFO",
    "audit_log_target": "FILE",
    "audit_log_level": "INFO"
}
```

To enable prefix on `STDOUT` logging, set the `enable_stdout_log_prefix` key. Example:

```
{"config_api_log_target":"STDOUT","script_log_target":"STDOUT","enable_stdout_log_prefix":true}
```

### Configure plugin loggers

Plugin loggers can be configured to define where the logs will be redirected and what is the level the logs should be displayed.

Supported redirect target:

- `STDOUT`
- `FILE`

Supported level:

- `FATAL`
- `ERROR`
- `WARN`
- `INFO`
- `DEBUG`
- `TRACE`

The following key-value pairs are the defaults:

```json
{
    "admin_ui_log_target": "FILE",
    "admin_ui_log_level": "INFO",
    "admin_ui_audit_log_target": "FILE",
    "admin_ui_audit_log_level": "INFO"
}
```

To enable prefix on `STDOUT` logging, set the `enable_stdout_log_prefix` key. Example:

```
{"admin_ui_log_target":"STDOUT","enable_stdout_log_prefix":true}
```

### Hybrid mapping

As per v1.0.1, hybrid persistence supports all available persistence types. To configure hybrid persistence and its data mapping, follow steps below:

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

### Exposing metrics

As per v1.0.1, certain metrics can be exposed via Prometheus JMX exporter.
To expose the metrics, set the `CN_PROMETHEUS_PORT` environment variable, i.e. `CN_PROMETHEUS_PORT=9093`.
Afterwards, metrics can be scraped by Prometheus or accessed manually by making request to `/metrics` URL,
i.e. `http://container:9093/metrics`.

Note that Prometheus JMX exporter uses pre-defined config file (see `conf/prometheus-config.yaml`).
To customize the config, mount custom config file to `/opt/prometheus/prometheus-config.yaml` inside the container.

