---
tags:
  - administration
  - reference
  - kubernetes
 - docker image
---
## Overview

Docker image packaging for OpenDJ.

## Versions

See [Releases](https://github.com/GluuFederation/docker-opendj/releases) for stable versions.
For bleeding-edge/unstable version, use `gluufederation/opendj:5.0.0_dev`.

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
- `CN_SECRET_KUBERNETES_CONFIGMAP`: Kubernetes secrets name (default to `jans`).
- `CN_SECRET_KUBERNETES_USE_KUBE_CONFIG`: Load credentials from `$HOME/.kube/config`, only useful for non-container environment (default to `false`).
- `CN_SECRET_GOOGLE_SECRET_VERSION_ID`:  Janssen secret version ID in Google Secret Manager. Defaults to `latest`, which is recommended.
- `CN_SECRET_GOOGLE_SECRET_MANAGER_PASSPHRASE`: Passphrase for Janssen secret in Google Secret Manager. This is recommended to be changed and defaults to `secret`.
- `CN_SECRET_GOOGLE_SECRET_NAME_PREFIX`: Prefix for Janssen secret in Google Secret Manager. Defaults to `jans`. If left `jans-secret` secret will be created.
- `CN_WAIT_MAX_TIME`: How long the startup "health checks" should run (default to `300` seconds).
- `CN_WAIT_SLEEP_DURATION`: Delay between startup "health checks" (default to `10` seconds).
- `CN_CERT_ALT_NAME`: an additional DNS name set as Subject Alt Name in cert. If the value is not an empty string and doesn't match existing Subject Alt Name (or doesn't exist) in existing cert, then new cert will be regenerated and overwrite the one that saved in config backend. This environment variable is __required only if__ oxShibboleth is deployed, to address issue with mismatched `CN` and destination hostname while trying to connect to OpenDJ. Note, any existing containers that connect to OpenDJ must be re-deployed to download new cert.
- `CN_MAX_RAM_PERCENTAGE`: Value passed to Java option `-XX:MaxRAMPercentage`.
- `CN_JAVA_OPTIONS`: Java options passed to entrypoint, i.e. `-Xmx1024m` (default to empty-string).
- `CN_LDAP_AUTO_REPLICATE`: enable replication automatically (default to `true`).
- `CN_LDAP_REPL_CHECK_INTERVAL` : Interval between replication check in seconds (default to `10`).
- `CN_LDAP_REPL_MAX_RETRIES`: Maximum retries for auto-replication initialization (default to `30`). After max. retries is reached, the process will be stopped regardless of replication status (may need to run the process manually).
- `CN_SERF_PROFILE`: Serf timing profile (one of `local`, `lan`, or `wan`) To support setting the correct configuration values for each environment (default to `lan`).
- `CN_SERF_LOG_LEVEL`: The level of logging to show after the Serf agent has started (one of `trace`, `debug`, `info`, `warn`, `err`; default to `warn`).
- `CN_SERF_MULTICAST_DISCOVER`: Auto-discover cluster using mDNS (default to `false`). Note this requires multicast support on network environment.
- `CN_SERF_ADVERTISE_ADDR`: The address (`host:ip` format) that advertised to other Serf nodes in the cluster. If the value is empty, fallback to FQDN of container's hostname and port 7946. Note that port 7946 will always be opened inside container.
- `CN_SERF_KEY_FILE`: Absolute path to file contains encryption key for Serf (default to `/etc/jans/conf/serf-key`). See [Serf encryption key](#serf-encryption-key) for reference.
- `CN_LDAP_ADVERTISE_ADMIN_PORT`: The admin port that advertised to other OpenDJ nodes in the cluster (default to `4444`). Note that the port inside the container will use this value instead of `4444`.
- `CN_LDAP_ADVERTISE_REPLICATION_PORT`: The replication port that advertised to other OpenDJ nodes in the cluster (default to `8989`). Note that the port inside container will use this value instead of `8989`.
- `CN_LDAP_ADVERTISE_LDAPS_PORT`: The secure port that advertised to other OpenDJ nodes in the cluster (default to `1636`). Note that port 1636 will always be opened inside container.
- `CN_LDAP_ADVERTISE_LDAP_PORT`: The non-secure port that advertised to other OpenDJ nodes in the cluster (default to `1389`). Note that port 1389 will always be opened inside container.
- `CN_LDAP_USE_SSL`: Whether to use SSL connection to LDAP server (default to `true`).
- `GOOGLE_PROJECT_ID`: Google Project ID (default to empty string). Used when `CN_CONFIG_ADAPTER` or `CN_SECRET_ADAPTER` set to `google`.
- `GOOGLE_APPLICATION_CREDENTIALS`: Path to Google credentials JSON file (default to `/etc/jans/conf/google-credentials.json`). Used when `CN_CONFIG_ADAPTER` or `CN_SECRET_ADAPTER` set to `google`.

## Deployment Strategy

1. Deploy single OpenDJ instance
2. Initialize the data (see [Initializing LDAP Data](#initializing-ldap-data))
3. Scale up OpenDJ instances and auto replication will occur by default (see also [LDAP Replication](#ldap-replication))
4. Run `python3 /app/scripts/deregister_peer.py` inside the container before removing any OpenDJ instance (in Kubernetes, we can use `preStop` hook instead)

## LDAP Replication

The replication process is automatically run when the container runs, only if these conditions are met:

1. There are multiple LDAP containers running in the cluster
2. The `o=jans`, `o=site`, `o=metric` backends have not been replicated nor have entries

Check the LDAP container logs to see the result and optionally run `/opt/opendj/bin/dsreplication status` inside the container.

### Replication Using Advertised Address and Port

**WARNING:** this feature is considered alpha and should be used with caution.

By default, each container will open the following ports internally:

- 1636/tcp (LDAPS port)
- 4444/tcp (admin port)
- 8989/tcp (replication port)
- 7946/udp+tcp (Serf port)

All replication communication is done via address that resolved from container's hostname (FQDN format; i.e. `opendj-0.opendj.gluu.svc.cluster.local`).

If somehow we want to use another address and ports, it can be done by running the following steps on each container:

1.  Set the `CN_SERF_ADVERTISE_ADDR` envvar; i.e. `CN_SERF_ADVERTISE_ADDR=node1.example.com:30946`. Make sure there is port mapping between 30946 (node) and 7946 (container) for both TCP and UDP protocols. Note that `node1.example.com` address must be reachable from all containers.
1.  Set the `CN_LDAP_ADVERTISE_ADMIN_PORT` envvar; i.e. `CN_LDAP_ADVERTISE_ADMIN_PORT=30444`. Make sure there is port mapping between 30444 (node) and 30444 (container).
1.  Set the `CN_LDAP_ADVERTISE_LDAPS_PORT` envvar; i.e. `CN_LDAP_ADVERTISE_LDAPS_PORT=30636`. Make sure there is port mapping between 30636 (node) and 1636 (container).
1.  Set the `CN_LDAP_ADVERTISE_REPLICATION_PORT` envvar; i.e. `CN_LDAP_ADVERTISE_REPLICATION_PORT=30989`. Make sure there is port mapping between 30989 (node) and 30989 (container).

Check the LDAP container logs to see the result of replication and optionally run `/opt/opendj/bin/dsreplication status -X` inside the container.

## Serf Encryption Key

Each Serf agent running inside the container requires same encryption key to communicate to each other.

The key is resolved by the following order:

1.  Load from secrets (if any).

1.  Load from file (if any) where the absolute path is defined in `CN_SERF_KEY_FILE` environment variable (default to `/etc/jans/conf/serf-key`).
    The key size must be a 16, 24, or 32 bytes encoded as base64 string.

    Example of valid key string:

        # python 3.6+
        python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes()).decode())"
        # output: Z51b6PgKU1MZ75NCZOTGGoc0LP2OF3qvF6sjxHyQCYk=

    This key will be saved to secrets.

1.  Load from `serf keygen` command. This key will be saved to secrets.
