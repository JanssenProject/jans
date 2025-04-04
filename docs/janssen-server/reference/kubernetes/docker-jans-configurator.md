---
tags:
- administration
- reference
- kubernetes
- docker image
---

# Configurator

Configurator is a tool to load (generate/restore) and/or dump (backup) the configuration (consists of configmaps and secrets).

## Versions

See [Packages](https://github.com/orgs/JanssenProject/packages/container/package/jans%2Fconfigurator) for available versions.

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
- `CN_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `CN_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `GOOGLE_APPLICATION_CREDENTIALS`: Optional JSON file (contains Google credentials) that can be injected into container for authentication. Refer to https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to for supported credentials.
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
- `CN_SSL_CERT_FROM_DOMAIN`: Validate certificate is downloaded from given domain. If set to `true` (default to `false`), raise an error if cert is not downloaded. Note that the flag is ignored if mounted SSL cert and key files exist.

## Commands

The following commands are supported by the container:

- `load`
- `dump`

### load

The load command can be used either to generate/restore configmaps and secrets for the cluster.

For fresh installation, generate the initial configuration by creating `/path/to/host/volume/configuration.json` similar to example below:

```json
{
    "_configmap": {
        "hostname": "demoexample.jans.io",
        "country_code": "US",
        "state": "TX",
        "city": "Austin",
        "admin_email": "s@jans.io",
        "orgName": "Gluu Inc."
    },
    "_secret": {
        "admin_password": "S3cr3t+pass"
    }
}
```

**NOTE**: `configuration.json` has optional attributes as seen below.

1.  `_configmap`:

    - `auth_sig_keys`: space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`)
    - `auth_enc_keys`: space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`)
    - `optional_scopes`: list of optional scopes (as JSON string) that will be used (supported scopes are `redis`, `sql`; default to empty list)
    - `init_keys_exp`: the initial keys expiration time in hours (default to `48`; extra 1 hour will be added for hard limit)

2.  `_secret`:

    - `sql_password`: user's password to access SQL database (only used if `optional_scopes` list contains `sql` scope)
    - `encoded_salt`: user-defined salt (24 characters length); if omitted, salt will be generated automatically

    Example of generating `encoded_salt` value:

    ```
    # using shell script
    cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 24 | head -n 1
    # output: NFAG5g4R0NSkAZXHL8t2DScL

    # using python oneliner
    python -c 'import random, string; print("".join(random.choices(string.ascii_letters + string.digits, k=24)))'
    # ouput: HsPzqiPkRzNySWlOVui8Ilmw
    ```

To generate initial configmaps and secrets:

1.  Create config map `config-generate-params` to store the contents of `configuration.json`

    ```sh
    kubectl create cm config-generate-params --from-file=configuration.json
    ```

1.  Mount the configmap into container and apply the yaml:

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
              image: ghcr.io/janssenproject/jans/configurator:$VERSION
              volumeMounts:
                - mountPath: /app/db/configuration.json
                  name: config-generate-params
                  subPath: configuration.json
              envFrom:
              - configMapRef:
                  name: config-cm
              args: ["load"]
    ```

A successful `load` command will dump the pre-populated configuration into `/app/db/configuration.out.json`.

To restore configuration from `configuration.out.json` file:

1.  Create config map `config-dump-params`:

    ```sh
    kubectl create cm config-dump-params --from-file=configuration.out.json
    ```

2.  Mount the configmap into container and apply the yaml:

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
            - name: config-dump-params
              configMap:
                name: config-dump-params
          containers:
            - name: configurator-load
              image: ghcr.io/janssenproject/jans/configurator:$VERSION
              volumeMounts:
                - mountPath: /app/db/configuration.out.json
                  name: config-dump-params
                  subPath: configuration.out.json
              envFrom:
              - configMapRef:
                  name: config-cm
              args: ["load"]
    ```

### dump

The dump command will dump all configuration from the backends saved into the `/app/db/configuration.out.json` file.

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
          image: ghcr.io/janssenproject/jans/configurator:$VERSION
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

`kubectl cp configurator-dump-job:/app/db .`
