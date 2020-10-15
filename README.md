## Overview

Container image to manage X.509 certificates and crypto keys in Gluu Server.
The container designed to run as one-time command (or Job in kubernetes world).

## Versions

See [Releases](https://github.com/GluuFederation/docker-certmanager/releases) for stable versions.
For bleeding-edge/unstable version, use `gluufederation/certmanager:4.2.2_dev`.

## Environment Variables

The following environment variables are supported by the container:

- `GLUU_CONFIG_ADAPTER`: The config backend adapter, can be `consul` (default) or `kubernetes`.
- `GLUU_CONFIG_CONSUL_HOST`: hostname or IP of Consul (default to `localhost`).
- `GLUU_CONFIG_CONSUL_PORT`: port of Consul (default to `8500`).
- `GLUU_CONFIG_CONSUL_CONSISTENCY`: Consul consistency mode (choose one of `default`, `consistent`, or `stale`). Default to `stale` mode.
- `GLUU_CONFIG_CONSUL_SCHEME`: supported Consul scheme (`http` or `https`).
- `GLUU_CONFIG_CONSUL_VERIFY`: whether to verify cert or not (default to `false`).
- `GLUU_CONFIG_CONSUL_CACERT_FILE`: path to Consul CA cert file (default to `/etc/certs/consul_ca.crt`). This file will be used if it exists and `GLUU_CONFIG_CONSUL_VERIFY` set to `true`.
- `GLUU_CONFIG_CONSUL_CERT_FILE`: path to Consul cert file (default to `/etc/certs/consul_client.crt`).
- `GLUU_CONFIG_CONSUL_KEY_FILE`: path to Consul key file (default to `/etc/certs/consul_client.key`).
- `GLUU_CONFIG_CONSUL_TOKEN_FILE`: path to file contains ACL token (default to `/etc/certs/consul_token`).
- `GLUU_CONFIG_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `GLUU_CONFIG_KUBERNETES_CONFIGMAP`: Kubernetes configmaps name (default to `gluu`).
- `GLUU_CONFIG_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `GLUU_SECRET_ADAPTER`: The secrets adapter, can be `vault` or `kubernetes`.
- `GLUU_SECRET_VAULT_SCHEME`: supported Vault scheme (`http` or `https`).
- `GLUU_SECRET_VAULT_HOST`: hostname or IP of Vault (default to `localhost`).
- `GLUU_SECRET_VAULT_PORT`: port of Vault (default to `8200`).
- `GLUU_SECRET_VAULT_VERIFY`: whether to verify cert or not (default to `false`).
- `GLUU_SECRET_VAULT_ROLE_ID_FILE`: path to file contains Vault AppRole role ID (default to `/etc/certs/vault_role_id`).
- `GLUU_SECRET_VAULT_SECRET_ID_FILE`: path to file contains Vault AppRole secret ID (default to `/etc/certs/vault_secret_id`).
- `GLUU_SECRET_VAULT_CERT_FILE`: path to Vault cert file (default to `/etc/certs/vault_client.crt`).
- `GLUU_SECRET_VAULT_KEY_FILE`: path to Vault key file (default to `/etc/certs/vault_client.key`).
- `GLUU_SECRET_VAULT_CACERT_FILE`: path to Vault CA cert file (default to `/etc/certs/vault_ca.crt`). This file will be used if it exists and `GLUU_SECRET_VAULT_VERIFY` set to `true`.
- `GLUU_SECRET_KUBERNETES_NAMESPACE`: Kubernetes namespace (default to `default`).
- `GLUU_SECRET_KUBERNETES_CONFIGMAP`: Kubernetes secrets name (default to `gluu`).
- `GLUU_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `GLUU_PERSISTENCE_TYPE`: Persistence backend being used (one of `ldap`, `couchbase`, or `hybrid`; default to `ldap`).
- `GLUU_PERSISTENCE_LDAP_MAPPING`: Specify data that should be saved in LDAP (one of `default`, `user`, `cache`, `site`, or `token`; default to `default`). Note this environment only takes effect when `GLUU_PERSISTENCE_TYPE` is set to `hybrid`.
- `GLUU_LDAP_URL`: Address and port of LDAP server (default to `localhost:1636`); required if `GLUU_PERSISTENCE_TYPE` is set to `ldap` or `hybrid`.
- `GLUU_COUCHBASE_URL`: Address of Couchbase server (default to `localhost`); required if `GLUU_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `GLUU_COUCHBASE_USER`: Username of Couchbase server (default to `admin`); required if `GLUU_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `GLUU_COUCHBASE_CERT_FILE`: Couchbase root certificate location (default to `/etc/certs/couchbase.crt`); required if `GLUU_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `GLUU_COUCHBASE_PASSWORD_FILE`: Path to file contains Couchbase password (default to `/etc/gluu/conf/couchbase_password`); required if `GLUU_PERSISTENCE_TYPE` is set to `couchbase` or `hybrid`.
- `GLUU_CONTAINER_METADATA`: The name of scheduler to pull container metadata (one of `docker` or `kubernetes`; default to `docker`).

## Usage

### Commands

The following commands are supported by the container:

- `patch`

#### patch

Updates X.509 certificates and/or crypto keys related to the service.

```text
Usage: certmanager patch [OPTIONS] SERVICE

  Patch cert and/or crypto keys for the targeted service.

Options:
  --dry-run                       Generate save certs and/or crypto keys only
                                  without saving it to external backends.
  --opts KEY:VALUE                Options for targeted service (can be set
                                  multiple times).
  -h, --help                      Show this message and exit.
```

Global options:

- `--dry-run`
- `--opts`: service-dependent options, example: `--opts interval:48`

Supported services:

1.  `web` (nginx container or ingress)

    Load from existing or re-generate:

    - `/etc/certs/gluu_https.crt`
    - `/etc/certs/gluu_https.key`.

    Options:

    - `source`: `from-files` or empty string

1.  `oxauth`

    Re-generate:

    - `/etc/certs/oxauth-keys.json`
    - `/etc/certs/oxauth-keys.jks`

    Options:

    - `interval`: cryto keys expiration time (in hours)
    - `push-to-container`: whether to _push_ `oxauth-keys.jks` and `oxauth-keys.json` to oxAuth containers (default to `true`)

1.  `oxshibboleth`

    Re-generate:

    - `/etc/certs/shibIDP.crt`
    - `/etc/certs/shibIDP.key`
    - `/etc/certs/shibIDP.jks`
    - `/etc/certs/sealer.jks`
    - `/etc/certs/sealer.kver`
    - `/etc/certs/idp-signing.crt`
    - `/etc/certs/idp-signing.key`
    - `/etc/certs/idp-encryption.crt`
    - `/etc/certs/idp-encryption.key`

1.  `oxd`

    Re-generate:

    - `/etc/certs/oxd_application.crt`
    - `/etc/certs/oxd_application.key`
    - `/etc/certs/oxd_application.keystore`
    - `/etc/certs/oxd_admin.crt`
    - `/etc/certs/oxd_admin.key`
    - `/etc/certs/oxd_admin.keystore`

    Options:

    - `application-cn`: CommonName for application certificate (default to `localhost`)
    - `admin-cn`: CommonName for admin certificate (default to `localhost`)

1.  `ldap`

    Re-generate:

    - `/etc/certs/opendj.crt`
    - `/etc/certs/opendj.key`
    - `/etc/certs/opendj.pem`
    - `/etc/certs/opendj.pkcs12`

    Options:

    - `subj-alt-name`: Subject Alternative Name (SAN) for certificate (default to `localhost`)

1.  `passport`

    Re-generate:

    - `/etc/certs/passport-rs.jks`
    - `/etc/certs/passport-rs-keys.json`
    - `/etc/certs/passport-rp.jks`
    - `/etc/certs/passport-rp-keys.json`
    - `/etc/certs/passport-rp.pem`
    - `/etc/certs/passport-sp.key`
    - `/etc/certs/passport-sp.crt`

1.  `scim`

    Re-generate:

    - `/etc/certs/scim-rs.jks`
    - `/etc/certs/scim-rs-keys.json`
    - `/etc/certs/scim-rp.jks`
    - `/etc/certs/scim-rp-keys.json`

Docker example:

```sh
docker run \
    --rm \
    --network container:consul \
    -e GLUU_CONFIG_ADAPTER=consul \
    -e GLUU_CONFIG_CONSUL_HOST=consul \
    -e GLUU_SECRET_ADAPTER=vault \
    -e GLUU_SECRET_VAULT_HOST=vault \
    -v $PWD/vault_role_id.txt:/etc/certs/vault_role_id \
    -v $PWD/vault_secret_id.txt:/etc/certs/vault_secret_id \
    -v $PWD/ssl.crt:/etc/certs/gluu_https.crt \
    -v $PWD/ssl.key:/etc/certs/gluu_https.key \
    -v /var/run/docker.sock:/var/run/docker.sock \
    gluufederation/certmanager:4.2.1_02 patch web --opts source:from-files
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
              image: gluufederation/certmanager:4.2.1_02
              resources:
                requests:
                  memory: "300Mi"
                  cpu: "300m"
                limits:
                  memory: "300Mi"
                  cpu: "300m"
              envFrom:
                - configMapRef:
                    name: gluu-config-cm
              args: ["patch", "oxauth", "--opts", "interval:48"]
          restartPolicy: Never
```
