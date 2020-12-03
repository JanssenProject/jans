## Overview

Container image to manage X.509 certificates and crypto keys in Janssen Server.
The container designed to run as one-time command (or Job in kubernetes world).

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-certmanager/releases) for stable versions.
For bleeding-edge/unstable version, use `janssenproject/certmanager:5.0.0_dev`.

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
- `CN_SECRET_ADAPTER`: The secrets adapter, can be `vault` or `kubernetes`.
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
- `CN_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, or `hybrid`; default to `ldap`).
- `CN_PERSISTENCE_LDAP_MAPPING`: Specify data that should be saved in LDAP (one of `default`, `user`, `cache`, `site`, or `token`; default to `default`). Note this environment only takes effect when `CN_PERSISTENCE_TYPE` is set to `hybrid`.
- `CN_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`); required if `CN_PERSISTENCE_TYPE` is set to `ldap` or `hybrid`.
- `CN_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_USER`: Username of Couchbase server (default to `admin`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_COUCHBASE_PASSWORD_FILE`: Path to file contains Couchbase password (default to `/etc/jans/conf/couchbase_password`); required if `CN_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `CN_CONTAINER_METADATA`: The name of scheduler to pull container metadata (one of `docker` or `kubernetes`; default to `docker`).

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

    - `/etc/certs/oxauth-keys.json`
    - `/etc/certs/oxauth-keys.jks`

    Options:

    - `interval`: cryto keys expiration time (in hours)
    - `push-to-container`: whether to _push_ `oxauth-keys.jks` and `oxauth-keys.json` to auth-server containers (default to `true`)
    - `key-strategy`: key selection strategy (choose one of `OLDER`, `NEWER`, `FIRST`; default to `OLDER`)
    - `privkey-push-delay`: delay time in seconds before pushing `oxauth-keys.jks` to oxAuth containers (default to `0`)

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

    - `/etc/certs/oxauth-keys.json`
    - `/etc/certs/oxauth-keys.jks`

    Options:

    - `push-to-container`: whether to _push_ `oxauth-keys.jks` and `oxauth-keys.json` to oxAuth containers (default to `true`)

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
    janssenproject/certmanager:5.0.0_dev patch web --opts source:from-files
```

Kubernetes CronJob example:

```yaml
kind: CronJob
apiVersion: batch/v1beta1
metadata:
  name: oxauth-key-rotation
spec:
  schedule: "0 */48 * * *"
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: oxauth-key-rotation
              image: janssenproject/certmanager:5.0.0_dev
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
              args: ["patch", "oxauth", "--opts", "interval:48"]
          restartPolicy: Never
```
