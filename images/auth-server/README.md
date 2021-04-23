[![Codacy Badge](https://app.codacy.com/project/badge/Grade/0bcee00d656b4533ab1e32c36e32a17d)](https://www.codacy.com/gh/JanssenProject/docker-jans-auth-server/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=JanssenProject/docker-jans-auth-server&amp;utm_campaign=Badge_Grade)

## Overview

Docker image packaging for Janssen Authorization Server.

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-auth-server/releases) for stable versions.
For bleeding-edge/unstable version, use `janssenproject/auth-server:1.0.0_dev`.

## Environment Variables

The following environment variables are supported by the container:

- `CN_CONFIG_ADAPTER`: The config backend adapter, can be `consul` (default), `kubernetes`, or `google`.
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
- `CN_CONFIG_GOOGLE_SECRET_VERSION_ID`: Janssen configuration secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_CONFIG_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen configuration secret in Google Secret Manager. Defaults to `jans`. If left intact `jans-configuration` secret will be created.
- `CN_SECRET_ADAPTER`: The secrets adapter, can be `vault` (default), `kubernetes`, or `google`.
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
- `CN_SECRET_KUBERNETES_SECRET`: Kubernetes secrets name (default to `jans`).
- `CN_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `CN_SECRET_GOOGLE_SECRET_VERSION_ID`:  Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
- `CN_SECRET_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created..
- `CN_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `CN_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `CN_MAX_RAM_PERCENTAGE`: Value passed to Java option `-XX:MaxRAMPercentage`.
- `CN_DEBUG_PORT`: port of remote debugging (if omitted, remote debugging will be disabled).
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, or `hybrid`; default to `ldap`).
- `CN_PERSISTENCE_LDAP_MAPPING`: Specify data that should be saved in LDAP (one of `default`, `user`, `cache`, `site`, `token`, or `session`; default to `default`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`.
- `CN_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`); required if `CN_PERSISTENCE_TYPE` is set to `ldap` or `hybrid`.
- `CN_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_USER`: Username of Couchbase server (default to `admin`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_PASSWORD_FILE`: Path to file contains Couchbase password (default to `/etc/jans/conf/couchbase_password`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_CONN_TIMEOUT`: Connect timeout used when a bucket is opened (default to `10000` milliseconds).
- `CN_COUCHBASE_CONN_MAX_WAIT`: Maximum time to wait before retrying connection (default to `20000` milliseconds).
- `CN_COUCHBASE_SCAN_CONSISTENCY`: Default scan consistency; one of `not_bounded`, `request_plus`, or `statement_plus` (default to `not_bounded`).
- `CN_COUCHBASE_BUCKET_PREFIX`: Prefix for Couchbase buckets (default to `jans`).
- `CN_COUCHBASE_TRUSTSTORE_ENABLE`: Enable truststore for encrypted Couchbase connection (default to `true`).
- `CN_COUCHBASE_KEEPALIVE_INTERVAL`: Keep-alive interval for Couchbase connection (default to `30000` milliseconds).
- `CN_COUCHBASE_KEEPALIVE_TIMEOUT`: Keep-alive timeout for Couchbase connection (default to `2500` milliseconds).
- `CN_JAVA_OPTIONS`: Java options passed to entrypoint, i.e. `-Xmx1024m` (default to empty-string).
- `CN_DOCUMENT_STORE_TYPE`: Document store type (one of `LOCAL` or `JCA`; default to `LOCAL`).
- `CN_JACKRABBIT_URL`: URL to remote repository (default to `http://localhost:8080`).
- `CN_JACKRABBIT_SYNC_INTERVAL`: Interval between files sync (default to `300` seconds).
- `CN_JACKRABBIT_ADMIN_ID`: Admin username (default to `admin`).
- `CN_JACKRABBIT_ADMIN_PASSWORD_FILE`: Absolute path to file contains password for admin user (default to `/etc/jans/conf/jackrabbit_admin_password`).
- `CN_SYNC_JKS_ENABLED`: Whether to enable JKS sync (default to `false`). Enable this flag if using `gluufederation/key-rotation:4.1.1_02` and below (for key-regeneration).
- `CN_SYNC_JKS_INTERVAL`: Interval of JKS sync in seconds (if needed); obsolete.
- `GOOGLE_PROJECT_ID`: Google Project ID (default to empty string). Used when `CN_CONFIG_ADAPTER` or `CN_SECRET_ADAPTER` set to `google`.
- `GOOGLE_APPLICATION_CREDENTIALS`: Path to Google credentials JSON file (default to `/etc/jans/conf/google-credentials.json`). Used when `CN_CONFIG_ADAPTER` or `CN_SECRET_ADAPTER` set to `google`.
