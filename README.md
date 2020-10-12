# Overview

Docker image packaging for Fido2.

## Versions

See [Releases](https://github.com/GluuFederation/docker-fido2/releases) for stable versions.
For bleeding-edge/unstable version, use `gluufederation/fido2:4.2.2_dev`.

## Environment Variables

The following environment variables are supported by the container:

- `JANS_CONFIG_ADAPTER`: The config backend adapter, can be `consul` (default) or `kubernetes`.
- `JANS_CONFIG_CONSUL_HOST`: hostname or IP of Consul (default to `localhost`).
- `JANS_CONFIG_CONSUL_PORT`: port of Consul (default to `8500`).
- `JANS_CONFIG_CONSUL_CONSISTENCY`: Consul consistency mode (choose one of `default`, `consistent`, or `stale`). Default to `stale` mode.
- `JANS_CONFIG_CONSUL_SCHEME`: supported Consul scheme (`http` or `https`).
- `JANS_CONFIG_CONSUL_VERIFY`: whether to verify cert or not (default to `false`).
- `JANS_CONFIG_CONSUL_CACERT_FILE`: path to Consul CA cert file (default to `/etc/certs/consul_ca.crt`). This file will be used if it exists and `JANS_CONFIG_CONSUL_VERIFY` set to `true`.
- `JANS_CONFIG_CONSUL_CERT_FILE`: path to Consul cert file (default to `/etc/certs/consul_client.crt`).
- `JANS_CONFIG_CONSUL_KEY_FILE`: path to Consul key file (default to `/etc/certs/consul_client.key`).
- `JANS_CONFIG_CONSUL_TOKEN_FILE`: path to file contains ACL token (default to `/etc/certs/consul_token`).
- `JANS_CONFIG_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `JANS_CONFIG_KUBERNETES_CONFIGMAP`: Kubernetes configmaps name (default to `gluu`).
- `JANS_CONFIG_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `JANS_SECRET_ADAPTER`: The secrets adapter, can be `vault` or `kubernetes`.
- `JANS_SECRET_VAULT_SCHEME`: supported Vault scheme (`http` or `https`).
- `JANS_SECRET_VAULT_HOST`: hostname or IP of Vault (default to `localhost`).
- `JANS_SECRET_VAULT_PORT`: port of Vault (default to `8200`).
- `JANS_SECRET_VAULT_VERIFY`: whether to verify cert or not (default to `false`).
- `JANS_SECRET_VAULT_ROLE_ID_FILE`: path to file contains Vault AppRole role ID (default to `/etc/certs/vault_role_id`).
- `JANS_SECRET_VAULT_SECRET_ID_FILE`: path to file contains Vault AppRole secret ID (default to `/etc/certs/vault_secret_id`).
- `JANS_SECRET_VAULT_CERT_FILE`: path to Vault cert file (default to `/etc/certs/vault_client.crt`).
- `JANS_SECRET_VAULT_KEY_FILE`: path to Vault key file (default to `/etc/certs/vault_client.key`).
- `JANS_SECRET_VAULT_CACERT_FILE`: path to Vault CA cert file (default to `/etc/certs/vault_ca.crt`). This file will be used if it exists and `JANS_SECRET_VAULT_VERIFY` set to `true`.
- `JANS_SECRET_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `JANS_SECRET_KUBERNETES_CONFIGMAP`: Kubernetes secrets name (default to `gluu`).
- `JANS_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `JANS_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `JANS_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `JANS_MAX_RAM_PERCENTAGE`: Value passed to Java option `-XX:MaxRAMPercentage`.
- `JANS_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, or `hybrid`; default to `ldap`).
- `JANS_PERSISTENCE_LDAP_MAPPING`: Specify data that should be saved in LDAP (one of `default`, `user`, `cache`, `site`, or `token`; default to `default`). Note this environment only takes effect when `JANS_PERSISTENCE_TYPE` is set to `hybrid`.
- `JANS_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`); required if `JANS_PERSISTENCE_TYPE` is set to `ldap` or `hybrid`.
- `JANS_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`); required if `JANS_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `JANS_COUCHBASE_USER`: Username of Couchbase server (default to `admin`); required if `JANS_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `JANS_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`); required if `JANS_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `JANS_COUCHBASE_PASSWORD_FILE`: Path to file contains Couchbase password (default to `/etc/gluu/conf/couchbase_password`); required if `JANS_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `JANS_COUCHBASE_CONN_TIMEOUT`: Connect timeout used when a bucket is opened (default to `10000` milliseconds).
- `JANS_COUCHBASE_CONN_MAX_WAIT`: Maximum time to wait before retrying connection (default to `20000` milliseconds).
- `JANS_COUCHBASE_SCAN_CONSISTENCY`: Default scan consistency; one of `not_bounded`, `request_plus`, or `statement_plus` (default to `not_bounded`).
- `JANS_JAVA_OPTIONS`: Java options passed to entrypoint, i.e. `-Xmx1024m` (default to empty-string).
- `JANS_SSL_CERT_FROM_SECRETS`: Determine whether to get SSL cert from secrets backend (default to `false`). Note that the flag will take effect only if there's no mounted `/etc/certs/gluu_https.crt` file.
