# config

![Version: 1.0.0-b8](https://img.shields.io/badge/Version-1.0.0--b8-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.0.0-b8](https://img.shields.io/badge/AppVersion-1.0.0--b8-informational?style=flat-square)

Configuration parameters for setup and initial configuration secret and config layers used by Janssen services.

**Homepage:** <https://jans.io/reference/container-configs/>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | support@jans.io | https://github.com/moabu |

## Source Code

* <https://jans.io/reference/container-configs/>
* <https://github.com/JanssenProject/docker-jans-configuration-manager>
* <https://github.com/JanssenFederation/cloud-native-edition/tree/master/pyjans/kubernetes/templates/helm/jans/charts/config>

## Requirements

Kubernetes: `>=v1.17.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| adminPassword | string | `"Test1234#"` | Admin password to log in to the UI. |
| city | string | `"Austin"` | City. Used for certificate creation. |
| configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| configmap.cnClientApiAdminCertCn | string | `"client-api"` | Client-api OAuth client admin certificate common name. This should be left to the default value client-api . |
| configmap.cnClientApiApplicationCertCn | string | `"client-api"` | Client-api OAuth client application certificate common name. This should be left to the default value client-api. |
| configmap.cnClientApiBindIpAddresses | string | `"*"` | Client-api bind address. This limits what ip ranges can access the client-api. This should be left as * and controlled by a NetworkPolicy |
| configmap.cnConfigGoogleSecretNamePrefix | string | `"jans"` | Prefix for Janssen configuration secret in Google Secret Manager. Defaults to jans. If left intact jans-configuration secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnConfigGoogleSecretVersionId | string | `"latest"` | Secret version to be used for configuration. Defaults to latest and should normally always stay that way. Used only when global.configAdapterName and global.configSecretAdapter is set to google. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| configmap.cnCouchbaseBucketPrefix | string | `"jans"` | The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Janssen. |
| configmap.cnCouchbaseCertFile | string | `"/etc/certs/couchbase.crt"` | Location of `couchbase.crt` used by Couchbase SDK for tls termination. The file path must end with couchbase.crt. In mTLS setups this is not required. |
| configmap.cnCouchbaseCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Couchbase certificate authority string. This must be encoded using base64. This can also be found in your couchbase UI Security > Root Certificate. In mTLS setups this is not required. |
| configmap.cnCouchbaseIndexNumReplica | int | `0` | The number of replicas per index created. Please note that the number of index nodes must be one greater than the number of index replicas. That means if your couchbase cluster only has 2 index nodes you cannot place the number of replicas to be higher than 1. |
| configmap.cnCouchbasePassword | string | `"P@ssw0rd"` | Couchbase password for the restricted user config.configmap.cnCouchbaseUser  that is often used inside the services. The password must contain one digit, one uppercase letter, one lower case letter and one symbol . |
| configmap.cnCouchbasePasswordFile | string | `"/etc/jans/conf/couchbase_password"` | The location of the Couchbase restricted user config.configmap.cnCouchbaseUser password. The file path must end with couchbase_password |
| configmap.cnCouchbaseSuperUser | string | `"admin"` | The Couchbase super user (admin) user name. This user is used during initialization only. |
| configmap.cnCouchbaseSuperUserPassword | string | `"Test1234#"` | Couchbase password for the super user config.configmap.cnCouchbaseSuperUser  that is used during the initialization process. The password must contain one digit, one uppercase letter, one lower case letter and one symbol |
| configmap.cnCouchbaseSuperUserPasswordFile | string | `"/etc/jans/conf/couchbase_superuser_password"` | The location of the Couchbase restricted user config.configmap.cnCouchbaseSuperUser password. The file path must end with couchbase_superuser_password. |
| configmap.cnCouchbaseUrl | string | `"cbjans.default.svc.cluster.local"` | Couchbase URL. Used only when global.cnPersistenceType is hybrid or couchbase. This should be in FQDN format for either remote or local Couchbase clusters. The address can be an internal address inside the kubernetes cluster |
| configmap.cnCouchbaseUser | string | `"jans"` | Couchbase restricted user. Used only when global.cnPersistenceType is hybrid or couchbase. |
| configmap.cnDocumentStoreType | string | `"JCA"` | Document store type to use for shibboleth files JCA or LOCAL. Note that if JCA is selected Apache Jackrabbit will be used. Jackrabbit also enables loading custom files across all services easily. |
| configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the google project the secret manager belongs to. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnGoogleSecretManagerPassPhrase | string | `"Test1234#"` | Passphrase for Janssen secret in Google Secret Manager. This is used for encrypting and decrypting data from the Google Secret Manager. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnGoogleSecretManagerServiceAccount | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` |  |
| configmap.cnGoogleSpannerDatabaseId | string | `""` | Google Spanner Database ID. Used only when global.cnPersistenceType is spanner. |
| configmap.cnGoogleSpannerInstanceId | string | `""` |  |
| configmap.cnJackrabbitAdminId | string | `"admin"` | Jackrabbit admin uid. |
| configmap.cnJackrabbitAdminIdFile | string | `"/etc/jans/conf/jackrabbit_admin_id"` | The location of the Jackrabbit admin uid config.cnJackrabbitAdminId. The file path must end with jackrabbit_admin_id. |
| configmap.cnJackrabbitAdminPasswordFile | string | `"/etc/jans/conf/jackrabbit_admin_password"` | The location of the Jackrabbit admin password jackrabbit.secrets.cnJackrabbitAdminPassword. The file path must end with jackrabbit_admin_password. |
| configmap.cnJackrabbitPostgresDatabaseName | string | `"jackrabbit"` | Jackrabbit postgres database name. |
| configmap.cnJackrabbitPostgresHost | string | `"postgresql.postgres.svc.cluster.local"` | Postgres url |
| configmap.cnJackrabbitPostgresPasswordFile | string | `"/etc/jans/conf/postgres_password"` | The location of the Jackrabbit postgres password file jackrabbit.secrets.cnJackrabbitPostgresPassword. The file path must end with postgres_password. |
| configmap.cnJackrabbitPostgresPort | int | `5432` | Jackrabbit Postgres port |
| configmap.cnJackrabbitPostgresUser | string | `"jackrabbit"` | Jackrabbit Postgres uid |
| configmap.cnJackrabbitSyncInterval | int | `300` | Interval between files sync (default to 300 seconds). |
| configmap.cnJackrabbitUrl | string | `"http://jackrabbit:8080"` | Jackrabbit internal url. Normally left as default. |
| configmap.cnJettyRequestHeaderSize | int | `8192` | Jetty header size in bytes in the auth server |
| configmap.cnLdapUrl | string | `"opendj:1636"` |  |
| configmap.cnMaxRamPercent | string | `"75.0"` | Value passed to Java option -XX:MaxRAMPercentage |
| configmap.cnPersistenceLdapMapping | string | `"default"` | Boolean flag to enable/disable passport chart -- Specify data that should be saved in LDAP (one of default, user, cache, site, token, or session; default to default). Note this environment only takes effect when `global.cnPersistenceType`  is set to `hybrid`. |
| configmap.cnRedisSentinelGroup | string | `""` | Redis Sentinel Group. Often set when `config.configmap.cnRedisType` is set to `SENTINEL`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisSslTruststore | string | `""` | Redis SSL truststore. Optional. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisType | string | `"STANDALONE"` | Redis service type. `STANDALONE` or `CLUSTER`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisUrl | string | `"redis.redis.svc.cluster.local:6379"` | Redis URL and port number <url>:<port>. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisUseSsl | bool | `false` | Boolean to use SSL in Redis. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnSamlEnabled | bool | `false` | Enable SAML-related features; UI menu, etc. |
| configmap.cnSecretGoogleSecretNamePrefix | string | `"jans"` | Prefix for Janssen secret in Google Secret Manager. Defaults to jans. If left jans-secret secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| configmap.cnSecretGoogleSecretVersionId | string | `"latest"` |  |
| configmap.cnSecretKubernetesSecret | string | `"cn"` | Kubernetes secret name holding configuration keys. Used when global.configSecretAdapter is set to kubernetes which is the default. |
| configmap.cnSqlDbDialect | string | `"mysql"` | SQL database dialect. `mysql` or `pgsql` |
| configmap.cnSqlDbHost | string | `"my-release-mysql.default.svc.cluster.local"` | SQL database host uri. |
| configmap.cnSqlDbName | string | `"jans"` | SQL database name. |
| configmap.cnSqlDbPort | int | `3306` | SQL database port. |
| configmap.cnSqlDbTimezone | string | `"UTC"` | SQL database timezone. |
| configmap.cnSqlDbUser | string | `"jans"` | SQL database username. |
| configmap.cnSqlPasswordFile | string | `"/etc/jans/conf/sql_password"` | SQL password file holding password from config.configmap.cnSqldbUserPassword . |
| configmap.cnSqldbUserPassword | string | `"Test1234#"` | SQL password  injected as config.configmap.cnSqlPasswordFile . |
| configmap.containerMetadataName | string | `"kubernetes"` |  |
| configmap.lbAddr | string | `""` | Loadbalancer address for AWS if the FQDN is not registered. |
| countryCode | string | `"US"` | Country code. Used for certificate creation. |
| dnsConfig | object | `{}` | Add custom dns config |
| dnsPolicy | string | `""` | Add custom dns policy |
| email | string | `"support@jans.io"` | Email address of the administrator usually. Used for certificate creation. |
| fullNameOverride | string | `""` |  |
| image.repository | string | `"janssenproject/configuration-manager"` | Image  to use for deploying. |
| image.tag | string | `"1.0.0_b8"` | Image  tag to use for deploying. |
| ldapPassword | string | `"P@ssw0rds"` | LDAP admin password if OpennDJ is used for persistence. |
| migration | object | `{"enabled":false,"migrationDataFormat":"ldif","migrationDir":"/ce-migration"}` | CE to CN Migration section |
| migration.enabled | bool | `false` | Boolean flag to enable migration from CE |
| migration.migrationDataFormat | string | `"ldif"` | migration data-format depending on persistence backend. Supported data formats are ldif, couchbase+json, spanner+avro, postgresql+json, and mysql+json. |
| migration.migrationDir | string | `"/ce-migration"` | Directory holding all migration files |
| nameOverride | string | `""` |  |
| orgName | string | `"Janssen"` | Organization name. Used for certificate creation. |
| redisPassword | string | `"P@assw0rd"` | Redis admin password if `config.configmap.cnCacheType` is set to `REDIS`. |
| resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| resources.limits.cpu | string | `"300m"` | CPU limit. |
| resources.limits.memory | string | `"300Mi"` | Memory limit. |
| resources.requests.cpu | string | `"300m"` | CPU request. |
| resources.requests.memory | string | `"300Mi"` | Memory request. |
| state | string | `"TX"` | State code. Used for certificate creation. |
| usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.5.0](https://github.com/norwoodj/helm-docs/releases/v1.5.0)
