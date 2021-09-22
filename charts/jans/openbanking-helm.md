# jans

![Version: 5.0.0](https://img.shields.io/badge/Version-5.0.0-informational?style=flat-square) ![AppVersion: 5.0.0](https://img.shields.io/badge/AppVersion-5.0.0-informational?style=flat-square)

Janssen Access and Identity Management OpenBanking distribution

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| moabu | support@jans.io |  |

## Source Code

* <https://jans.io>
* <https://github.com/JanssenFederation/cloud-native-edition>

## Requirements

Kubernetes: `>=v1.17.0-0`

| Repository | Name | Version |
|------------|------|---------|
|  | auth-server | 5.0.0 |
|  | cn-istio-ingress | 5.0.0 |
|  | config | 5.0.0 |
|  | config-api | 5.0.0 |
|  | nginx-ingress | 5.0.0 |
|  | persistence | 5.0.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| auth-server | object | `{"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","repository":"janssenproject/auth-server","tag":"1.0.0_b9"},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Janssen. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing. |
| auth-server.dnsConfig | object | `{}` | Add custom dns config |
| auth-server.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| auth-server.hpa.behavior | object | `{}` | Scaling Policies |
| auth-server.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| auth-server.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server.image.repository | string | `"janssenproject/auth-server"` | Image  to use for deploying. |
| auth-server.image.tag | string | `"1.0.0_b9"` | Image  tag to use for deploying. |
| auth-server.livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| auth-server.livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | Executes the python3 healthcheck. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| auth-server.readinessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the auth server if needed. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| auth-server.replicas | int | `1` | Service replica number. |
| auth-server.resources | object | `{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}}` | Resource specs. |
| auth-server.resources.limits.cpu | string | `"2500m"` | CPU limit. |
| auth-server.resources.limits.memory | string | `"2500Mi"` | Memory limit. |
| auth-server.resources.requests.cpu | string | `"2500m"` | CPU request. |
| auth-server.resources.requests.memory | string | `"2500Mi"` | Memory request. |
| auth-server.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| auth-server.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| auth-server.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| auth-server.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| auth-server.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| config | object | `{"adminPassword":"Test1234#","city":"Austin","configmap":{"cnCacheType":"NATIVE_PERSISTENCE","cnCasaEnabled":false,"cnClientApiAdminCertCn":"client-api","cnClientApiApplicationCertCn":"client-api","cnClientApiBindIpAddresses":"*","cnConfigGoogleSecretNamePrefix":"jans","cnConfigGoogleSecretVersionId":"latest","cnConfigKubernetesConfigMap":"cn","cnCouchbaseBucketPrefix":"jans","cnCouchbaseCertFile":"/etc/certs/couchbase.crt","cnCouchbaseCrt":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnCouchbaseIndexNumReplica":0,"cnCouchbasePassword":"P@ssw0rd","cnCouchbasePasswordFile":"/etc/jans/conf/couchbase_password","cnCouchbaseSuperUser":"admin","cnCouchbaseSuperUserPassword":"Test1234#","cnCouchbaseSuperUserPasswordFile":"/etc/jans/conf/couchbase_superuser_password","cnCouchbaseUrl":"cbjans.default.svc.cluster.local","cnCouchbaseUser":"jans","cnDocumentStoreType":"JCA","cnGoogleProjectId":"google-project-to-save-config-and-secrets-to","cnGoogleSecretManagerPassPhrase":"Test1234#","cnGoogleSecretManagerServiceAccount":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnGoogleSpannerDatabaseId":"","cnGoogleSpannerInstanceId":"","cnJackrabbitAdminId":"admin","cnJackrabbitAdminIdFile":"/etc/jans/conf/jackrabbit_admin_id","cnJackrabbitAdminPasswordFile":"/etc/jans/conf/jackrabbit_admin_password","cnJackrabbitPostgresDatabaseName":"jackrabbit","cnJackrabbitPostgresHost":"postgresql.postgres.svc.cluster.local","cnJackrabbitPostgresPasswordFile":"/etc/jans/conf/postgres_password","cnJackrabbitPostgresPort":5432,"cnJackrabbitPostgresUser":"jackrabbit","cnJackrabbitSyncInterval":300,"cnJackrabbitUrl":"http://jackrabbit:8080","cnJettyRequestHeaderSize":8192,"cnLdapUrl":"opendj:1636","cnMaxRamPercent":"75.0","cnPassportEnabled":false,"cnPersistenceLdapMapping":"default","cnRedisSentinelGroup":"","cnRedisSslTruststore":"","cnRedisType":"STANDALONE","cnRedisUrl":"redis.redis.svc.cluster.local:6379","cnRedisUseSsl":false,"cnSamlEnabled":false,"cnSecretGoogleSecretNamePrefix":"jans","cnSecretGoogleSecretVersionId":"latest","cnSecretKubernetesSecret":"cn","cnSqlDbDialect":"mysql","cnSqlDbHost":"my-release-mysql.default.svc.cluster.local","cnSqlDbName":"jans","cnSqlDbPort":3306,"cnSqlDbTimezone":"UTC","cnSqlDbUser":"jans","cnSqlPasswordFile":"/etc/jans/conf/sql_password","cnSqldbUserPassword":"Test1234#","lbAddr":""},"countryCode":"US","dnsConfig":{},"dnsPolicy":"","email":"support@jans.io","image":{"pullSecrets":[],"repository":"janssenproject/configuration-manager","tag":"1.0.0_b9"},"ldapPassword":"P@ssw0rds","migration":{"enabled":false,"migrationDataFormat":"ldif","migrationDir":"/ce-migration"},"orgName":"Janssen","redisPassword":"P@assw0rd","resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"state":"TX","usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Configuration parameters for setup and initial configuration secret and config layers used by Janssen services. |
| config-api | object | `{"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/config-api","tag":"1.0.0_b9"},"livenessProbe":{"httpGet":{"path":"/jans-config-api/api/v1/health/live","port":8074},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"httpGet":{"path":"jans-config-api/api/v1/health/ready","port":8074},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"400Mi"},"requests":{"cpu":"1000m","memory":"400Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Config Api endpoints can be used to configure the auth-server, which is an open-source OpenID Connect Provider (OP) and UMA Authorization Server (AS). |
| config-api.dnsConfig | object | `{}` | Add custom dns config |
| config-api.dnsPolicy | string | `""` | Add custom dns policy |
| config-api.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| config-api.hpa.behavior | object | `{}` | Scaling Policies |
| config-api.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| config-api.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| config-api.image.pullSecrets | list | `[]` | Image Pull Secrets |
| config-api.image.repository | string | `"janssenproject/config-api"` | Image  to use for deploying. |
| config-api.image.tag | string | `"1.0.0_b9"` | Image  tag to use for deploying. |
| config-api.livenessProbe | object | `{"httpGet":{"path":"/jans-config-api/api/v1/health/live","port":8074},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| config-api.livenessProbe.httpGet | object | `{"path":"/jans-config-api/api/v1/health/live","port":8074}` | http liveness probe endpoint |
| config-api.readinessProbe.httpGet | object | `{"path":"jans-config-api/api/v1/health/ready","port":8074}` | http readiness probe endpoint |
| config-api.replicas | int | `1` | Service replica number. |
| config-api.resources | object | `{"limits":{"cpu":"1000m","memory":"400Mi"},"requests":{"cpu":"1000m","memory":"400Mi"}}` | Resource specs. |
| config-api.resources.limits.cpu | string | `"1000m"` | CPU limit. |
| config-api.resources.limits.memory | string | `"400Mi"` | Memory limit. |
| config-api.resources.requests.cpu | string | `"1000m"` | CPU request. |
| config-api.resources.requests.memory | string | `"400Mi"` | Memory request. |
| config-api.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| config-api.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| config-api.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| config-api.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| config-api.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| config.adminPassword | string | `"Test1234#"` | Admin password to log in to the UI. |
| config.city | string | `"Austin"` | City. Used for certificate creation. |
| config.configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| config.configmap.cnCasaEnabled | bool | `false` | Enable Casa flag . |
| config.configmap.cnClientApiAdminCertCn | string | `"client-api"` | Client-api OAuth client admin certificate common name. This should be left to the default value client-api . |
| config.configmap.cnClientApiApplicationCertCn | string | `"client-api"` | Client-api OAuth client application certificate common name. This should be left to the default value client-api. |
| config.configmap.cnClientApiBindIpAddresses | string | `"*"` | Client-api bind address. This limits what ip ranges can access the client-api. This should be left as * and controlled by a NetworkPolicy |
| config.configmap.cnConfigGoogleSecretNamePrefix | string | `"jans"` | Prefix for Janssen configuration secret in Google Secret Manager. Defaults to jans. If left intact jans-configuration secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnConfigGoogleSecretVersionId | string | `"latest"` | Secret version to be used for configuration. Defaults to latest and should normally always stay that way. Used only when global.configAdapterName and global.configSecretAdapter is set to google. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| config.configmap.cnCouchbaseBucketPrefix | string | `"jans"` | The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Janssen. |
| config.configmap.cnCouchbaseCertFile | string | `"/etc/certs/couchbase.crt"` | Location of `couchbase.crt` used by Couchbase SDK for tls termination. The file path must end with couchbase.crt. In mTLS setups this is not required. |
| config.configmap.cnCouchbaseCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Couchbase certificate authority string. This must be encoded using base64. This can also be found in your couchbase UI Security > Root Certificate. In mTLS setups this is not required. |
| config.configmap.cnCouchbaseIndexNumReplica | int | `0` | The number of replicas per index created. Please note that the number of index nodes must be one greater than the number of index replicas. That means if your couchbase cluster only has 2 index nodes you cannot place the number of replicas to be higher than 1. |
| config.configmap.cnCouchbasePassword | string | `"P@ssw0rd"` | Couchbase password for the restricted user config.configmap.cnCouchbaseUser  that is often used inside the services. The password must contain one digit, one uppercase letter, one lower case letter and one symbol . |
| config.configmap.cnCouchbasePasswordFile | string | `"/etc/jans/conf/couchbase_password"` | The location of the Couchbase restricted user config.configmap.cnCouchbaseUser password. The file path must end with couchbase_password |
| config.configmap.cnCouchbaseSuperUser | string | `"admin"` | The Couchbase super user (admin) user name. This user is used during initialization only. |
| config.configmap.cnCouchbaseSuperUserPassword | string | `"Test1234#"` | Couchbase password for the super user config.configmap.cnCouchbaseSuperUser  that is used during the initialization process. The password must contain one digit, one uppercase letter, one lower case letter and one symbol |
| config.configmap.cnCouchbaseSuperUserPasswordFile | string | `"/etc/jans/conf/couchbase_superuser_password"` | The location of the Couchbase restricted user config.configmap.cnCouchbaseSuperUser password. The file path must end with couchbase_superuser_password. |
| config.configmap.cnCouchbaseUrl | string | `"cbjans.default.svc.cluster.local"` | Couchbase URL. Used only when global.cnPersistenceType is hybrid or couchbase. This should be in FQDN format for either remote or local Couchbase clusters. The address can be an internal address inside the kubernetes cluster |
| config.configmap.cnCouchbaseUser | string | `"jans"` | Couchbase restricted user. Used only when global.cnPersistenceType is hybrid or couchbase. |
| config.configmap.cnDocumentStoreType | string | `"JCA"` | Document store type to use for shibboleth files JCA or LOCAL. Note that if JCA is selected Apache Jackrabbit will be used. Jackrabbit also enables loading custom files across all services easily. |
| config.configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the google project the secret manager belongs to. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSecretManagerPassPhrase | string | `"Test1234#"` | Passphrase for Janssen secret in Google Secret Manager. This is used for encrypting and decrypting data from the Google Secret Manager. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSpannerDatabaseId | string | `""` | Google Spanner Database ID. Used only when global.cnPersistenceType is spanner. |
| config.configmap.cnJettyRequestHeaderSize | int | `8192` | Jetty header size in bytes in the auth server |
| config.configmap.cnMaxRamPercent | string | `"75.0"` | Value passed to Java option -XX:MaxRAMPercentage |
| config.configmap.cnPassportEnabled | bool | `false` | Boolean flag to enable/disable passport chart |
| config.configmap.cnPersistenceLdapMapping | string | `"default"` | Specify data that should be saved in LDAP (one of default, user, cache, site, token, or session; default to default). Note this environment only takes effect when `global.cnPersistenceType`  is set to `hybrid`. |
| config.configmap.cnRedisSentinelGroup | string | `""` | Redis Sentinel Group. Often set when `config.configmap.cnRedisType` is set to `SENTINEL`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisSslTruststore | string | `""` | Redis SSL truststore. Optional. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisType | string | `"STANDALONE"` | Redis service type. `STANDALONE` or `CLUSTER`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisUrl | string | `"redis.redis.svc.cluster.local:6379"` | Redis URL and port number <url>:<port>. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisUseSsl | bool | `false` | Boolean to use SSL in Redis. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnSamlEnabled | bool | `false` | Enable SAML-related features; UI menu, etc. |
| config.configmap.cnSecretGoogleSecretNamePrefix | string | `"jans"` | Prefix for Janssen secret in Google Secret Manager. Defaults to jans. If left jans-secret secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnSecretKubernetesSecret | string | `"cn"` | Kubernetes secret name holding configuration keys. Used when global.configSecretAdapter is set to kubernetes which is the default. |
| config.configmap.cnSqlDbDialect | string | `"mysql"` | SQL database dialect. `mysql` or `pgsql` |
| config.configmap.cnSqlDbHost | string | `"my-release-mysql.default.svc.cluster.local"` | SQL database host uri. |
| config.configmap.cnSqlDbName | string | `"jans"` | SQL database name. |
| config.configmap.cnSqlDbPort | int | `3306` | SQL database port. |
| config.configmap.cnSqlDbTimezone | string | `"UTC"` | SQL database timezone. |
| config.configmap.cnSqlDbUser | string | `"jans"` | SQL database username. |
| config.configmap.cnSqlPasswordFile | string | `"/etc/jans/conf/sql_password"` | SQL password file holding password from config.configmap.cnSqldbUserPassword . |
| config.configmap.cnSqldbUserPassword | string | `"Test1234#"` | SQL password  injected as config.configmap.cnSqlPasswordFile . |
| config.configmap.lbAddr | string | `""` | Loadbalancer address for AWS if the FQDN is not registered. |
| config.countryCode | string | `"US"` | Country code. Used for certificate creation. |
| config.dnsConfig | object | `{}` | Add custom dns config |
| config.dnsPolicy | string | `""` | Add custom dns policy |
| config.email | string | `"support@jans.io"` | Email address of the administrator usually. Used for certificate creation. |
| config.image.pullSecrets | list | `[]` | Image Pull Secrets |
| config.image.repository | string | `"janssenproject/configuration-manager"` | Image  to use for deploying. |
| config.image.tag | string | `"1.0.0_b9"` | Image  tag to use for deploying. |
| config.ldapPassword | string | `"P@ssw0rds"` | LDAP admin password if OpennDJ is used for persistence. |
| config.migration | object | `{"enabled":false,"migrationDataFormat":"ldif","migrationDir":"/ce-migration"}` | CE to CN Migration section |
| config.migration.enabled | bool | `false` | Boolean flag to enable migration from CE |
| config.migration.migrationDataFormat | string | `"ldif"` | migration data-format depending on persistence backend. Supported data formats are ldif, couchbase+json, spanner+avro, postgresql+json, and mysql+json. |
| config.migration.migrationDir | string | `"/ce-migration"` | Directory holding all migration files |
| config.orgName | string | `"Janssen"` | Organization name. Used for certificate creation. |
| config.redisPassword | string | `"P@assw0rd"` | Redis admin password if `config.configmap.cnCacheType` is set to `REDIS`. |
| config.resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| config.resources.limits.cpu | string | `"300m"` | CPU limit. |
| config.resources.limits.memory | string | `"300Mi"` | Memory limit. |
| config.resources.requests.cpu | string | `"300m"` | CPU request. |
| config.resources.requests.memory | string | `"300Mi"` | Memory request. |
| config.state | string | `"TX"` | State code. Used for certificate creation. |
| config.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service. |
| config.usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| config.usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| config.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| config.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| global | object | `{"alb":{"ingress":false},"auth-server":{"authServerServiceName":"auth-server","enabled":true},"auth-server-key-rotation":{"enabled":false},"awsStorageType":"io1","azureStorageAccountType":"Standard_LRS","azureStorageKind":"Managed","client-api":{"clientApiServerServiceName":"client-api","enabled":false},"cloud":{"testEnviroment":false},"cnGoogleApplicationCredentials":"/etc/jans/conf/google-credentials.json","cnJackrabbitCluster":true,"cnObExtSigningAlias":"","cnObExtSigningJwksCrt":"","cnObExtSigningJwksKey":"","cnObExtSigningJwksKeyPassPhrase":"","cnObExtSigningJwksUri":"","cnObStaticSigningKeyKid":"","cnObTransportAlias":"","cnObTransportCrt":"","cnObTransportKey":"","cnObTransportKeyPassPhrase":"","cnObTransportTrustStore":"","cnPersistenceType":"ldap","config":{"enabled":true},"config-api":{"configApiServerServiceName":"config-api","enabled":true},"configAdapterName":"kubernetes","configSecretAdapter":"kubernetes","cr-rotate":{"enabled":false},"distribution":"default","fido2":{"enabled":false},"fqdn":"demoexample.jans.io","gcePdStorageType":"pd-standard","isFqdnRegistered":false,"istio":{"enabled":false,"ingress":false,"namespace":"istio-system"},"jackrabbit":{"enabled":false,"jackRabbitServiceName":"jackrabbit"},"lbIp":"","nginx-ingress":{"enabled":true},"opendj":{"enabled":false,"ldapServiceName":"opendj"},"oxshibboleth":{"enabled":false},"persistence":{"enabled":true},"scim":{"enabled":false},"storageClass":{"allowVolumeExpansion":true,"allowedTopologies":[],"mountOptions":["debug"],"parameters":{},"provisioner":"microk8s.io/hostpath","reclaimPolicy":"Retain","volumeBindingMode":"WaitForFirstConsumer"},"upgrade":{"enabled":false},"usrEnvs":{"normal":{},"secret":{}}}` | Parameters used globally across all services helm charts. |
| global.alb.ingress | bool | `false` | Activates ALB ingress |
| global.auth-server.authServerServiceName | string | `"auth-server"` | Name of the auth-server service. Please keep it as default. |
| global.auth-server.enabled | bool | `true` | Boolean flag to enable/disable auth-server chart. You should never set this to false. |
| global.cloud.testEnviroment | bool | `false` | Boolean flag if enabled will strip resources requests and limits from all services. |
| global.cnGoogleApplicationCredentials | string | `"/etc/jans/conf/google-credentials.json"` | Base64 encoded service account. The sa must have roles/secretmanager.admin to use Google secrets and roles/spanner.databaseUser to use Spanner. |
| global.cnObExtSigningAlias | string | `""` | Open banking external signing AS Alias. This is a kid value.Used in SSA Validation, kid used while encoding a JWT sent to token URL i.e XkwIzWy44xWSlcWnMiEc8iq9s2G |
| global.cnObExtSigningJwksCrt | string | `""` | Open banking external signing jwks AS certificate authority string. Used in SSA Validation. This must be encoded using base64.. Used when `.global.cnObExtSigningJwksUri` is set. |
| global.cnObExtSigningJwksKey | string | `""` | Open banking external signing jwks AS key string. Used in SSA Validation. This must be encoded using base64. Used when `.global.cnObExtSigningJwksUri` is set. |
| global.cnObExtSigningJwksKeyPassPhrase | string | `""` | Open banking external signing jwks AS key passphrase to unlock provided key. This must be encoded using base64. Used when `.global.cnObExtSigningJwksUri` is set. |
| global.cnObExtSigningJwksUri | string | `""` | Open banking external signing jwks uri. Used in SSA Validation. |
| global.cnObStaticSigningKeyKid | string | `""` | Open banking  signing AS kid to force the AS to use a specific signing key. i.e Wy44xWSlcWnMiEc8iq9s2G |
| global.cnObTransportAlias | string | `""` | Open banking transport Alias used inside the JVM. |
| global.cnObTransportCrt | string | `""` | Open banking AS transport crt. Used in SSA Validation. This must be encoded using base64. |
| global.cnObTransportKey | string | `""` | Open banking AS transport key. Used in SSA Validation. This must be encoded using base64. |
| global.cnObTransportKeyPassPhrase | string | `""` | Open banking AS transport key passphrase to unlock AS transport key. This must be encoded using base64. |
| global.cnObTransportTrustStore | string | `""` | Open banking AS transport truststore crt. This is normally generated from the OB issuing CA, OB Root CA and Signing CA. Used when .global.cnObExtSigningJwksUri is set. Used in SSA Validation. This must be encoded using base64. |
| global.cnPersistenceType | string | `"sql"` | Persistence backend to run Janssen with ldap|couchbase|hybrid|sql|spanner. |
| global.config-api.configApiServerServiceName | string | `"config-api"` | Name of the config-api service. Please keep it as default. |
| global.config-api.enabled | bool | `true` | Boolean flag to enable/disable the config-api chart. |
| global.config.enabled | bool | `true` | Boolean flag to enable/disable the configuration chart. This normally should never be false |
| global.configAdapterName | string | `"kubernetes"` | The config backend adapter that will hold Janssen configuration layer. google|kubernetes |
| global.configSecretAdapter | string | `"kubernetes"` | The config backend adapter that will hold Janssen secret layer. google|kubernetes |
| global.distribution | string | `"openbanking"` | Janssen distributions supported are: default|openbanking. |
| global.fqdn | string | `"demoexample.jans.io"` | Fully qualified domain name to be used for Janssen installation. This address will be used to reach Janssen services. |
| global.gcePdStorageType | string | `"pd-standard"` | GCE storage kind if using Google disks |
| global.isFqdnRegistered | bool | `false` | Boolean flag to enable mapping global.lbIp  to global.fqdn inside pods on clouds that provide static ip for loadbalancers. On cloud that provide only addresses to the LB this flag will enable a script to actively scan config.configmap.lbAddr and update the hosts file inside the pods automatically. |
| global.istio.enabled | bool | `false` | Boolean flag that enables using istio side cars with Janssen services. |
| global.istio.ingress | bool | `false` | Boolean flag that enables using istio gateway for Janssen. This assumes istio ingress is installed and hence the LB is available. |
| global.istio.namespace | string | `"istio-system"` | The namespace istio is deployed in. The is normally istio-system. |
| global.lbIp | string | `""` | The Loadbalancer IP created by nginx or istio on clouds that provide static IPs. This is not needed if `global.fqdn` is globally resolvable. |
| global.nginx-ingress.enabled | bool | `true` | Boolean flag to enable/disable the nginx-ingress definitions chart. |
| global.opendj.enabled | bool | `false` | Boolean flag to enable/disable the OpenDJ  chart. |
| global.persistence.enabled | bool | `true` | Boolean flag to enable/disable the persistence chart. |
| global.storageClass | object | `{"allowVolumeExpansion":true,"allowedTopologies":[],"mountOptions":["debug"],"parameters":{},"provisioner":"microk8s.io/hostpath","reclaimPolicy":"Retain","volumeBindingMode":"WaitForFirstConsumer"}` | StorageClass section for Jackrabbit and OpenDJ charts. This is not currently used by the openbanking distribution. You may specify custom parameters as needed. |
| global.storageClass.parameters | object | `{}` | parameters: |
| global.upgrade.enabled | bool | `false` | Boolean flag used when running helm upgrade command. This allows upgrading the chart without immutable objects errors. |
| global.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service. Envs defined in global.userEnvs will be globally available to all services |
| global.usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| global.usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| nginx-ingress | object | `{"ingress":{"additionalAnnotations":{},"additionalLabels":{},"adminUiEnabled":true,"adminUiLabels":{},"authServerEnabled":true,"authServerLabels":{},"authServerProtectedRedisterLabels":{},"authServerProtectedRegister":false,"authServerProtectedToken":false,"authServerProtectedTokenLabels":{},"configApiEnabled":true,"configApiLabels":{},"fido2ConfigEnabled":false,"fido2ConfigLabels":{},"hosts":["demoexample.jans.io"],"openidConfigEnabled":true,"openidConfigLabels":{},"path":"/","scimConfigEnabled":false,"scimConfigLabels":{},"scimEnabled":false,"scimLabels":{},"tls":[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}],"u2fConfigEnabled":true,"u2fConfigLabels":{},"uma2ConfigEnabled":true,"uma2ConfigLabels":{},"webdiscoveryEnabled":true,"webdiscoveryLabels":{},"webfingerEnabled":true,"webfingerLabels":{}}}` | Nginx ingress definitions chart |
| nginx-ingress.ingress.additionalAnnotations | object | `{}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"} Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "jans/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| nginx-ingress.ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| nginx-ingress.ingress.adminUiEnabled | bool | `true` | Enable Admin UI endpoints. COMING SOON. |
| nginx-ingress.ingress.adminUiLabels | object | `{}` | Admin UI ingress resource labels. key app is taken. |
| nginx-ingress.ingress.authServerEnabled | bool | `true` | Enable Auth server endpoints /jans-auth |
| nginx-ingress.ingress.authServerLabels | object | `{}` | Auth server config ingress resource labels. key app is taken |
| nginx-ingress.ingress.authServerProtectedRedisterLabels | object | `{}` | Auth server protected token ingress resource labels. key app is taken |
| nginx-ingress.ingress.authServerProtectedRegister | bool | `false` | Enable mTLS onn Auth server endpoint /jans-auth/restv1/register |
| nginx-ingress.ingress.authServerProtectedToken | bool | `false` | Enable mTLS on Auth server endpoint /jans-auth/restv1/token |
| nginx-ingress.ingress.authServerProtectedTokenLabels | object | `{}` | Auth server protected token ingress resource labels. key app is taken |
| nginx-ingress.ingress.configApiLabels | object | `{}` | configAPI ingress resource labels. key app is taken |
| nginx-ingress.ingress.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| nginx-ingress.ingress.fido2ConfigLabels | object | `{}` | fido2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| nginx-ingress.ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| nginx-ingress.ingress.scimConfigLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimEnabled | bool | `false` | Enable SCIM endpoints /jans-scim |
| nginx-ingress.ingress.scimLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.tls | list | `[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}]` | Secrets holding HTTPS CA cert and key. |
| nginx-ingress.ingress.u2fConfigEnabled | bool | `true` | Enable endpoint /.well-known/fido-configuration |
| nginx-ingress.ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| nginx-ingress.ingress.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| nginx-ingress.ingress.uma2ConfigLabels | object | `{}` | uma2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.webdiscoveryEnabled | bool | `true` | Enable endpoint /.well-known/simple-web-discovery |
| nginx-ingress.ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| nginx-ingress.ingress.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
| nginx-ingress.ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| persistence | object | `{"dnsConfig":{},"dnsPolicy":"","image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/persistence-loader","tag":"1.0.0_b9"},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Job to generate data and intial config for Janssen Server persistence layer. |
| persistence.dnsConfig | object | `{}` | Add custom dns config |
| persistence.dnsPolicy | string | `""` | Add custom dns policy |
| persistence.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| persistence.image.pullSecrets | list | `[]` | Image Pull Secrets |
| persistence.image.repository | string | `"janssenproject/persistence-loader"` | Image  to use for deploying. |
| persistence.image.tag | string | `"1.0.0_b9"` | Image  tag to use for deploying. |
| persistence.resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| persistence.resources.limits.cpu | string | `"300m"` | CPU limit |
| persistence.resources.limits.memory | string | `"300Mi"` | Memory limit. |
| persistence.resources.requests.cpu | string | `"300m"` | CPU request. |
| persistence.resources.requests.memory | string | `"300Mi"` | Memory request. |
| persistence.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| persistence.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| persistence.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| persistence.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| persistence.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.5.0](https://github.com/norwoodj/helm-docs/releases/v1.5.0)
