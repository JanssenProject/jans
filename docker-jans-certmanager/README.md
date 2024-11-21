---
tags:
- administration
- reference
- kubernetes
- docker image
---

## Overview

Container image to manage X.509 certificates and crypto keys in Janssen Server.
The container is designed to run as a one-time command (or Job in Kubernetes world).

## Versions

See [Packages](https://github.com/orgs/JanssenProject/packages/container/package/jans%2Fcertmanager) for available versions.

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
- `CN_SECRET_ADAPTER`: The secrets' adapter, can be `vault` (default), `kubernetes`, `google`, or `aws`.
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
- `CN_SECRET_GOOGLE_SECRET_VERSION_ID`: Google Secret Manager version ID (default to `latest`).
- `CN_SECRET_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Google Secret Manager name (default to `jans`).
- `CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Google Secret Manager (default to `secret`).
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (one of `sql` or `hybrid`; default to `sql`).
- `CN_HYBRID_MAPPING`: Specify data mapping for each persistence (default to `"{}"`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`. See [hybrid mapping](#hybrid-mapping) section for details.
- `CN_CONTAINER_METADATA`: The name of scheduler to pull container metadata (one of `docker` or `kubernetes`; default to `docker`).
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

1. `web` (nginx container or ingress)

    Load from existing or re-generate:

    - `/etc/certs/jans_https.crt`
    - `/etc/certs/jans_https.key`.

    Options:

    - `source`: `from-files` or empty string
    - `valid-to`: Validity length in days (default to `365`)

2. `auth`

    Re-generate:

    - `/etc/certs/auth-keys.json`
    - `/etc/certs/auth-keys.jks`

    Options:

    - `interval`: crypto keys expiration time (in hours)
    - `push-to-container`: whether to _push_ `auth-keys.jks` and `auth-keys.json` to auth-server containers (default to `true`)
    - `key-strategy`: key selection strategy (choose one of `OLDER`, `NEWER`, `FIRST`; default to `NEWER`)
    - `privkey-push-delay`: delay time in seconds before pushing `auth-keys.jks` to auth containers (default to `0`)
    - `privkey-push-strategy`: key selection strategy after `auth-keys.jks` is pushed to auth containers (choose one of `OLDER`, `NEWER`, `FIRST`; default to `NEWER`)
    - `sig-keys`: space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`)
    - `enc-keys`: space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`)

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
              image: ghcr.io/janssenproject/jans/certmanager:0.0.0-nightly
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

### Hybrid mapping

As per v1.0.1, hybrid persistence supports all available persistence types. To configure hybrid persistence and its data mapping, follow steps below:

1. Set `CN_PERSISTENCE_TYPE` environment variable to `hybrid`

2. Set `CN_HYBRID_MAPPING` with the following format:

    ```
    {
        "default": "<sql>",
        "user": "<sql>",
        "site": "<sql>",
        "cache": "<sql>",
        "token": "<sql>",
        "session": "<sql>",
    }
    ```

    Example:

    ```
    {
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
    }
    ```
