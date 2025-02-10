# config

![Version: 0.0.0-nightly](https://img.shields.io/badge/Version-0.0.0--nightly-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.0.0-nightly](https://img.shields.io/badge/AppVersion-0.0.0--nightly-informational?style=flat-square)

Configuration parameters for setup and initial configuration secret and config layers used by Janssen services.

**Homepage:** </docker-jans-configurator>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | <support@jans.io> | <https://github.com/moabu> |

## Source Code

* </docker-jans-configurator>
* <https://github.com/JanssenProject/jans/docker-jans-configurator>

## Requirements

Kubernetes: `>=v1.22.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAnnotations | object | `{}` | Additional annotations that will be added across all resources  in the format of {cert-manager.io/issuer: "letsencrypt-prod"}. key app is taken |
| additionalLabels | object | `{}` | Additional labels that will be added across all resources definitions in the format of {mylabel: "myapp"} |
| adminPassword | string | `"Test1234#"` | Admin password to log in to the UI. |
| city | string | `"Austin"` | City. Used for certificate creation. |
| configmap.cnAwsAccessKeyId | string | `""` |  |
| configmap.cnAwsDefaultRegion | string | `"us-west-1"` |  |
| configmap.cnAwsProfile | string | `"janssen"` |  |
| configmap.cnAwsSecretAccessKey | string | `""` |  |
| configmap.cnAwsSecretsEndpointUrl | string | `""` |  |
| configmap.cnAwsSecretsNamePrefix | string | `"janssen"` |  |
| configmap.cnAwsSecretsReplicaRegions | list | `[]` |  |
| configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the google project the secret manager belongs to. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnGoogleSecretManagerServiceAccount | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Service account with roles roles/secretmanager.admin base64 encoded string. This is used often inside the services to reach the configuration layer. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnGoogleSecretNamePrefix | string | `"janssen"` | Prefix for Janssen secret in Google Secret Manager. Defaults to janssen. If left janssen-secret secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnGoogleSecretVersionId | string | `"latest"` | Secret version to be used for secret configuration. Defaults to latest and should normally always stay that way. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnJettyRequestHeaderSize | int | `8192` | Jetty header size in bytes in the auth server |
| configmap.cnMaxRamPercent | string | `"75.0"` | Value passed to Java option -XX:MaxRAMPercentage |
| configmap.cnMessageType | string | `"DISABLED"` | Message type (one of POSTGRES, REDIS, or DISABLED) |
| configmap.cnPersistenceHybridMapping | string | `"{}"` | Specify data that should be saved in each persistence (one of default, user, cache, site, token, or session; default to default). Note this environment only takes effect when `global.cnPersistenceType`  is set to `hybrid`. {  "default": "<sql>",  "user": "<sql>",  "site": "<sql>",  "cache": "<sql>",  "token": "<sql>",  "session": "<sql>", } |
| configmap.cnRedisSentinelGroup | string | `""` | Redis Sentinel Group. Often set when `config.configmap.cnRedisType` is set to `SENTINEL`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisSslTruststore | string | `""` | Redis SSL truststore. Optional. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisType | string | `"STANDALONE"` | Redis service type. `STANDALONE` or `CLUSTER`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisUrl | string | `"redis.redis.svc.cluster.local:6379"` | Redis URL and port number <url>:<port>. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisUseSsl | bool | `false` | Boolean to use SSL in Redis. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnSecretKubernetesSecret | string | `"cn"` | Kubernetes secret name holding configuration keys. Used when global.configSecretAdapter is set to kubernetes which is the default. |
| configmap.cnSqlDbDialect | string | `"mysql"` | SQL database dialect. `mysql` or `pgsql` |
| configmap.cnSqlDbHost | string | `"my-release-mysql.default.svc.cluster.local"` | SQL database host uri. |
| configmap.cnSqlDbName | string | `"jans"` | SQL database name. |
| configmap.cnSqlDbPort | int | `3306` | SQL database port. |
| configmap.cnSqlDbTimezone | string | `"UTC"` | SQL database timezone. |
| configmap.cnSqlDbUser | string | `"jans"` | SQL database username. |
| configmap.cnSqldbUserPassword | string | `"Test1234#"` | SQL password  injected in the secrets. |
| configmap.cnVaultAddr | string | `"http://localhost:8200"` | Base URL of Vault. |
| configmap.cnVaultAppRolePath | string | `"approle"` | Path to Vault AppRole. |
| configmap.cnVaultKvPath | string | `"secret"` | Path to Vault KV secrets engine. |
| configmap.cnVaultNamespace | string | `""` | Vault namespace used to access the secrets. |
| configmap.cnVaultPrefix | string | `"jans"` | Base prefix name used to access secrets. |
| configmap.cnVaultRoleId | string | `""` | Vault AppRole RoleID. |
| configmap.cnVaultRoleIdFile | string | `"/etc/certs/vault_role_id"` | Path to file contains Vault AppRole role ID. |
| configmap.cnVaultSecretId | string | `""` | Vault AppRole SecretID. |
| configmap.cnVaultSecretIdFile | string | `"/etc/certs/vault_secret_id"` | Path to file contains Vault AppRole secret ID. |
| configmap.cnVaultVerify | bool | `false` | Verify connection to Vault. |
| configmap.containerMetadataName | string | `"kubernetes"` |  |
| configmap.kcDbPassword | string | `"Test1234#"` | Password for Keycloak database access |
| configmap.kcDbSchema | string | `"keycloak"` | Keycloak database schema name (note that PostgreSQL may using "public" schema). |
| configmap.kcDbUrlDatabase | string | `"keycloak"` | Keycloak database name |
| configmap.kcDbUrlHost | string | `"mysql.kc.svc.cluster.local"` | Keycloak database host |
| configmap.kcDbUrlPort | int | `3306` | Keycloak database port (default to port 3306 for mysql). |
| configmap.kcDbUrlProperties | string | `"?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4"` | Keycloak database connection properties. If using postgresql, the value can be set to empty string. |
| configmap.kcDbUsername | string | `"keycloak"` | Keycloak database username |
| configmap.kcDbVendor | string | `"mysql"` | Keycloak database vendor name (default to MySQL server). To use PostgreSQL server, change the value to postgres. |
| configmap.kcLogLevel | string | `"INFO"` | Keycloak logging level |
| configmap.lbAddr | string | `""` | Loadbalancer address for AWS if the FQDN is not registered. |
| configmap.quarkusTransactionEnableRecovery | bool | `true` | Quarkus transaction recovery. When using MySQL, there could be issue regarding XA_RECOVER_ADMIN; refer to https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_xa-recover-admin for details. |
| countryCode | string | `"US"` | Country code. Used for certificate creation. |
| customCommand | list | `[]` | Add custom job's command. If passed, it will override the default conditional command. |
| customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| dnsConfig | object | `{}` | Add custom dns config |
| dnsPolicy | string | `""` | Add custom dns policy |
| email | string | `"support@jans.io"` | Email address of the administrator usually. Used for certificate creation. |
| fullNameOverride | string | `""` |  |
| image.pullSecrets | list | `[]` | Image Pull Secrets |
| image.repository | string | `"janssenproject/configurator"` | Image  to use for deploying. |
| image.tag | string | `"0.0.0-nightly"` | Image  tag to use for deploying. |
| lifecycle | object | `{}` |  |
| migration | object | `{"enabled":false,"migrationDataFormat":"ldif","migrationDir":"/ce-migration"}` | CE to CN Migration section |
| migration.enabled | bool | `false` | Boolean flag to enable migration from CE |
| migration.migrationDataFormat | string | `"ldif"` | migration data-format depending on persistence backend. Supported data formats are ldif, postgresql+json, and mysql+json. |
| migration.migrationDir | string | `"/ce-migration"` | Directory holding all migration files |
| nameOverride | string | `""` |  |
| orgName | string | `"Janssen"` | Organization name. Used for certificate creation. |
| redisPassword | string | `"P@assw0rd"` | Redis admin password if `config.configmap.cnCacheType` is set to `REDIS`. |
| resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| resources.limits.cpu | string | `"300m"` | CPU limit. |
| resources.limits.memory | string | `"300Mi"` | Memory limit. |
| resources.requests.cpu | string | `"300m"` | CPU request. |
| resources.requests.memory | string | `"300Mi"` | Memory request. |
| salt | string | `""` | Salt. Used for encoding/decoding sensitive data. If omitted or set to empty string, the value will be self-generated. Otherwise, a 24 alphanumeric characters are allowed as its value. |
| state | string | `"TX"` | State code. Used for certificate creation. |
| usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service. |
| usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
