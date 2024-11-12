---
tags:
- administration
- reference
- kubernetes
- docker image
---

## Overview

Persistence is a special container to load initial data for supported persistence.

## Versions

See [Packages](https://github.com/orgs/JanssenProject/packages/container/package/jans%2Fpersistence-loader) for available versions.

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
- `CN_SECRET_ADAPTER`: The secrets' adapter, can be `vault` (default), `kubernetes`, or `google`.
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
- `CN_OXTRUST_CONFIG_GENERATION`: Whether to generate oxShibboleth configuration or not (default to `true`).
- `CN_CACHE_TYPE`: Supported values are `IN_MEMORY`, `REDIS`, `MEMCACHED`, and `NATIVE_PERSISTENCE` (default to `NATIVE_PERSISTENCE`).
- `CN_EXT_SIGNING_JWKS_URI`: URI of external signing JWKS (default is "").
- `CN_REDIS_URL`: URL of Redis server, format is host:port (optional; default to `localhost:6379`).
- `CN_REDIS_TYPE`: Redis service type, either `STANDALONE` or `CLUSTER` (optional; default to `STANDALONE`).
- `CN_MEMCACHED_URL`: URL of Memcache server, format is host:port (optional; default to `localhost:11211`).
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (`couchbase`, `sql`, or `hybrid`; default to `sql`).
- `CN_HYBRID_MAPPING`: Specify data mapping for each persistence (default to `"{}"`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`. See [hybrid mapping](#hybrid-mapping) section for details.
- `CN_PERSISTENCE_SKIP_INITIALIZED`: skip initialization if backend already initialized (default to `false`).
- `CN_PERSISTENCE_UPDATE_AUTH_DYNAMIC_CONFIG`: Whether to allow automatic updates of `jans-auth` configuration (default to `true`).
- `CN_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`).
- `CN_COUCHBASE_USER`: Username of Couchbase server (default to `admin`).
- `CN_COUCHBASE_SUPERUSER`: Superuser of Couchbase server (default to empty-string).
- `CN_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`).
- `CN_COUCHBASE_PASSWORD_FILE`: Path to file contains Couchbase password (default to `/etc/jans/conf/couchbase_password`).
- `CN_COUCHBASE_SUPERUSER_PASSWORD_FILE`: Path to file contains Couchbase superuser password (default to `/etc/jans/conf/couchbase_superuser_password`).
- `CN_DOCUMENT_STORE_TYPE`: Document store type (one of `LOCAL` or `DB`; default to `DB`).
- `CN_JACKRABBIT_URL`: URL to remote repository (default to `http://localhost:8080`).
- `CN_JACKRABBIT_ADMIN_ID_FILE`: Absolute path to file contains ID for admin user (default to `/etc/jans/conf/jackrabbit_admin_id`).
- `CN_JACKRABBIT_ADMIN_PASSWORD_FILE`: Absolute path to file contains password for admin user (default to `/etc/gluu/conf/jackrabbit_admin_password`).
- `GOOGLE_APPLICATION_CREDENTIALS`: Optional JSON file (contains Google credentials) that can be injected into container for authentication. Refer to https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to for supported credentials.
- `GOOGLE_PROJECT_ID`: ID of Google project.
- `CN_GOOGLE_SECRET_VERSION_ID`: Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
- `CN_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
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
- `CN_MESSAGE_TYPE`: Message provider type (one of `DISABLED`, `POSTGRES`, and `REDIS`; default to `DISABLED`).

### Hybrid mapping

As per v1.0.1, hybrid persistence supports all available persistence types. To configure hybrid persistence and its data mapping, follow steps below:

1. Set `CN_PERSISTENCE_TYPE` environment variable to `hybrid`

2. Set `CN_HYBRID_MAPPING` with the following format:

    ```
    {
        "default": "<couchbase|sql>",
        "user": "<couchbase|sql>",
        "site": "<couchbase|sql>",
        "cache": "<couchbase|sql>",
        "token": "<couchbase|sql>",
        "session": "<couchbase|sql>",
    }
    ```

    Example:

    ```
    {
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "couchbase",
        "session": "sql",
    }
    ```

