## Overview

ConfigInit is a special container used to load (generate/restore) and dump (backup) the configuration and secrets.

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-configuration-initializer/releases) for stable versions.
For bleeding-edge/unstable version, use `janssenproject/config-initializer:5.0.0_dev`.

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
- `JANS_CONFIG_KUBERNETES_CONFIGMAP`: Kubernetes configmaps name (default to `janssen`).
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
- `JANS_SECRET_KUBERNETES_CONFIGMAP`: Kubernetes secrets name (default to `janssen`).
- `JANS_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `JANS_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `JANS_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `JANS_OVERWRITE_ALL`: Overwrite all config (default to `false`).

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

1. Mount the volume into container:

    ```sh
    docker run \
        --rm \
        --network container:consul \
        -e JANS_CONFIG_ADAPTER=consul \
        -e JANS_CONFIG_CONSUL_HOST=consul \
        -e JANS_SECRET_ADAPTER=vault \
        -e JANS_SECRET_VAULT_HOST=vault \
        -v /path/to/host/volume:/opt/config-init/db \
        -v /path/to/vault_role_id.txt:/etc/certs/vault_role_id \
        -v /path/to/vault_secret_id.txt:/etc/certs/vault_secret_id \
        janssenproject/config-initializer:5.0.0_dev load
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

1. Create config map `config-generate-params`

   ```sh
   kubectl create cm config-generate-params --from-file=generate.json
   ```


1. Mount the configmap into container and apply the yaml:

    ```yaml
	apiVersion: batch/v1
	kind: Job
	metadata:
	  name: config-init-load-job
	spec:
	  template:
	    spec:
	      restartPolicy: Never
	      volumes:
	        - name: config-generate-params
	          configMap:
	            name: config-generate-params
	      containers:
	        - name: config-init-load
	          image: gluufederation/config-init:4.2.1_02
	          volumeMounts:
	            - mountPath: /opt/config-init/db/generate.json
	              name: config-generate-params
	              subPath: generate.json
	          envFrom:
	          - configMapRef:
	              name: config-cm
	          args: ["load"]
    ```

-   To restore configuration and secrets from a backup of `/path/to/host/volume/config.json` and `/path/to/host/volume/secret.json`: mount the directory as `/opt/config-init/db` inside the container:

1. Create config map `config-params` and `secret-params`:

   ```sh
   kubectl create cm config-params --from-file=config.json
   kubectl create cm secret-params --from-file=secret.json
   ```

1. Mount the configmap into container and apply the yaml:

    ```yaml
	apiVersion: batch/v1
	kind: Job
	metadata:
	  name: config-init-load-job
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
	        - name: config-init-load
	          image: gluufederation/config-init:4.2.1_02
	          volumeMounts:
	            - mountPath: /opt/config-init/db/config.json
	              name: config-params
	              subPath: config.json
	            - mountPath: /opt/config-init/db/secret.json
	              name: secret-params
	              subPath: secret.json
	          envFrom:
	          - configMapRef:
	              name: config-cm
	          args: ["load"]
	   ```


### dump

The dump command will dump all configuration and secrets from the backends saved into the `/opt/config-init/db/config.json` and `/opt/config-init/db/secret.json` files.

#### Docker

Please note that to dump this file into the host, mount a volume to the `/opt/config-init/db` directory as seen in the following example:

```sh
docker run \
    --rm \
    --network container:consul \
    -e JANS_CONFIG_ADAPTER=consul \
    -e JANS_CONFIG_CONSUL_HOST=consul \
    -e JANS_SECRET_ADAPTER=vault \
    -e JANS_SECRET_VAULT_HOST=vault \
    -v /path/to/host/volume:/opt/config-init/db \
    -v /path/to/vault_role_id.txt:/etc/certs/vault_role_id \
    -v /path/to/vault_secret_id.txt:/etc/certs/vault_secret_id \
    gluufederation/config-init:4.2.1_02 dump
```

#### Kubernetes

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: config-init-load-job
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: config-init-load
          image: gluufederation/config-init:4.2.1_02
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

`kubectl cp config-init-load-job:opt/config-init/db .`
