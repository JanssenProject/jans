---
tags:
- administration
- reference
- kubernetes
- docker image
---

## Overview

Configuration manager is a special container used to load (generate/restore) and dump (backup) the configuration and secrets.

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-configurator/releases) for stable versions.
For bleeding-edge/unstable version, use `janssenproject/configurator:1.0.1_dev`.

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
- `CN_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `CN_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `GOOGLE_APPLICATION_CREDENTIALS`: JSON file (contains Google credentials) that should be injected into container.
- `GOOGLE_PROJECT_ID`: ID of Google project.
- `CN_GOOGLE_SECRET_VERSION_ID`: Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
- `CN_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
- `CN_CONFIGURATION_SKIP_INITIALIZED`: skip initialization if backend already initialized (default to `false`).
- `CN_AWS_SECRETS_ENDPOINT_URL`: The URL of AWS secretsmanager service (if omitted, will use the one in specified region).
- `CN_AWS_SECRETS_PREFIX`: The prefix name of the secrets (default to `jans`).
- `CN_AWS_SECRETS_REPLICA_FILE`: The location of file contains replica regions definition (if any). This file is mostly used in primary region. Example of contents of the file: `[{"Region": "us-west-1"}]`.
- `AWS_DEFAULT_REGION`: The default AWS Region to use, for example, `us-west-1` or `us-west-2`.
- `AWS_SHARED_CREDENTIALS_FILE`: The location of the shared credentials file used by the client (see https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).
- `AWS_CONFIG_FILE`: The location of the config file used by the client (see https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).
- `AWS_PROFILE`: The default profile to use, if any.

## Commands

The following commands are supported by the container:

- `load`
- `dump`

### load

The load command can be used either to generate or restore config and secret for the cluster.

#### Docker

1. To generate the initial configuration and secret, create `/path/to/host/volume/generate.json` similar to example below:

    ```json
    {
        "hostname": "demoexample.jans.io",
        "country_code": "US",
        "state": "TX",
        "city": "Austin",
        "admin_pw": "S3cr3t+pass",
        "ldap_pw": "S3cr3t+pass",
        "email": "s@jans.io",
        "org_name": "Gluu Inc."
    }
    ```

    **NOTE**: `generate.json` has optional attributes to generate oxAuth signing and encryption keys based on specific algorithms.

    - `auth_sig_keys`: space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`)
    - `auth_enc_keys`: space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`)
    - `optional_scopes`: list of scopes that will be used (supported scopes are `ldap`, `scim`, `fido2`, `couchbase`, `redis`, `sql`, `casa`; default to empty list)
    - `ldap_pw`: user's password to access LDAP database (only used if `optional_scopes` list contains `ldap` scope)
    - `sql_pw`: user's password to access SQL database (only used if `optional_scopes` list contains `sql` scope)
    - `couchbase_pw`: user's password to access Couchbase database (only used if `optional_scopes` list contains `couchbase` scope)
    - `couchbase_superuser_pw`: superusers password to access Couchbase database (only used if `optional_scopes` list contains `couchbase` scope)

2. Mount the volume into container:

    ```sh
    docker run \
        --rm \
        --network container:consul \
        -e CN_CONFIG_ADAPTER=consul \
        -e CN_CONFIG_CONSUL_HOST=consul \
        -e CN_SECRET_ADAPTER=vault \
        -e CN_SECRET_VAULT_HOST=vault \
        -v /path/to/host/volume:/app/db \
        -v /path/to/vault_role_id.txt:/etc/certs/vault_role_id \
        -v /path/to/vault_secret_id.txt:/etc/certs/vault_secret_id \
        janssenproject/configurator:1.0.1_dev load
    ```

#### Kubernetes

1. To generate the initial configuration and secret, create `/path/to/host/volume/generate.json` similar to example below:

    ```json
    {
        "hostname": "demoexample.jans.io",
        "country_code": "US",
        "state": "TX",
        "city": "Austin",
        "admin_pw": "S3cr3t+pass",
        "ldap_pw": "S3cr3t+pass",
        "email": "s@gluu.local",
        "org_name": "Gluu Inc."
    }
    ```

    **NOTE**: `generate.json` has optional attributes to generate oxAuth signing and encryption keys based on specific algorithms.

    - `auth_sig_keys`: space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`)
    - `auth_enc_keys`: space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`)

2. Create config map `config-generate-params`

   ```sh
   kubectl create cm config-generate-params --from-file=generate.json
   ```

3. Mount the configmap into container and apply the yaml:

    ```yaml
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: configurator-load-job
    spec:
      template:
        spec:
          restartPolicy: Never
          volumes:
            - name: config-generate-params
              configMap:
                name: config-generate-params
          containers:
            - name: configurator-load
              image: janssenproject/configurator:1.0.1_dev
              volumeMounts:
                - mountPath: /app/db/generate.json
                  name: config-generate-params
                  subPath: generate.json
              envFrom:
              - configMapRef:
                  name: config-cm
              args: ["load"]
    ```

-   To restore configuration and secrets from a backup of `/path/to/host/volume/config.json` and `/path/to/host/volume/secret.json`: mount the directory as `/app/db` inside the container:

1. Create config map `config-params` and `secret-params`:

   ```sh
   kubectl create cm config-params --from-file=config.json
   kubectl create cm secret-params --from-file=secret.json
   ```

2. Mount the configmap into container and apply the yaml:

    ```yaml
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: configurator-load-job
    spec:
      template:
        spec:
          restartPolicy: Never
          volumes:
            - name: config-params
              configMap:
                name: config-params
               - name: secret-params
              configMap:
                name: secret-params
          containers:
            - name: configurator-load
              image: janssenproject/configurator:1.0.1_dev
              volumeMounts:
                - mountPath: /app/db/config.json
                  name: config-params
                  subPath: config.json
                - mountPath: /app/db/secret.json
                  name: secret-params
                  subPath: secret.json
              envFrom:
              - configMapRef:
                  name: config-cm
              args: ["load"]
       ```


### dump

The dump command will dump all configuration and secrets from the backends saved into the `/app/db/config.json` and `/app/db/secret.json` files.

#### Docker

Please note that to dump this file into the host, mount a volume to the `/app/db` directory as seen in the following example:

```sh
docker run \
    --rm \
    --network container:consul \
    -e CN_CONFIG_ADAPTER=consul \
    -e CN_CONFIG_CONSUL_HOST=consul \
    -e CN_SECRET_ADAPTER=vault \
    -e CN_SECRET_VAULT_HOST=vault \
    -v /path/to/host/volume:/app/db \
    -v /path/to/vault_role_id.txt:/etc/certs/vault_role_id \
    -v /path/to/vault_secret_id.txt:/etc/certs/vault_secret_id \
    janssenproject/configurator:1.0.1_dev dump
```

#### Kubernetes

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: configurator-dump-job
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: configurator-dump-job
          image: janssenproject/configurator:1.0.1_dev
          command:
            - /bin/sh
            - -c
            - |
                /app/scripts/entrypoint.sh dump
                sleep 300
          envFrom:
          - configMapRef:
              name: config-cm
```

Copy over the files to host

`kubectl cp config-init-load-job:/app/db .`

