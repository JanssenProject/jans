[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b6e9b72a8c484d13a22b315de52a68c5)](https://www.codacy.com/gh/JanssenProject/docker-jans-certmanager/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=JanssenProject/docker-jans-certmanager&amp;utm_campaign=Badge_Grade)

## Overview

Container image to manage X.509 certificates and crypto keys in Janssen Server.
The container designed to run as one-time command (or Job in kubernetes world).

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-certmanager/releases) for stable versions.
For bleeding-edge/unstable version, use `janssenproject/certmanager:1.0.0_dev`.

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
- `CN_SECRET_GOOGLE_SECRET_VERSION_ID`:  Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
- `CN_SECRET_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
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
- `CN_SECRET_GOOGLE_SECRET_VERSION_ID`: Google Secret Manager version ID (default to `latest`).
- `CN_SECRET_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Google Secret Manager name (default to `jans`).
- `CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Google Secret Manager (default to `secret`).
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, or `hybrid`; default to `ldap`).
- `CN_PERSISTENCE_LDAP_MAPPING`: Specify data that should be saved in LDAP (one of `default`, `user`, `cache`, `site`, or `token`; default to `default`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`.
- `CN_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`); required if `CN_PERSISTENCE_TYPE` is set to `ldap` or `hybrid`.
- `CN_LDAP_USE_SSL`: Whether to use SSL connection to LDAP server (default to `true`).
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
- `CN_CONTAINER_METADATA`: The name of scheduler to pull container metadata (one of `docker` or `kubernetes`; default to `docker`).
- `GOOGLE_PROJECT_ID`: Google Project ID (default to empty string). Used when `CN_CONFIG_ADAPTER` or `CN_SECRET_ADAPTER` set to `google`.
- `GOOGLE_APPLICATION_CREDENTIALS`: Path to Google credentials JSON file (default to `/etc/jans/conf/google-credentials.json`). Used when `CN_CONFIG_ADAPTER` or `CN_SECRET_ADAPTER` set to `google`.

## Usage

### Commands

The following commands are supported by the container:

- `patch`
- `prune`

#### patch

Updates X.509 certificates and/or crypto keys related to the service.

```text
Usage: certmanager patch [OPTIONS] SERVICE

  Patch cert and/or crypto keys for the targeted service.

Options:
  --dry-run         Enable dryrun mode.
  --opts KEY:VALUE  Options for targeted service (can be set multiple times).
  -h, --help        Show this message and exit.
```

Global options:

- `--dry-run`
- `--opts`: service-dependent options, example: `--opts interval:48`

Supported services:

1.  `web` (nginx container or ingress)

    Load from existing or re-generate:

    - `/etc/certs/jans_https.crt`
    - `/etc/certs/jans_https.key`.

    Options:

    - `source`: `from-files` or empty string

1.  `auth`

    Re-generate:

    - `/etc/certs/auth-keys.json`
    - `/etc/certs/auth-keys.jks`

    Options:

    - `interval`: cryto keys expiration time (in hours)
    - `push-to-container`: whether to _push_ `auth-keys.jks` and `auth-keys.json` to auth-server containers (default to `true`)
    - `key-strategy`: key selection strategy (choose one of `OLDER`, `NEWER`, `FIRST`; default to `OLDER`)
    - `privkey-push-delay`: delay time in seconds before pushing `auth-keys.jks` to auth containers (default to `0`)
    - `privkey-push-strategy`: key selection strategy after `auth-keys.jks` is pushed to auth containers (choose one of `OLDER`, `NEWER`, `FIRST`; default to `OLDER`)
    - `sig-keys`: space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`)
    - `enc-keys`: space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`)

1.  `ldap`

    Re-generate:

    - `/etc/certs/opendj.crt`
    - `/etc/certs/opendj.key`
    - `/etc/certs/opendj.pem`
    - `/etc/certs/opendj.pkcs12`

    Options:

    - `subj-alt-name`: Subject Alternative Name (SAN) for certificate (default to `localhost`)

1.  `client-api`

    Re-generate:

    - `/etc/certs/client_api_application.crt`
    - `/etc/certs/client_api_application.key`
    - `/etc/certs/client_api_application.keystore`
    - `/etc/certs/client_api_admin.crt`
    - `/etc/certs/client_api_admin.key`
    - `/etc/certs/client_api_admin.keystore`

    Options:

    - `application-cn`: Subject alternative name for application certificate (default to `localhost`)
    - `admin-cn`: Subject alternative name for admin certificate (default to `localhost`)

#### prune

Delete expired crypto keys (if any) related to the service.

```
Usage: certmanager prune [OPTIONS] SERVICE

  Cleanup expired crypto keys for the targeted service.

Options:
  --dry-run         Enable dryrun mode.
  --opts KEY:VALUE  Options for targeted service (can be set multiple times).
  -h, --help        Show this message and exit.
```

Global options:

- `--dry-run`
- `--opts`: service-dependent options, example: `--opts interval:48`

Supported services:

1.  `auth`

    Delete expired keys (if any) from the following files:

    - `/etc/certs/auth-keys.json`
    - `/etc/certs/auth-keys.jks`

    Options:

    - `push-to-container`: whether to _push_ `auth-keys.jks` and `auth-keys.json` to auth containers (default to `true`)
    - `sig-keys`: space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`)
    - `enc-keys`: space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`)

### Examples

Docker example:

```sh
docker run \
    --rm \
    --network container:consul \
    -e CN_CONFIG_ADAPTER=consul \
    -e CN_CONFIG_CONSUL_HOST=consul \
    -e CN_SECRET_ADAPTER=vault \
    -e CN_SECRET_VAULT_HOST=vault \
    -v $PWD/vault_role_id.txt:/etc/certs/vault_role_id \
    -v $PWD/vault_secret_id.txt:/etc/certs/vault_secret_id \
    -v $PWD/ssl.crt:/etc/certs/jans_https.crt \
    -v $PWD/ssl.key:/etc/certs/jans_https.key \
    -v /var/run/docker.sock:/var/run/docker.sock \
    janssenproject/certmanager:1.0.0_dev patch web --opts source:from-files
```

Kubernetes CronJob example:

```yaml
kind: CronJob
apiVersion: batch/v1beta1
metadata:
  name: auth-key-rotation
spec:
  schedule: "0 */48 * * *"
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: auth-key-rotation
              image: janssenproject/certmanager:1.0.0_dev
              resources:
                requests:
                  memory: "300Mi"
                  cpu: "300m"
                limits:
                  memory: "300Mi"
                  cpu: "300m"
              envFrom:
                - configMapRef:
                    name: jans-config-cm
              args: ["patch", "auth", "--opts", "interval:48"]
          restartPolicy: Never
```
