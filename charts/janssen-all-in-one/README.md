# janssen-all-in-one

![Version: 1.0.22](https://img.shields.io/badge/Version-1.0.22-informational?style=flat-square) ![AppVersion: 1.0.22](https://img.shields.io/badge/AppVersion-1.0.22-informational?style=flat-square)

Janssen Access and Identity Management All-in-One Chart. This chart deploys the selected janssen microservice all in one deployment.

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| moabu | <support@jans.io> |  |

## Source Code

* <https://jans.io>
* <https://github.com/JanssenProject/jans/charts/janssen>

## Requirements

Kubernetes: `>=v1.22.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| adminPassword | string | `"Test1234#"` | Admin password to log in to the UI. |
| alb.ingress | bool | `false` | switches the service to Nodeport for ALB ingress |
| auth-server | object | `{"appLoggers":{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","enableStdoutLogPrefix":"true","httpLogLevel":"INFO","httpLogTarget":"FILE","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"authEncKeys":"RSA1_5 RSA-OAEP","authSigKeys":"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512","enabled":true,"ingress":{"authServerEnabled":true,"deviceCodeEnabled":true,"firebaseMessagingEnabled":true,"openidConfigEnabled":true,"u2fConfigEnabled":true,"uma2ConfigEnabled":true,"webdiscoveryEnabled":true,"webfingerEnabled":true}}` | Parameters used globally across all services helm charts. |
| auth-server-key-rotation | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","enabled":true,"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/certmanager","tag":"1.0.22-1"},"keysLife":48,"keysPushDelay":0,"keysPushStrategy":"NEWER","keysStrategy":"NEWER","lifecycle":{},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Responsible for regenerating auth-keys per x hours |
| auth-server-key-rotation.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| auth-server-key-rotation.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| auth-server-key-rotation.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| auth-server-key-rotation.dnsConfig | object | `{}` | Add custom dns config |
| auth-server-key-rotation.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server-key-rotation.enabled | bool | `true` | Boolean flag to enable/disable the auth-server-key rotation cronjob. |
| auth-server-key-rotation.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server-key-rotation.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server-key-rotation.image.repository | string | `"ghcr.io/janssenproject/jans/certmanager"` | Image  to use for deploying. |
| auth-server-key-rotation.image.tag | string | `"1.0.22-1"` | Image  tag to use for deploying. |
| auth-server-key-rotation.keysLife | int | `48` | Auth server key rotation keys life in hours |
| auth-server-key-rotation.keysPushDelay | int | `0` | Delay (in seconds) before pushing private keys to Auth server |
| auth-server-key-rotation.keysPushStrategy | string | `"NEWER"` | Set key selection strategy after pushing private keys to Auth server (only takes effect when keysPushDelay value is greater than 0) |
| auth-server-key-rotation.keysStrategy | string | `"NEWER"` | Set key selection strategy used by Auth server |
| auth-server-key-rotation.resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| auth-server-key-rotation.resources.limits.cpu | string | `"300m"` | CPU limit. |
| auth-server-key-rotation.resources.limits.memory | string | `"300Mi"` | Memory limit. |
| auth-server-key-rotation.resources.requests.cpu | string | `"300m"` | CPU request. |
| auth-server-key-rotation.resources.requests.memory | string | `"300Mi"` | Memory request. |
| auth-server-key-rotation.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| auth-server-key-rotation.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| auth-server-key-rotation.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| auth-server-key-rotation.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| auth-server-key-rotation.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| auth-server.appLoggers | object | `{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","enableStdoutLogPrefix":"true","httpLogLevel":"INFO","httpLogTarget":"FILE","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| auth-server.appLoggers.auditStatsLogLevel | string | `"INFO"` | jans-auth_audit.log level |
| auth-server.appLoggers.auditStatsLogTarget | string | `"FILE"` | jans-auth_script.log target |
| auth-server.appLoggers.authLogLevel | string | `"INFO"` | jans-auth.log level |
| auth-server.appLoggers.authLogTarget | string | `"STDOUT"` | jans-auth.log target |
| auth-server.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e auth-server-script ===> 2022-12-20 17:49:55,744 INFO |
| auth-server.appLoggers.httpLogLevel | string | `"INFO"` | http_request_response.log level |
| auth-server.appLoggers.httpLogTarget | string | `"FILE"` | http_request_response.log target |
| auth-server.appLoggers.ldapStatsLogLevel | string | `"INFO"` | jans-auth_persistence_ldap_statistics.log level |
| auth-server.appLoggers.ldapStatsLogTarget | string | `"FILE"` | jans-auth_persistence_ldap_statistics.log target |
| auth-server.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | jans-auth_persistence_duration.log level |
| auth-server.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | jans-auth_persistence_duration.log target |
| auth-server.appLoggers.persistenceLogLevel | string | `"INFO"` | jans-auth_persistence.log level |
| auth-server.appLoggers.persistenceLogTarget | string | `"FILE"` | jans-auth_persistence.log target |
| auth-server.appLoggers.scriptLogLevel | string | `"INFO"` | jans-auth_script.log level |
| auth-server.appLoggers.scriptLogTarget | string | `"FILE"` | jans-auth_script.log target |
| auth-server.authEncKeys | string | `"RSA1_5 RSA-OAEP"` | space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`) |
| auth-server.authSigKeys | string | `"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"` | space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`) |
| auth-server.enabled | bool | `true` | Boolean flag to enable/disable auth-server chart. You should never set this to false. |
| auth-server.ingress | object | `{"authServerEnabled":true,"deviceCodeEnabled":true,"firebaseMessagingEnabled":true,"openidConfigEnabled":true,"u2fConfigEnabled":true,"uma2ConfigEnabled":true,"webdiscoveryEnabled":true,"webfingerEnabled":true}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| auth-server.ingress.authServerEnabled | bool | `true` | Enable Auth server endpoints /jans-auth |
| auth-server.ingress.deviceCodeEnabled | bool | `true` | Enable endpoint /device-code |
| auth-server.ingress.firebaseMessagingEnabled | bool | `true` | Enable endpoint /firebase-messaging-sw.js |
| auth-server.ingress.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| auth-server.ingress.u2fConfigEnabled | bool | `true` | Enable endpoint /.well-known/fido-configuration |
| auth-server.ingress.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| auth-server.ingress.webdiscoveryEnabled | bool | `true` | Enable endpoint /.well-known/simple-web-discovery |
| auth-server.ingress.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
| casa.appLoggers | object | `{"casaLogLevel":"INFO","casaLogTarget":"STDOUT","enableStdoutLogPrefix":"true","timerLogLevel":"INFO","timerLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| casa.appLoggers.casaLogLevel | string | `"INFO"` | casa.log level |
| casa.appLoggers.casaLogTarget | string | `"STDOUT"` | casa.log target |
| casa.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e casa ===> 2022-12-20 17:49:55,744 INFO |
| casa.appLoggers.timerLogLevel | string | `"INFO"` | casa timer log level |
| casa.appLoggers.timerLogTarget | string | `"FILE"` | casa timer log target |
| casa.casaServiceName | string | `"casa"` | Name of the casa service. Please keep it as default. |
| casa.enabled | bool | `true` | Boolean flag to enable/disable the casa chart. |
| casa.ingress | object | `{"casaEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| casa.ingress.casaEnabled | bool | `false` | Enable casa endpoints /casa |
| certManager.certificate.enabled | bool | `false` |  |
| certManager.certificate.issuerGroup | string | `"cert-manager.io"` |  |
| certManager.certificate.issuerKind | string | `"ClusterIssuer"` |  |
| certManager.certificate.issuerName | string | `""` |  |
| certManager.certificate.tlsSecretName | string | `"tls-certificate"` |  |
| city | string | `"Austin"` | City. Used for certificate creation. |
| cnAwsConfigFile | string | `"/etc/jans/conf/aws_config_file"` |  |
| cnAwsSecretsReplicaRegionsFile | string | `"/etc/jans/conf/aws_secrets_replica_regions"` |  |
| cnAwsSharedCredentialsFile | string | `"/etc/jans/conf/aws_shared_credential_file"` |  |
| cnCouchbasePasswordFile | string | `"/etc/jans/conf/couchbase_password"` | Path to Couchbase password file |
| cnCouchbaseSuperuserPasswordFile | string | `"/etc/jans/conf/couchbase_superuser_password"` | Path to Couchbase superuser password file |
| cnDocumentStoreType | string | `"LOCAL"` | Document store type to use for shibboleth files LOCAL. |
| cnGoogleApplicationCredentials | string | `"/etc/jans/conf/google-credentials.json"` | Base64 encoded service account. The sa must have roles/secretmanager.admin to use Google secrets and roles/spanner.databaseUser to use Spanner. Leave as this is a sensible default. |
| cnPersistenceType | string | `"sql"` | Persistence backend to run Janssen with couchbase|hybrid|sql|spanner. |
| cnPrometheusPort | string | `""` | Port used by Prometheus JMX agent (default to empty string). To enable Prometheus JMX agent, set the value to a number. |
| cnSqlPasswordFile | string | `"/etc/jans/conf/sql_password"` | Path to SQL password file |
| config-api.appLoggers | object | `{"configApiLogLevel":"INFO","configApiLogTarget":"STDOUT","enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| config-api.appLoggers.configApiLogLevel | string | `"INFO"` | configapi.log level |
| config-api.appLoggers.configApiLogTarget | string | `"STDOUT"` | configapi.log target |
| config-api.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e config-api_persistence ===> 2022-12-20 17:49:55,744 INFO |
| config-api.appLoggers.ldapStatsLogLevel | string | `"INFO"` | config-api_persistence_ldap_statistics.log level |
| config-api.appLoggers.ldapStatsLogTarget | string | `"FILE"` | config-api_persistence_ldap_statistics.log target |
| config-api.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | config-api_persistence_duration.log level |
| config-api.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | config-api_persistence_duration.log target |
| config-api.appLoggers.persistenceLogLevel | string | `"INFO"` | config-api_persistence.log level |
| config-api.appLoggers.persistenceLogTarget | string | `"FILE"` | config-api_persistence.log target |
| config-api.appLoggers.scriptLogLevel | string | `"INFO"` | config-api_script.log level |
| config-api.appLoggers.scriptLogTarget | string | `"FILE"` | config-api_script.log target |
| config-api.configApiServerServiceName | string | `"config-api"` | Name of the config-api service. Please keep it as default. |
| config-api.enabled | bool | `true` | Boolean flag to enable/disable the config-api chart. |
| config-api.ingress | object | `{"configApiEnabled":true}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| config.enabled | bool | `true` | Boolean flag to enable/disable the configuration job. This normally should never be false |
| configAdapterName | string | `"kubernetes"` | The config backend adapter that will hold Janssen configuration layer. aws|google|kubernetes |
| configSecretAdapter | string | `"kubernetes"` | The config backend adapter that will hold Janssen secret layer. aws|google|kubernetes |
| configmap.cnAwsAccessKeyId | string | `""` |  |
| configmap.cnAwsDefaultRegion | string | `"us-west-1"` |  |
| configmap.cnAwsProfile | string | `"janssen"` |  |
| configmap.cnAwsSecretAccessKey | string | `""` |  |
| configmap.cnAwsSecretsEndpointUrl | string | `""` |  |
| configmap.cnAwsSecretsNamePrefix | string | `"janssen"` |  |
| configmap.cnAwsSecretsReplicaRegions | list | `[]` |  |
| configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| configmap.cnCouchbaseBucketPrefix | string | `"jans"` | The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Janssen. |
| configmap.cnCouchbaseCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Couchbase certificate authority string. This must be encoded using base64. This can also be found in your couchbase UI Security > Root Certificate. In mTLS setups this is not required. |
| configmap.cnCouchbaseIndexNumReplica | int | `0` | The number of replicas per index created. Please note that the number of index nodes must be one greater than the number of index replicas. That means if your couchbase cluster only has 2 index nodes you cannot place the number of replicas to be higher than 1. |
| configmap.cnCouchbasePassword | string | `"P@ssw0rd"` | Couchbase password for the restricted user config.configmap.cnCouchbaseUser  that is often used inside the services. The password must contain one digit, one uppercase letter, one lower case letter and one symbol . |
| configmap.cnCouchbaseSuperUser | string | `"admin"` | The Couchbase super user (admin) username. This user is used during initialization only. |
| configmap.cnCouchbaseSuperUserPassword | string | `"Test1234#"` | Couchbase password for the superuser config.configmap.cnCouchbaseSuperUser  that is used during the initialization process. The password must contain one digit, one uppercase letter, one lower case letter and one symbol |
| configmap.cnCouchbaseUrl | string | `"cbjanssen.default.svc.cluster.local"` | Couchbase URL. Used only when cnPersistenceType is hybrid or couchbase. This should be in FQDN format for either remote or local Couchbase clusters. The address can be an internal address inside the kubernetes cluster |
| configmap.cnCouchbaseUser | string | `"janssen"` | Couchbase restricted user. Used only when cnPersistenceType is hybrid or couchbase. |
| configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the Google project the secret manager belongs to. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSecretManagerServiceAccount | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Service account with roles roles/secretmanager.admin base64 encoded string. This is used often inside the services to reach the configuration layer. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSecretNamePrefix | string | `"janssen"` | Prefix for Janssen secret in Google Secret Manager. Defaults to janssen. If left janssen-secret secret will be created. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSecretVersionId | string | `"latest"` | Secret version to be used for secret configuration. Defaults to latest and should normally always stay that way. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSpannerDatabaseId | string | `""` | Google Spanner Database ID. Used only when cnPersistenceType is spanner. |
| configmap.cnGoogleSpannerInstanceId | string | `""` | Google Spanner ID. Used only when cnPersistenceType is spanner. |
| configmap.cnJettyRequestHeaderSize | int | `8192` | Jetty header size in bytes in the auth server |
| configmap.cnLdapCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | OpenDJ certificate string. This must be encoded using base64. |
| configmap.cnLdapKey | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | OpenDJ key string. This must be encoded using base64. |
| configmap.cnMaxRamPercent | string | `"75.0"` | Value passed to Java option -XX:MaxRAMPercentage |
| configmap.cnRedisSentinelGroup | string | `""` | Redis Sentinel Group. Often set when `config.configmap.cnRedisType` is set to `SENTINEL`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisSslTruststore | string | `""` | Redis SSL truststore. Optional. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisType | string | `"STANDALONE"` | Redis service type. `STANDALONE` or `CLUSTER`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisUrl | string | `"redis.redis.svc.cluster.local:6379"` | Redis URL and port number <url>:<port>. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnRedisUseSsl | bool | `false` | Boolean to use SSL in Redis. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| configmap.cnScimProtectionMode | string | `"OAUTH"` | SCIM protection mode OAUTH|TEST|UMA |
| configmap.cnSecretKubernetesSecret | string | `"cn"` | Kubernetes secret name holding configuration keys. Used when configSecretAdapter is set to kubernetes which is the default. |
| configmap.cnSqlDbDialect | string | `"mysql"` | SQL database dialect. `mysql` or `pgsql` |
| configmap.cnSqlDbHost | string | `"my-release-mysql.default.svc.cluster.local"` | SQL database host uri. |
| configmap.cnSqlDbName | string | `"jans"` | SQL database name. |
| configmap.cnSqlDbPort | int | `3306` | SQL database port. |
| configmap.cnSqlDbSchema | string | `""` | Schema name used by SQL database (default to empty-string; if using MySQL, the schema name will be resolved as the database name, whereas in PostgreSQL the schema name will be resolved as `"public"`). |
| configmap.cnSqlDbTimezone | string | `"UTC"` | SQL database timezone. |
| configmap.cnSqlDbUser | string | `"jans"` | SQL database username. |
| configmap.cnSqldbUserPassword | string | `"Test1234#"` | SQL password  injected the secrets . |
| configmap.kcDbPassword | string | `"Test1234#"` | Password for Keycloak database access |
| configmap.kcDbSchema | string | `"keycloak"` | Keycloak database schema name (note that PostgreSQL may using "public" schema). |
| configmap.kcDbUrlDatabase | string | `"keycloak"` | Keycloak database name |
| configmap.kcDbUrlHost | string | `"mysql.kc.svc.cluster.local"` | Keycloak database host |
| configmap.kcDbUrlPort | int | `3306` | Keycloak database port (default to port 3306 for mysql). |
| configmap.kcDbUrlProperties | string | `"?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4"` | Keycloak database connection properties. If using postgresql, the value can be set to empty string. |
| configmap.kcDbUsername | string | `"keycloak"` | Keycloak database username |
| configmap.kcDbVendor | string | `"mysql"` | Keycloak database vendor name (default to MySQL server). To use PostgreSQL server, change the value to postgres. |
| configmap.kcLogLevel | string | `"INFO"` | Keycloak logging level |
| configmap.kcProxy | string | `"edge"` | Keycloak proxy mode (for most deployments, this doesn't need to be changed) |
| configmap.lbAddr | string | `""` | Load balancer address for AWS if the FQDN is not registered. |
| configmap.quarkusTransactionEnableRecovery | bool | `true` | Quarkus transaction recovery. When using MySQL, there could be issue regarding XA_RECOVER_ADMIN; refer to https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_xa-recover-admin for details. |
| countryCode | string | `"US"` | Country code. Used for certificate creation. |
| customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| dnsConfig | object | `{}` | Add custom dns config |
| dnsPolicy | string | `""` | Add custom dns policy |
| email | string | `"support@jans.io"` | Email address of the administrator usually. Used for certificate creation. |
| fido2.appLoggers | object | `{"enableStdoutLogPrefix":"true","fido2LogLevel":"INFO","fido2LogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| fido2.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e fido2 ===> 2022-12-20 17:49:55,744 INFO |
| fido2.appLoggers.fido2LogLevel | string | `"INFO"` | fido2.log level |
| fido2.appLoggers.fido2LogTarget | string | `"STDOUT"` | fido2.log target |
| fido2.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | fido2_persistence_duration.log level |
| fido2.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | fido2_persistence_duration.log target |
| fido2.appLoggers.persistenceLogLevel | string | `"INFO"` | fido2_persistence.log level |
| fido2.appLoggers.persistenceLogTarget | string | `"FILE"` | fido2_persistence.log target |
| fido2.appLoggers.scriptLogLevel | string | `"INFO"` | fido2_script.log level |
| fido2.appLoggers.scriptLogTarget | string | `"FILE"` | fido2_script.log target |
| fido2.enabled | bool | `true` | Boolean flag to enable/disable the fido2 chart. |
| fido2.fido2ServiceName | string | `"fido2"` | Name of the fido2 service. Please keep it as default. |
| fido2.ingress | object | `{"fido2ConfigEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| fido2.ingress.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| fqdn | string | `"demoexample.jans.io"` | Fully qualified domain name to be used for Janssen installation. This address will be used to reach Janssen services. |
| fullNameOverride | string | `""` |  |
| hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| hpa.behavior | object | `{}` | Scaling Policies |
| hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| image.pullSecrets | list | `[]` | Image Pull Secrets |
| image.repository | string | `"ghcr.io/janssenproject/jans/all-in-one"` | Image  to use for deploying. |
| image.tag | string | `"1.0.22-1"` | Image  tag to use for deploying. |
| isFqdnRegistered | bool | `false` | Boolean flag to enable mapping lbIp  to fqdn inside pods on clouds that provide static ip for load balancers. On cloud that provide only addresses to the LB this flag will enable a script to actively scan config.configmap.lbAddr and update the hosts file inside the pods automatically. |
| istio.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| istio.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| istio.enabled | bool | `false` | Boolean flag that enables using istio side-cars with Janssen services. |
| istio.gateways | list | `[]` | Override the gateway that can be created by default. This is used when istio ingress has already been setup and the gateway exists. |
| istio.ingress | bool | `false` | Boolean flag that enables using istio gateway for Janssen. This assumes istio ingress is installed and hence the LB is available. |
| istio.namespace | string | `"istio-system"` | The namespace istio is deployed in. The is normally istio-system. |
| istio.tlsSecretName | string | `"istio-tls-certificate"` |  |
| kcAdminCredentialsFile | string | `"/etc/jans/conf/kc_admin_creds"` | Path to file contains Keycloak admin credentials (username and password) |
| kcDbPasswordFile | string | `"/etc/jans/conf/kc_db_password"` | Path to file contains password for database access |
| lbIp | string | `"22.22.22.22"` | The Loadbalancer IP created by nginx or istio on clouds that provide static IPs. This is not needed if `fqdn` is globally resolvable. |
| lifecycle | object | `{}` |  |
| link.appLoggers | object | `{"enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","linkLogLevel":"INFO","linkLogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| link.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e link-persistence ===> 2022-12-20 17:49:55,744 INFO |
| link.appLoggers.ldapStatsLogLevel | string | `"INFO"` | cacherefresh_persistence_ldap_statistics.log level |
| link.appLoggers.ldapStatsLogTarget | string | `"FILE"` | cacherefresh_persistence_ldap_statistics.log target |
| link.appLoggers.linkLogLevel | string | `"INFO"` | cacherefresh.log level |
| link.appLoggers.linkLogTarget | string | `"STDOUT"` | cacherefresh.log target |
| link.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | cacherefresh_persistence_duration.log level |
| link.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | cacherefresh_persistence_duration.log target |
| link.appLoggers.persistenceLogLevel | string | `"INFO"` | cacherefresh_persistence.log level |
| link.appLoggers.persistenceLogTarget | string | `"FILE"` | cacherefresh_persistence.log target |
| link.appLoggers.scriptLogLevel | string | `"INFO"` | cacherefresh_script.log level |
| link.appLoggers.scriptLogTarget | string | `"FILE"` | cacherefresh_script.log target |
| link.enabled | bool | `false` | Boolean flag to enable/disable the link chart. |
| link.ingress | object | `{"linkEnabled":true}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| link.linkServiceName | string | `"link"` | Name of the link service. Please keep it as default. |
| livenessProbe | object | `{"exec":{"command":["python3","/app/jans_aio/jans_auth/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| livenessProbe.exec | object | `{"command":["python3","/app/jans_aio/jans_auth/healthcheck.py"]}` | Executes the python3 healthcheck. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| nameOverride | string | `""` |  |
| nginx-ingress.enabled | bool | `true` | Boolean flag to enable/disable the nginx-ingress definitions chart. |
| nginx-ingress.ingress.additionalAnnotations | object | `{}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"} Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "janssen/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| nginx-ingress.ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| nginx-ingress.ingress.authServerAdditionalAnnotations | object | `{}` | Auth server ingress resource additional annotations. |
| nginx-ingress.ingress.authServerLabels | object | `{}` | Auth server ingress resource labels. key app is taken |
| nginx-ingress.ingress.casaAdditionalAnnotations | object | `{}` | Casa ingress resource additional annotations. |
| nginx-ingress.ingress.casaLabels | object | `{}` | Casa ingress resource labels. key app is taken |
| nginx-ingress.ingress.configApiAdditionalAnnotations | object | `{}` | ConfigAPI ingress resource additional annotations. |
| nginx-ingress.ingress.configApiLabels | object | `{}` | configAPI ingress resource labels. key app is taken |
| nginx-ingress.ingress.deviceCodeAdditionalAnnotations | object | `{}` | device-code ingress resource additional annotations. |
| nginx-ingress.ingress.deviceCodeLabels | object | `{}` | device-code ingress resource labels. key app is taken |
| nginx-ingress.ingress.fido2ConfigAdditionalAnnotations | object | `{}` | fido2 config ingress resource additional annotations. |
| nginx-ingress.ingress.fido2ConfigLabels | object | `{}` | fido2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.firebaseMessagingAdditionalAnnotations | object | `{}` | Firebase Messaging ingress resource additional annotations. |
| nginx-ingress.ingress.firebaseMessagingLabels | object | `{}` | Firebase Messaging ingress resource labels. key app is taken |
| nginx-ingress.ingress.ingressClassName | string | `"nginx"` |  |
| nginx-ingress.ingress.openidAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| nginx-ingress.ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| nginx-ingress.ingress.path | string | `"/"` |  |
| nginx-ingress.ingress.samlAdditionalAnnotations | object | `{}` | SAML ingress resource additional annotations. |
| nginx-ingress.ingress.samlLabels | object | `{}` | SAML config ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimAdditionalAnnotations | object | `{}` | SCIM ingress resource additional annotations. |
| nginx-ingress.ingress.scimConfigAdditionalAnnotations | object | `{}` | SCIM config ingress resource additional annotations. |
| nginx-ingress.ingress.scimConfigLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.tlsSecretName | string | `"tls-certificate"` | Secrets holding HTTPS CA cert and key. |
| nginx-ingress.ingress.u2fAdditionalAnnotations | object | `{}` | u2f config ingress resource additional annotations. |
| nginx-ingress.ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| nginx-ingress.ingress.uma2AdditionalAnnotations | object | `{}` | uma2 config ingress resource additional annotations. |
| nginx-ingress.ingress.uma2ConfigLabels | object | `{}` | uma2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.webdiscoveryAdditionalAnnotations | object | `{}` | webdiscovery ingress resource additional annotations. |
| nginx-ingress.ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| nginx-ingress.ingress.webfingerAdditionalAnnotations | object | `{}` | webfinger ingress resource additional annotations. |
| nginx-ingress.ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| orgName | string | `"Janssen"` | Organization name. Used for certificate creation. |
| pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| persistence.enabled | bool | `true` | Boolean flag to enable/disable the persistence job. |
| readinessProbe | object | `{"exec":{"command":["python3","/app/jans_aio/jans_auth/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the auth server if needed. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| redisPassword | string | `"P@assw0rd"` | Redis admin password if `configmap.cnCacheType` is set to `REDIS`. |
| replicas | int | `1` | Service replica number. |
| resources | object | `{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}}` | Resource specs. |
| resources.limits.cpu | string | `"2500m"` | CPU limit. |
| resources.limits.memory | string | `"2500Mi"` | Memory limit. |
| resources.requests.cpu | string | `"2500m"` | CPU request. |
| resources.requests.memory | string | `"2500Mi"` | Memory request. |
| salt | string | `""` | Salt. Used for encoding/decoding sensitive data. If omitted or set to empty string, the value will be self-generated. Otherwise, a 24 alphanumeric characters are allowed as its value. |
| saml.enabled | bool | `false` | Boolean flag to enable/disable the saml chart. |
| saml.ingress | object | `{"samlEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| saml.samlServiceName | string | `"saml"` | Name of the saml service. Please keep it as default. |
| scim.appLoggers | object | `{"enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scimLogLevel":"INFO","scimLogTarget":"STDOUT","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| scim.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e jans-scim ===> 2022-12-20 17:49:55,744 INFO |
| scim.appLoggers.ldapStatsLogLevel | string | `"INFO"` | jans-scim_persistence_ldap_statistics.log level |
| scim.appLoggers.ldapStatsLogTarget | string | `"FILE"` | jans-scim_persistence_ldap_statistics.log target |
| scim.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | jans-scim_persistence_duration.log level |
| scim.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | jans-scim_persistence_duration.log target |
| scim.appLoggers.persistenceLogLevel | string | `"INFO"` | jans-scim_persistence.log level |
| scim.appLoggers.persistenceLogTarget | string | `"FILE"` | jans-scim_persistence.log target |
| scim.appLoggers.scimLogLevel | string | `"INFO"` | jans-scim.log level |
| scim.appLoggers.scimLogTarget | string | `"STDOUT"` | jans-scim.log target |
| scim.appLoggers.scriptLogLevel | string | `"INFO"` | jans-scim_script.log level |
| scim.appLoggers.scriptLogTarget | string | `"FILE"` | jans-scim_script.log target |
| scim.enabled | bool | `true` | Boolean flag to enable/disable the SCIM chart. |
| scim.ingress | object | `{"scimConfigEnabled":false,"scimEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| scim.ingress.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| scim.ingress.scimEnabled | bool | `false` | Enable SCIM endpoints /jans-scim |
| scim.scimServiceName | string | `"scim"` | Name of the scim service. Please keep it as default. |
| service.name | string | `"http-aio"` | The name of the aio port within the aio service. Please keep it as default. |
| service.port | int | `8080` | Port of the fido2 service. Please keep it as default. |
| service.sessionAffinity | string | `"None"` | Default set to None If you want to make sure that connections from a particular client are passed to the same Pod each time, you can select the session affinity based on the client's IP addresses by setting this to ClientIP |
| service.sessionAffinityConfig | object | `{"clientIP":{"timeoutSeconds":10800}}` | the maximum session sticky time if sessionAffinity is ClientIP |
| state | string | `"TX"` | State code. Used for certificate creation. |
| testEnviroment | bool | `false` | Boolean flag if enabled will strip resources requests and limits from all services. |
| topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
