# janssen

![Version: 1.1.0-dev](https://img.shields.io/badge/Version-1.1.0--dev-informational?style=flat-square) ![AppVersion: 1.1.0-dev](https://img.shields.io/badge/AppVersion-1.1.0--dev-informational?style=flat-square)

Janssen Access and Identity Management Microservices Chart. This chart deploys each janssen microservice as a separate deployment.

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

| Repository | Name | Version |
|------------|------|---------|
|  | auth-server | 1.1.0-dev |
|  | auth-server-key-rotation | 1.1.0-dev |
|  | casa | 1.1.0-dev |
|  | cn-istio-ingress | 1.1.0-dev |
|  | config | 1.1.0-dev |
|  | config-api | 1.1.0-dev |
|  | fido2 | 1.1.0-dev |
|  | link | 1.1.0-dev |
|  | nginx-ingress | 1.1.0-dev |
|  | opendj | 1.1.0-dev |
|  | persistence | 1.1.0-dev |
|  | saml | 1.1.0-dev |
|  | scim | 1.1.0-dev |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| auth-server | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/auth-server","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Janssen. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing. |
| auth-server-key-rotation | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/certmanager","tag":"1.1.0_dev"},"keysLife":48,"keysPushDelay":0,"keysPushStrategy":"NEWER","keysStrategy":"NEWER","lifecycle":{},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Responsible for regenerating auth-keys per x hours |
| auth-server-key-rotation.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| auth-server-key-rotation.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| auth-server-key-rotation.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| auth-server-key-rotation.dnsConfig | object | `{}` | Add custom dns config |
| auth-server-key-rotation.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server-key-rotation.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server-key-rotation.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server-key-rotation.image.repository | string | `"ghcr.io/janssenproject/jans/certmanager"` | Image  to use for deploying. |
| auth-server-key-rotation.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
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
| auth-server.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| auth-server.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| auth-server.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| auth-server.dnsConfig | object | `{}` | Add custom dns config |
| auth-server.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| auth-server.hpa.behavior | object | `{}` | Scaling Policies |
| auth-server.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| auth-server.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server.image.repository | string | `"ghcr.io/janssenproject/jans/auth-server"` | Image  to use for deploying. |
| auth-server.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| auth-server.livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| auth-server.livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | Executes the python3 healthcheck. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| auth-server.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| auth-server.readinessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the auth server if needed. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| auth-server.replicas | int | `1` | Service replica number. |
| auth-server.resources | object | `{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}}` | Resource specs. |
| auth-server.resources.limits.cpu | string | `"2500m"` | CPU limit. |
| auth-server.resources.limits.memory | string | `"2500Mi"` | Memory limit. |
| auth-server.resources.requests.cpu | string | `"2500m"` | CPU request. |
| auth-server.resources.requests.memory | string | `"2500Mi"` | Memory request. |
| auth-server.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| auth-server.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| auth-server.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| auth-server.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| auth-server.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| auth-server.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| casa | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/casa","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"httpGet":{"path":"/jans-casa/health-check","port":"http-casa"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"httpGet":{"path":"/jans-casa/health-check","port":"http-casa"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Janssen Casa ("Casa") is a self-service web portal for end-users to manage authentication and authorization preferences for their account in a Janssen Auth Server. |
| casa.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| casa.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| casa.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| casa.dnsConfig | object | `{}` | Add custom dns config |
| casa.dnsPolicy | string | `""` | Add custom dns policy |
| casa.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| casa.hpa.behavior | object | `{}` | Scaling Policies |
| casa.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| casa.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| casa.image.pullSecrets | list | `[]` | Image Pull Secrets |
| casa.image.repository | string | `"ghcr.io/janssenproject/jans/casa"` | Image  to use for deploying. |
| casa.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| casa.livenessProbe | object | `{"httpGet":{"path":"/jans-casa/health-check","port":"http-casa"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the liveness healthcheck for casa if needed. |
| casa.livenessProbe.httpGet.path | string | `"/jans-casa/health-check"` | http liveness probe endpoint |
| casa.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| casa.readinessProbe | object | `{"httpGet":{"path":"/jans-casa/health-check","port":"http-casa"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the readiness healthcheck for the casa if needed. |
| casa.readinessProbe.httpGet.path | string | `"/jans-casa/health-check"` | http readiness probe endpoint |
| casa.replicas | int | `1` | Service replica number. |
| casa.resources | object | `{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}}` | Resource specs. |
| casa.resources.limits.cpu | string | `"500m"` | CPU limit. |
| casa.resources.limits.memory | string | `"500Mi"` | Memory limit. |
| casa.resources.requests.cpu | string | `"500m"` | CPU request. |
| casa.resources.requests.memory | string | `"500Mi"` | Memory request. |
| casa.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| casa.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| casa.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| casa.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| casa.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| casa.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| config | object | `{"additionalAnnotations":{},"additionalLabels":{},"adminPassword":"Test1234#","city":"Austin","configmap":{"cnAwsAccessKeyId":"","cnAwsDefaultRegion":"us-west-1","cnAwsProfile":"janssen","cnAwsSecretAccessKey":"","cnAwsSecretsEndpointUrl":"","cnAwsSecretsNamePrefix":"janssen","cnAwsSecretsReplicaRegions":[],"cnCacheType":"NATIVE_PERSISTENCE","cnConfigKubernetesConfigMap":"cn","cnCouchbaseBucketPrefix":"jans","cnCouchbaseCrt":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnCouchbaseIndexNumReplica":0,"cnCouchbasePassword":"P@ssw0rd","cnCouchbaseSuperUser":"admin","cnCouchbaseSuperUserPassword":"Test1234#","cnCouchbaseUrl":"cbjanssen.default.svc.cluster.local","cnCouchbaseUser":"janssen","cnGoogleProjectId":"google-project-to-save-config-and-secrets-to","cnGoogleSecretManagerServiceAccount":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnGoogleSecretNamePrefix":"janssen","cnGoogleSecretVersionId":"latest","cnGoogleSpannerDatabaseId":"","cnGoogleSpannerInstanceId":"","cnJettyRequestHeaderSize":8192,"cnLdapCrt":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnLdapKey":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnLdapUrl":"opendj:1636","cnMaxRamPercent":"75.0","cnMessageType":"DISABLED","cnOpaUrl":"http://opa.opa.svc.cluster.cluster.local:8181/v1","cnPersistenceHybridMapping":"{}","cnRedisSentinelGroup":"","cnRedisSslTruststore":"","cnRedisType":"STANDALONE","cnRedisUrl":"redis.redis.svc.cluster.local:6379","cnRedisUseSsl":false,"cnScimProtectionMode":"OAUTH","cnSecretKubernetesSecret":"cn","cnSqlDbDialect":"mysql","cnSqlDbHost":"my-release-mysql.default.svc.cluster.local","cnSqlDbName":"jans","cnSqlDbPort":3306,"cnSqlDbSchema":"","cnSqlDbTimezone":"UTC","cnSqlDbUser":"jans","cnSqldbUserPassword":"Test1234#","cnVaultAddr":"http://localhost:8200","cnVaultAppRolePath":"approle","cnVaultKvPath":"secret","cnVaultNamespace":"","cnVaultPrefix":"jans","cnVaultRoleId":"","cnVaultRoleIdFile":"/etc/certs/vault_role_id","cnVaultSecretId":"","cnVaultSecretIdFile":"/etc/certs/vault_secret_id","cnVaultVerify":false,"kcDbPassword":"Test1234#","kcDbSchema":"keycloak","kcDbUrlDatabase":"keycloak","kcDbUrlHost":"mysql.kc.svc.cluster.local","kcDbUrlPort":3306,"kcDbUrlProperties":"?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4","kcDbUsername":"keycloak","kcDbVendor":"mysql","kcLogLevel":"INFO","kcProxy":"edge","lbAddr":"","quarkusTransactionEnableRecovery":true},"countryCode":"US","customScripts":[],"dnsConfig":{},"dnsPolicy":"","email":"support@jans.io","image":{"pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/configurator","tag":"1.1.0_dev"},"ldapPassword":"P@ssw0rds","ldapTruststorePassword":"changeit","lifecycle":{},"orgName":"Janssen","redisPassword":"P@assw0rd","resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"salt":"","state":"TX","usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Configuration parameters for setup and initial configuration secret and config layers used by Janssen services. |
| config-api | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/config-api","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"httpGet":{"path":"/jans-config-api/api/v1/health/live","port":8074},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"httpGet":{"path":"jans-config-api/api/v1/health/ready","port":8074},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"1000Mi"},"requests":{"cpu":"1000m","memory":"1000Mi"}},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Config Api endpoints can be used to configure the auth-server, which is an open-source OpenID Connect Provider (OP) and UMA Authorization Server (AS). |
| config-api.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| config-api.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| config-api.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| config-api.dnsConfig | object | `{}` | Add custom dns config |
| config-api.dnsPolicy | string | `""` | Add custom dns policy |
| config-api.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| config-api.hpa.behavior | object | `{}` | Scaling Policies |
| config-api.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| config-api.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| config-api.image.pullSecrets | list | `[]` | Image Pull Secrets |
| config-api.image.repository | string | `"ghcr.io/janssenproject/jans/config-api"` | Image  to use for deploying. |
| config-api.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| config-api.livenessProbe | object | `{"httpGet":{"path":"/jans-config-api/api/v1/health/live","port":8074},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| config-api.livenessProbe.httpGet | object | `{"path":"/jans-config-api/api/v1/health/live","port":8074}` | http liveness probe endpoint |
| config-api.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| config-api.readinessProbe.httpGet | object | `{"path":"jans-config-api/api/v1/health/ready","port":8074}` | http readiness probe endpoint |
| config-api.replicas | int | `1` | Service replica number. |
| config-api.resources | object | `{"limits":{"cpu":"1000m","memory":"1000Mi"},"requests":{"cpu":"1000m","memory":"1000Mi"}}` | Resource specs. |
| config-api.resources.limits.cpu | string | `"1000m"` | CPU limit. |
| config-api.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| config-api.resources.requests.cpu | string | `"1000m"` | CPU request. |
| config-api.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| config-api.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| config-api.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| config-api.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| config-api.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| config-api.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| config-api.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| config.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| config.adminPassword | string | `"Test1234#"` | Admin password to log in to the UI. |
| config.city | string | `"Austin"` | City. Used for certificate creation. |
| config.configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| config.configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| config.configmap.cnCouchbaseBucketPrefix | string | `"jans"` | The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Janssen. |
| config.configmap.cnCouchbaseCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Couchbase certificate authority string. This must be encoded using base64. This can also be found in your couchbase UI Security > Root Certificate. In mTLS setups this is not required. |
| config.configmap.cnCouchbaseIndexNumReplica | int | `0` | The number of replicas per index created. Please note that the number of index nodes must be one greater than the number of index replicas. That means if your couchbase cluster only has 2 index nodes you cannot place the number of replicas to be higher than 1. |
| config.configmap.cnCouchbasePassword | string | `"P@ssw0rd"` | Couchbase password for the restricted user config.configmap.cnCouchbaseUser  that is often used inside the services. The password must contain one digit, one uppercase letter, one lower case letter and one symbol . |
| config.configmap.cnCouchbaseSuperUser | string | `"admin"` | The Couchbase super user (admin) username. This user is used during initialization only. |
| config.configmap.cnCouchbaseSuperUserPassword | string | `"Test1234#"` | Couchbase password for the superuser config.configmap.cnCouchbaseSuperUser  that is used during the initialization process. The password must contain one digit, one uppercase letter, one lower case letter and one symbol |
| config.configmap.cnCouchbaseUrl | string | `"cbjanssen.default.svc.cluster.local"` | Couchbase URL. Used only when global.cnPersistenceType is hybrid or couchbase. This should be in FQDN format for either remote or local Couchbase clusters. The address can be an internal address inside the kubernetes cluster |
| config.configmap.cnCouchbaseUser | string | `"janssen"` | Couchbase restricted user. Used only when global.cnPersistenceType is hybrid or couchbase. |
| config.configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the Google project the secret manager belongs to. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSecretManagerServiceAccount | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Service account with roles roles/secretmanager.admin base64 encoded string. This is used often inside the services to reach the configuration layer. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSecretNamePrefix | string | `"janssen"` | Prefix for Janssen secret in Google Secret Manager. Defaults to janssen. If left janssen-secret secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSecretVersionId | string | `"latest"` | Secret version to be used for secret configuration. Defaults to latest and should normally always stay that way. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSpannerDatabaseId | string | `""` | Google Spanner Database ID. Used only when global.cnPersistenceType is spanner. |
| config.configmap.cnGoogleSpannerInstanceId | string | `""` | Google Spanner ID. Used only when global.cnPersistenceType is spanner. |
| config.configmap.cnJettyRequestHeaderSize | int | `8192` | Jetty header size in bytes in the auth server |
| config.configmap.cnLdapCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | OpenDJ certificate string. This must be encoded using base64. |
| config.configmap.cnLdapKey | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | OpenDJ key string. This must be encoded using base64. |
| config.configmap.cnLdapUrl | string | `"opendj:1636"` | OpenDJ internal address. Leave as default. Used when `global.cnPersistenceType` is set to `ldap`. |
| config.configmap.cnMaxRamPercent | string | `"75.0"` | Value passed to Java option -XX:MaxRAMPercentage |
| config.configmap.cnMessageType | string | `"DISABLED"` | Message type (one of POSTGRES, REDIS, or DISABLED) |
| config.configmap.cnOpaUrl | string | `"http://opa.opa.svc.cluster.cluster.local:8181/v1"` | URL of OPA API |
| config.configmap.cnPersistenceHybridMapping | string | `"{}"` | Specify data that should be saved in LDAP (one of default, user, cache, site, token, or session; default to default). Note this environment only takes effect when `global.cnPersistenceType`  is set to `hybrid`. {  "default": "<couchbase|ldap|spanner|sql>",  "user": "<couchbase|ldap|spanner|sql>",  "site": "<couchbase|ldap|spanner|sql>",  "cache": "<couchbase|ldap|spanner|sql>",  "token": "<couchbase|ldap|spanner|sql>",  "session": "<couchbase|ldap|spanner|sql>", } |
| config.configmap.cnRedisSentinelGroup | string | `""` | Redis Sentinel Group. Often set when `config.configmap.cnRedisType` is set to `SENTINEL`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisSslTruststore | string | `""` | Redis SSL truststore. Optional. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisType | string | `"STANDALONE"` | Redis service type. `STANDALONE` or `CLUSTER`. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisUrl | string | `"redis.redis.svc.cluster.local:6379"` | Redis URL and port number <url>:<port>. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnRedisUseSsl | bool | `false` | Boolean to use SSL in Redis. Can be used when  `config.configmap.cnCacheType` is set to `REDIS`. |
| config.configmap.cnScimProtectionMode | string | `"OAUTH"` | SCIM protection mode OAUTH|TEST|UMA |
| config.configmap.cnSecretKubernetesSecret | string | `"cn"` | Kubernetes secret name holding configuration keys. Used when global.configSecretAdapter is set to kubernetes which is the default. |
| config.configmap.cnSqlDbDialect | string | `"mysql"` | SQL database dialect. `mysql` or `pgsql` |
| config.configmap.cnSqlDbHost | string | `"my-release-mysql.default.svc.cluster.local"` | SQL database host uri. |
| config.configmap.cnSqlDbName | string | `"jans"` | SQL database name. |
| config.configmap.cnSqlDbPort | int | `3306` | SQL database port. |
| config.configmap.cnSqlDbSchema | string | `""` | Schema name used by SQL database (default to empty-string; if using MySQL, the schema name will be resolved as the database name, whereas in PostgreSQL the schema name will be resolved as `"public"`). |
| config.configmap.cnSqlDbTimezone | string | `"UTC"` | SQL database timezone. |
| config.configmap.cnSqlDbUser | string | `"jans"` | SQL database username. |
| config.configmap.cnSqldbUserPassword | string | `"Test1234#"` | SQL password  injected the secrets . |
| config.configmap.cnVaultAddr | string | `"http://localhost:8200"` | Base URL of Vault. |
| config.configmap.cnVaultAppRolePath | string | `"approle"` | Path to Vault AppRole. |
| config.configmap.cnVaultKvPath | string | `"secret"` | Path to Vault KV secrets engine. |
| config.configmap.cnVaultNamespace | string | `""` | Vault namespace used to access the secrets. |
| config.configmap.cnVaultPrefix | string | `"jans"` | Base prefix name used to access secrets. |
| config.configmap.cnVaultRoleId | string | `""` | Vault AppRole RoleID. |
| config.configmap.cnVaultRoleIdFile | string | `"/etc/certs/vault_role_id"` | Path to file contains Vault AppRole role ID. |
| config.configmap.cnVaultSecretId | string | `""` | Vault AppRole SecretID. |
| config.configmap.cnVaultSecretIdFile | string | `"/etc/certs/vault_secret_id"` | Path to file contains Vault AppRole secret ID. |
| config.configmap.cnVaultVerify | bool | `false` | Verify connection to Vault. |
| config.configmap.kcDbPassword | string | `"Test1234#"` | Password for Keycloak database access |
| config.configmap.kcDbSchema | string | `"keycloak"` | Keycloak database schema name (note that PostgreSQL may be using "public" schema). |
| config.configmap.kcDbUrlDatabase | string | `"keycloak"` | Keycloak database name. |
| config.configmap.kcDbUrlHost | string | `"mysql.kc.svc.cluster.local"` | Keycloak database host uri |
| config.configmap.kcDbUrlPort | int | `3306` | Keycloak database port (default to port 3306 for mysql). |
| config.configmap.kcDbUrlProperties | string | `"?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4"` | Keycloak database connection properties. If using postgresql, the value can be set to empty string. |
| config.configmap.kcDbUsername | string | `"keycloak"` | Keycloak database username |
| config.configmap.kcDbVendor | string | `"mysql"` | Keycloak database vendor name (default to MySQL server). To use PostgreSQL server, change the value to postgres. |
| config.configmap.kcLogLevel | string | `"INFO"` | Keycloak logging level |
| config.configmap.kcProxy | string | `"edge"` | Keycloak proxy mode (for most deployments, this doesn't need to be changed) |
| config.configmap.lbAddr | string | `""` | Load balancer address for AWS if the FQDN is not registered. |
| config.configmap.quarkusTransactionEnableRecovery | bool | `true` | Quarkus transaction recovery. When using MySQL, there could be issue regarding XA_RECOVER_ADMIN; refer to https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_xa-recover-admin for details. |
| config.countryCode | string | `"US"` | Country code. Used for certificate creation. |
| config.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| config.dnsConfig | object | `{}` | Add custom dns config |
| config.dnsPolicy | string | `""` | Add custom dns policy |
| config.email | string | `"support@jans.io"` | Email address of the administrator usually. Used for certificate creation. |
| config.image.pullSecrets | list | `[]` | Image Pull Secrets |
| config.image.repository | string | `"ghcr.io/janssenproject/jans/configurator"` | Image  to use for deploying. |
| config.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| config.ldapPassword | string | `"P@ssw0rds"` | LDAP admin password if OpenDJ is used for persistence. |
| config.ldapTruststorePassword | string | `"changeit"` | LDAP truststore password if OpenDJ is used for persistence |
| config.orgName | string | `"Janssen"` | Organization name. Used for certificate creation. |
| config.redisPassword | string | `"P@assw0rd"` | Redis admin password if `config.configmap.cnCacheType` is set to `REDIS`. |
| config.resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| config.resources.limits.cpu | string | `"300m"` | CPU limit. |
| config.resources.limits.memory | string | `"300Mi"` | Memory limit. |
| config.resources.requests.cpu | string | `"300m"` | CPU request. |
| config.resources.requests.memory | string | `"300Mi"` | Memory request. |
| config.salt | string | `""` | Salt. Used for encoding/decoding sensitive data. If omitted or set to empty string, the value will be self-generated. Otherwise, a 24 alphanumeric characters are allowed as its value. |
| config.state | string | `"TX"` | State code. Used for certificate creation. |
| config.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service. |
| config.usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| config.usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| config.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| config.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| fido2 | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/fido2","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}},"service":{"name":"http-fido2","port":8080},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | FIDO 2.0 (FIDO2) is an open authentication standard that enables leveraging common devices to authenticate to online services in both mobile and desktop environments. |
| fido2.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| fido2.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| fido2.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| fido2.dnsConfig | object | `{}` | Add custom dns config |
| fido2.dnsPolicy | string | `""` | Add custom dns policy |
| fido2.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| fido2.hpa.behavior | object | `{}` | Scaling Policies |
| fido2.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| fido2.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| fido2.image.pullSecrets | list | `[]` | Image Pull Secrets |
| fido2.image.repository | string | `"ghcr.io/janssenproject/jans/fido2"` | Image  to use for deploying. |
| fido2.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| fido2.livenessProbe | object | `{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the liveness healthcheck for the fido2 if needed. |
| fido2.livenessProbe.httpGet | object | `{"path":"/jans-fido2/sys/health-check","port":"http-fido2"}` | http liveness probe endpoint |
| fido2.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| fido2.readinessProbe | object | `{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the readiness healthcheck for the fido2 if needed. |
| fido2.replicas | int | `1` | Service replica number. |
| fido2.resources | object | `{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}}` | Resource specs. |
| fido2.resources.limits.cpu | string | `"500m"` | CPU limit. |
| fido2.resources.limits.memory | string | `"500Mi"` | Memory limit. |
| fido2.resources.requests.cpu | string | `"500m"` | CPU request. |
| fido2.resources.requests.memory | string | `"500Mi"` | Memory request. |
| fido2.service.name | string | `"http-fido2"` | The name of the fido2 port within the fido2 service. Please keep it as default. |
| fido2.service.port | int | `8080` | Port of the fido2 service. Please keep it as default. |
| fido2.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| fido2.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| fido2.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| fido2.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| fido2.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| fido2.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| global | object | `{"alb":{"ingress":false},"auth-server":{"appLoggers":{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","enableStdoutLogPrefix":"true","httpLogLevel":"INFO","httpLogTarget":"FILE","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"authEncKeys":"RSA1_5 RSA-OAEP","authServerServiceName":"auth-server","authSigKeys":"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512","enabled":true,"ingress":{"authServerEnabled":true,"deviceCodeEnabled":true,"firebaseMessagingEnabled":true,"openidConfigEnabled":true,"u2fConfigEnabled":true,"uma2ConfigEnabled":true,"webdiscoveryEnabled":true,"webfingerEnabled":true},"lockEnabled":false},"auth-server-key-rotation":{"enabled":true,"initKeysLife":48},"awsStorageType":"io1","azureStorageAccountType":"Standard_LRS","azureStorageKind":"Managed","casa":{"appLoggers":{"casaLogLevel":"INFO","casaLogTarget":"STDOUT","enableStdoutLogPrefix":"true","timerLogLevel":"INFO","timerLogTarget":"FILE"},"casaServiceName":"casa","enabled":true,"ingress":{"casaEnabled":false}},"cloud":{"testEnviroment":false},"cnAwsConfigFile":"/etc/jans/conf/aws_config_file","cnAwsSecretsReplicaRegionsFile":"/etc/jans/conf/aws_secrets_replica_regions","cnAwsSharedCredentialsFile":"/etc/jans/conf/aws_shared_credential_file","cnCouchbasePasswordFile":"/etc/jans/conf/couchbase_password","cnCouchbaseSuperuserPasswordFile":"/etc/jans/conf/couchbase_superuser_password","cnDocumentStoreType":"DB","cnGoogleApplicationCredentials":"/etc/jans/conf/google-credentials.json","cnLdapCacertFile":"/etc/certs/opendj.pem","cnLdapCertFile":"/etc/certs/opendj.crt","cnLdapKeyFile":"/etc/certs/opendj.key","cnLdapPasswordFile":"/etc/jans/conf/ldap_password","cnLdapTruststoreFile":"/etc/certs/opendj.pkcs12","cnLdapTruststorePasswordFile":"/etc/jans/conf/ldap_truststore_password","cnPersistenceType":"sql","cnPrometheusPort":"","cnSqlPasswordFile":"/etc/jans/conf/sql_password","config":{"enabled":true},"config-api":{"appLoggers":{"configApiLogLevel":"INFO","configApiLogTarget":"STDOUT","enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"configApiServerServiceName":"config-api","enabled":true,"ingress":{"configApiEnabled":true},"plugins":"fido2,scim,user-mgt"},"configAdapterName":"kubernetes","configSecretAdapter":"kubernetes","fido2":{"appLoggers":{"enableStdoutLogPrefix":"true","fido2LogLevel":"INFO","fido2LogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"enabled":true,"fido2ServiceName":"fido2","ingress":{"fido2ConfigEnabled":false}},"fqdn":"demoexample.jans.io","gcePdStorageType":"pd-standard","isFqdnRegistered":false,"istio":{"additionalAnnotations":{},"additionalLabels":{},"enabled":false,"gateways":[],"ingress":false,"namespace":"istio-system"},"jobTtlSecondsAfterFinished":300,"kcAdminCredentialsFile":"/etc/jans/conf/kc_admin_creds","kcDbPasswordFile":"/etc/jans/conf/kc_db_password","lbIp":"22.22.22.22","link":{"appLoggers":{"enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","linkLogLevel":"INFO","linkLogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"enabled":false,"ingress":{"linkEnabled":true},"linkServiceName":"link"},"nginx-ingress":{"enabled":true},"opendj":{"enabled":false,"ldapServiceName":"opendj"},"persistence":{"enabled":true},"saml":{"enabled":false,"ingress":{"samlEnabled":false},"samlServiceName":"saml"},"scim":{"appLoggers":{"enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scimLogLevel":"INFO","scimLogTarget":"STDOUT","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"enabled":true,"ingress":{"scimConfigEnabled":false,"scimEnabled":false},"scimServiceName":"scim"},"storageClass":{"allowVolumeExpansion":true,"allowedTopologies":[],"mountOptions":["debug"],"parameters":{},"provisioner":"microk8s.io/hostpath","reclaimPolicy":"Retain","volumeBindingMode":"WaitForFirstConsumer"},"usrEnvs":{"normal":{},"secret":{}}}` | Parameters used globally across all services helm charts. |
| global.alb.ingress | bool | `false` | Activates ALB ingress |
| global.auth-server-key-rotation.enabled | bool | `true` | Boolean flag to enable/disable the auth-server-key rotation cronjob chart. |
| global.auth-server-key-rotation.initKeysLife | int | `48` | The initial auth server key rotation keys life in hours |
| global.auth-server.appLoggers | object | `{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","enableStdoutLogPrefix":"true","httpLogLevel":"INFO","httpLogTarget":"FILE","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.auth-server.appLoggers.auditStatsLogLevel | string | `"INFO"` | jans-auth_audit.log level |
| global.auth-server.appLoggers.auditStatsLogTarget | string | `"FILE"` | jans-auth_script.log target |
| global.auth-server.appLoggers.authLogLevel | string | `"INFO"` | jans-auth.log level |
| global.auth-server.appLoggers.authLogTarget | string | `"STDOUT"` | jans-auth.log target |
| global.auth-server.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e auth-server-script ===> 2022-12-20 17:49:55,744 INFO |
| global.auth-server.appLoggers.httpLogLevel | string | `"INFO"` | http_request_response.log level |
| global.auth-server.appLoggers.httpLogTarget | string | `"FILE"` | http_request_response.log target |
| global.auth-server.appLoggers.ldapStatsLogLevel | string | `"INFO"` | jans-auth_persistence_ldap_statistics.log level |
| global.auth-server.appLoggers.ldapStatsLogTarget | string | `"FILE"` | jans-auth_persistence_ldap_statistics.log target |
| global.auth-server.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | jans-auth_persistence_duration.log level |
| global.auth-server.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | jans-auth_persistence_duration.log target |
| global.auth-server.appLoggers.persistenceLogLevel | string | `"INFO"` | jans-auth_persistence.log level |
| global.auth-server.appLoggers.persistenceLogTarget | string | `"FILE"` | jans-auth_persistence.log target |
| global.auth-server.appLoggers.scriptLogLevel | string | `"INFO"` | jans-auth_script.log level |
| global.auth-server.appLoggers.scriptLogTarget | string | `"FILE"` | jans-auth_script.log target |
| global.auth-server.authEncKeys | string | `"RSA1_5 RSA-OAEP"` | space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`) |
| global.auth-server.authServerServiceName | string | `"auth-server"` | Name of the auth-server service. Please keep it as default. |
| global.auth-server.authSigKeys | string | `"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"` | space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`) |
| global.auth-server.enabled | bool | `true` | Boolean flag to enable/disable auth-server chart. You should never set this to false. |
| global.auth-server.ingress | object | `{"authServerEnabled":true,"deviceCodeEnabled":true,"firebaseMessagingEnabled":true,"openidConfigEnabled":true,"u2fConfigEnabled":true,"uma2ConfigEnabled":true,"webdiscoveryEnabled":true,"webfingerEnabled":true}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.auth-server.ingress.authServerEnabled | bool | `true` | Enable Auth server endpoints /jans-auth |
| global.auth-server.ingress.deviceCodeEnabled | bool | `true` | Enable endpoint /device-code |
| global.auth-server.ingress.firebaseMessagingEnabled | bool | `true` | Enable endpoint /firebase-messaging-sw.js |
| global.auth-server.ingress.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| global.auth-server.ingress.u2fConfigEnabled | bool | `true` | Enable endpoint /.well-known/fido-configuration |
| global.auth-server.ingress.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| global.auth-server.ingress.webdiscoveryEnabled | bool | `true` | Enable endpoint /.well-known/simple-web-discovery |
| global.auth-server.ingress.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
| global.auth-server.lockEnabled | bool | `false` | Enable jans-lock as service running inside auth-server |
| global.awsStorageType | string | `"io1"` | Volume storage type if using AWS volumes. |
| global.azureStorageAccountType | string | `"Standard_LRS"` | Volume storage type if using Azure disks. |
| global.azureStorageKind | string | `"Managed"` | Azure storage kind if using Azure disks |
| global.casa.appLoggers | object | `{"casaLogLevel":"INFO","casaLogTarget":"STDOUT","enableStdoutLogPrefix":"true","timerLogLevel":"INFO","timerLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.casa.appLoggers.casaLogLevel | string | `"INFO"` | casa.log level |
| global.casa.appLoggers.casaLogTarget | string | `"STDOUT"` | casa.log target |
| global.casa.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e casa ===> 2022-12-20 17:49:55,744 INFO |
| global.casa.appLoggers.timerLogLevel | string | `"INFO"` | casa timer log level |
| global.casa.appLoggers.timerLogTarget | string | `"FILE"` | casa timer log target |
| global.casa.casaServiceName | string | `"casa"` | Name of the casa service. Please keep it as default. |
| global.casa.enabled | bool | `true` | Boolean flag to enable/disable the casa chart. |
| global.casa.ingress | object | `{"casaEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.casa.ingress.casaEnabled | bool | `false` | Enable casa endpoints /casa |
| global.cloud.testEnviroment | bool | `false` | Boolean flag if enabled will strip resources requests and limits from all services. |
| global.cnCouchbasePasswordFile | string | `"/etc/jans/conf/couchbase_password"` | Path to Couchbase password file |
| global.cnCouchbaseSuperuserPasswordFile | string | `"/etc/jans/conf/couchbase_superuser_password"` | Path to Couchbase superuser password file |
| global.cnDocumentStoreType | string | `"DB"` | Document store type to use for shibboleth files DB. |
| global.cnGoogleApplicationCredentials | string | `"/etc/jans/conf/google-credentials.json"` | Base64 encoded service account. The sa must have roles/secretmanager.admin to use Google secrets and roles/spanner.databaseUser to use Spanner. Leave as this is a sensible default. |
| global.cnLdapCacertFile | string | `"/etc/certs/opendj.pem"` | Path to OpenDJ CA cert file |
| global.cnLdapCertFile | string | `"/etc/certs/opendj.crt"` | Path to OpenDJ cert file |
| global.cnLdapKeyFile | string | `"/etc/certs/opendj.key"` | Path to OpenDJ key file |
| global.cnLdapPasswordFile | string | `"/etc/jans/conf/ldap_password"` | Path to LDAP password file |
| global.cnLdapTruststoreFile | string | `"/etc/certs/opendj.pkcs12"` | Path to OpenDJ truststore file |
| global.cnLdapTruststorePasswordFile | string | `"/etc/jans/conf/ldap_truststore_password"` | Path to LDAP truststore password file |
| global.cnPersistenceType | string | `"sql"` | Persistence backend to run Janssen with ldap|couchbase|hybrid|sql|spanner. |
| global.cnPrometheusPort | string | `""` | Port used by Prometheus JMX agent (default to empty string). To enable Prometheus JMX agent, set the value to a number. |
| global.cnSqlPasswordFile | string | `"/etc/jans/conf/sql_password"` | Path to SQL password file |
| global.config-api.appLoggers | object | `{"configApiLogLevel":"INFO","configApiLogTarget":"STDOUT","enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.config-api.appLoggers.configApiLogLevel | string | `"INFO"` | configapi.log level |
| global.config-api.appLoggers.configApiLogTarget | string | `"STDOUT"` | configapi.log target |
| global.config-api.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e config-api_persistence ===> 2022-12-20 17:49:55,744 INFO |
| global.config-api.appLoggers.ldapStatsLogLevel | string | `"INFO"` | config-api_persistence_ldap_statistics.log level |
| global.config-api.appLoggers.ldapStatsLogTarget | string | `"FILE"` | config-api_persistence_ldap_statistics.log target |
| global.config-api.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | config-api_persistence_duration.log level |
| global.config-api.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | config-api_persistence_duration.log target |
| global.config-api.appLoggers.persistenceLogLevel | string | `"INFO"` | config-api_persistence.log level |
| global.config-api.appLoggers.persistenceLogTarget | string | `"FILE"` | config-api_persistence.log target |
| global.config-api.appLoggers.scriptLogLevel | string | `"INFO"` | config-api_script.log level |
| global.config-api.appLoggers.scriptLogTarget | string | `"FILE"` | config-api_script.log target |
| global.config-api.configApiServerServiceName | string | `"config-api"` | Name of the config-api service. Please keep it as default. |
| global.config-api.enabled | bool | `true` | Boolean flag to enable/disable the config-api chart. |
| global.config-api.ingress | object | `{"configApiEnabled":true}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.config-api.plugins | string | `"fido2,scim,user-mgt"` | Comma-separated values of enabled plugins (supported plugins are "admin-ui","fido2","scim","user-mgt","jans-link","kc-saml") |
| global.config.enabled | bool | `true` | Boolean flag to enable/disable the configuration chart. This normally should never be false |
| global.configAdapterName | string | `"kubernetes"` | The config backend adapter that will hold Janssen configuration layer. aws|google|kubernetes |
| global.configSecretAdapter | string | `"kubernetes"` | The config backend adapter that will hold Janssen secret layer. vault|aws|google|kubernetes |
| global.fido2.appLoggers | object | `{"enableStdoutLogPrefix":"true","fido2LogLevel":"INFO","fido2LogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.fido2.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e fido2 ===> 2022-12-20 17:49:55,744 INFO |
| global.fido2.appLoggers.fido2LogLevel | string | `"INFO"` | fido2.log level |
| global.fido2.appLoggers.fido2LogTarget | string | `"STDOUT"` | fido2.log target |
| global.fido2.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | fido2_persistence_duration.log level |
| global.fido2.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | fido2_persistence_duration.log target |
| global.fido2.appLoggers.persistenceLogLevel | string | `"INFO"` | fido2_persistence.log level |
| global.fido2.appLoggers.persistenceLogTarget | string | `"FILE"` | fido2_persistence.log target |
| global.fido2.appLoggers.scriptLogLevel | string | `"INFO"` | fido2_script.log level |
| global.fido2.appLoggers.scriptLogTarget | string | `"FILE"` | fido2_script.log target |
| global.fido2.enabled | bool | `true` | Boolean flag to enable/disable the fido2 chart. |
| global.fido2.fido2ServiceName | string | `"fido2"` | Name of the fido2 service. Please keep it as default. |
| global.fido2.ingress | object | `{"fido2ConfigEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.fido2.ingress.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| global.fqdn | string | `"demoexample.jans.io"` | Fully qualified domain name to be used for Janssen installation. This address will be used to reach Janssen services. |
| global.gcePdStorageType | string | `"pd-standard"` | GCE storage kind if using Google disks |
| global.isFqdnRegistered | bool | `false` | Boolean flag to enable mapping global.lbIp  to global.fqdn inside pods on clouds that provide static ip for load balancers. On cloud that provide only addresses to the LB this flag will enable a script to actively scan config.configmap.lbAddr and update the hosts file inside the pods automatically. |
| global.istio.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| global.istio.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| global.istio.enabled | bool | `false` | Boolean flag that enables using istio side-cars with Janssen services. |
| global.istio.gateways | list | `[]` | Override the gateway that can be created by default. This is used when istio ingress has already been setup and the gateway exists. |
| global.istio.ingress | bool | `false` | Boolean flag that enables using istio gateway for Janssen. This assumes istio ingress is installed and hence the LB is available. |
| global.istio.namespace | string | `"istio-system"` | The namespace istio is deployed in. The is normally istio-system. |
| global.jobTtlSecondsAfterFinished | int | `300` | https://kubernetes.io/docs/concepts/workloads/controllers/ttlafterfinished/ |
| global.kcAdminCredentialsFile | string | `"/etc/jans/conf/kc_admin_creds"` | Path to file contains Keycloak admin credentials (username and password) |
| global.kcDbPasswordFile | string | `"/etc/jans/conf/kc_db_password"` | Path to file contains password for database access |
| global.lbIp | string | `"22.22.22.22"` | The Loadbalancer IP created by nginx or istio on clouds that provide static IPs. This is not needed if `global.fqdn` is globally resolvable. |
| global.link.appLoggers | object | `{"enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","linkLogLevel":"INFO","linkLogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.link.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e link-persistence ===> 2022-12-20 17:49:55,744 INFO |
| global.link.appLoggers.ldapStatsLogLevel | string | `"INFO"` | cacherefresh_persistence_ldap_statistics.log level |
| global.link.appLoggers.ldapStatsLogTarget | string | `"FILE"` | cacherefresh_persistence_ldap_statistics.log target |
| global.link.appLoggers.linkLogLevel | string | `"INFO"` | cacherefresh.log level |
| global.link.appLoggers.linkLogTarget | string | `"STDOUT"` | cacherefresh.log target |
| global.link.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | cacherefresh_persistence_duration.log level |
| global.link.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | cacherefresh_persistence_duration.log target |
| global.link.appLoggers.persistenceLogLevel | string | `"INFO"` | cacherefresh_persistence.log level |
| global.link.appLoggers.persistenceLogTarget | string | `"FILE"` | cacherefresh_persistence.log target |
| global.link.appLoggers.scriptLogLevel | string | `"INFO"` | cacherefresh_script.log level |
| global.link.appLoggers.scriptLogTarget | string | `"FILE"` | cacherefresh_script.log target |
| global.link.enabled | bool | `false` | Boolean flag to enable/disable the link chart. |
| global.link.ingress | object | `{"linkEnabled":true}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.link.linkServiceName | string | `"link"` | Name of the link service. Please keep it as default. |
| global.nginx-ingress.enabled | bool | `true` | Boolean flag to enable/disable the nginx-ingress definitions chart. |
| global.opendj.enabled | bool | `false` | Boolean flag to enable/disable the OpenDJ  chart. |
| global.opendj.ldapServiceName | string | `"opendj"` | Name of the OpenDJ service. Please keep it as default. |
| global.persistence.enabled | bool | `true` | Boolean flag to enable/disable the persistence chart. |
| global.saml.enabled | bool | `false` | Boolean flag to enable/disable the saml chart. |
| global.saml.ingress | object | `{"samlEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.saml.samlServiceName | string | `"saml"` | Name of the saml service. Please keep it as default. |
| global.scim.appLoggers | object | `{"enableStdoutLogPrefix":"true","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scimLogLevel":"INFO","scimLogTarget":"STDOUT","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.scim.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e jans-scim ===> 2022-12-20 17:49:55,744 INFO |
| global.scim.appLoggers.ldapStatsLogLevel | string | `"INFO"` | jans-scim_persistence_ldap_statistics.log level |
| global.scim.appLoggers.ldapStatsLogTarget | string | `"FILE"` | jans-scim_persistence_ldap_statistics.log target |
| global.scim.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | jans-scim_persistence_duration.log level |
| global.scim.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | jans-scim_persistence_duration.log target |
| global.scim.appLoggers.persistenceLogLevel | string | `"INFO"` | jans-scim_persistence.log level |
| global.scim.appLoggers.persistenceLogTarget | string | `"FILE"` | jans-scim_persistence.log target |
| global.scim.appLoggers.scimLogLevel | string | `"INFO"` | jans-scim.log level |
| global.scim.appLoggers.scimLogTarget | string | `"STDOUT"` | jans-scim.log target |
| global.scim.appLoggers.scriptLogLevel | string | `"INFO"` | jans-scim_script.log level |
| global.scim.appLoggers.scriptLogTarget | string | `"FILE"` | jans-scim_script.log target |
| global.scim.enabled | bool | `true` | Boolean flag to enable/disable the SCIM chart. |
| global.scim.ingress | object | `{"scimConfigEnabled":false,"scimEnabled":false}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| global.scim.ingress.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| global.scim.ingress.scimEnabled | bool | `false` | Enable SCIM endpoints /jans-scim |
| global.scim.scimServiceName | string | `"scim"` | Name of the scim service. Please keep it as default. |
| global.storageClass | object | `{"allowVolumeExpansion":true,"allowedTopologies":[],"mountOptions":["debug"],"parameters":{},"provisioner":"microk8s.io/hostpath","reclaimPolicy":"Retain","volumeBindingMode":"WaitForFirstConsumer"}` | StorageClass section for OpenDJ charts. This is not currently used by the openbanking distribution. You may specify custom parameters as needed. |
| global.storageClass.parameters | object | `{}` | parameters: fsType: "" kind: "" pool: "" storageAccountType: "" type: "" |
| global.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service. Envs defined in global.userEnvs will be globally available to all services |
| global.usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| global.usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| link | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/link","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"500m","memory":"1000Mi"},"requests":{"cpu":"500m","memory":"1000Mi"}},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Link. |
| link.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| link.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| link.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| link.dnsConfig | object | `{}` | Add custom dns config |
| link.dnsPolicy | string | `""` | Add custom dns policy |
| link.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| link.hpa.behavior | object | `{}` | Scaling Policies |
| link.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| link.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| link.image.pullSecrets | list | `[]` | Image Pull Secrets |
| link.image.repository | string | `"ghcr.io/janssenproject/jans/link"` | Image  to use for deploying. |
| link.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| link.livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| link.livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | http liveness probe endpoint |
| link.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| link.readinessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | http readiness probe endpoint |
| link.replicas | int | `1` | Service replica number. |
| link.resources | object | `{"limits":{"cpu":"500m","memory":"1000Mi"},"requests":{"cpu":"500m","memory":"1000Mi"}}` | Resource specs. |
| link.resources.limits.cpu | string | `"500m"` | CPU limit. |
| link.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| link.resources.requests.cpu | string | `"500m"` | CPU request. |
| link.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| link.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| link.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| link.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| link.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| link.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| link.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| nginx-ingress | object | `{"ingress":{"additionalAnnotations":{},"additionalLabels":{},"authServerAdditionalAnnotations":{},"authServerLabels":{},"casaAdditionalAnnotations":{},"casaLabels":{},"configApiAdditionalAnnotations":{},"configApiLabels":{},"deviceCodeAdditionalAnnotations":{},"deviceCodeLabels":{},"fido2ConfigAdditionalAnnotations":{},"fido2ConfigLabels":{},"firebaseMessagingAdditionalAnnotations":{},"firebaseMessagingLabels":{},"hosts":["demoexample.jans.io"],"ingressClassName":"nginx","openidAdditionalAnnotations":{},"openidConfigLabels":{},"path":"/","samlAdditionalAnnotations":{},"samlLabels":{},"scimAdditionalAnnotations":{},"scimConfigAdditionalAnnotations":{},"scimConfigLabels":{},"scimLabels":{},"tls":[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}],"u2fAdditionalAnnotations":{},"u2fConfigLabels":{},"uma2AdditionalAnnotations":{},"uma2ConfigLabels":{},"webdiscoveryAdditionalAnnotations":{},"webdiscoveryLabels":{},"webfingerAdditionalAnnotations":{},"webfingerLabels":{}}}` | Nginx ingress definitions chart |
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
| nginx-ingress.ingress.openidAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| nginx-ingress.ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| nginx-ingress.ingress.samlAdditionalAnnotations | object | `{}` | SAML ingress resource additional annotations. |
| nginx-ingress.ingress.samlLabels | object | `{}` | SAML config ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimAdditionalAnnotations | object | `{}` | SCIM ingress resource additional annotations. |
| nginx-ingress.ingress.scimConfigAdditionalAnnotations | object | `{}` | SCIM config ingress resource additional annotations. |
| nginx-ingress.ingress.scimConfigLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.tls | list | `[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}]` | Secrets holding HTTPS CA cert and key. |
| nginx-ingress.ingress.u2fAdditionalAnnotations | object | `{}` | u2f config ingress resource additional annotations. |
| nginx-ingress.ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| nginx-ingress.ingress.uma2AdditionalAnnotations | object | `{}` | uma2 config ingress resource additional annotations. |
| nginx-ingress.ingress.uma2ConfigLabels | object | `{}` | uma2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.webdiscoveryAdditionalAnnotations | object | `{}` | webdiscovery ingress resource additional annotations. |
| nginx-ingress.ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| nginx-ingress.ingress.webfingerAdditionalAnnotations | object | `{}` | webfinger ingress resource additional annotations. |
| nginx-ingress.ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| opendj | object | `{"additionalAnnotations":{},"additionalLabels":{},"backup":{"cronJobSchedule":"*/59 * * * *","enabled":true},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/opendj","tag":"5.0.0_dev"},"lifecycle":{"preStop":{"exec":{"command":["/bin/sh","-c","python3 /app/scripts/deregister_peer.py 1>&/proc/1/fd/1"]}}},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":20,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":1},"persistence":{"size":"5Gi"},"ports":{"tcp-admin":{"nodePort":"","port":4444,"protocol":"TCP","targetPort":4444},"tcp-ldap":{"nodePort":"","port":1389,"protocol":"TCP","targetPort":1389},"tcp-ldaps":{"nodePort":"","port":1636,"protocol":"TCP","targetPort":1636},"tcp-repl":{"nodePort":"","port":8989,"protocol":"TCP","targetPort":8989},"tcp-serf":{"nodePort":"","port":7946,"protocol":"TCP","targetPort":7946},"udp-serf":{"nodePort":"","port":7946,"protocol":"UDP","targetPort":7946}},"readinessProbe":{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1500m","memory":"2000Mi"},"requests":{"cpu":"1500m","memory":"2000Mi"}},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | OpenDJ is a directory server which implements a wide range of Lightweight Directory Access Protocol and related standards, including full compliance with LDAPv3 but also support for Directory Service Markup Language (DSMLv2).Written in Java, OpenDJ offers multi-master replication, access control, and many extensions. |
| opendj.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| opendj.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| opendj.backup | object | `{"cronJobSchedule":"*/59 * * * *","enabled":true}` | Configure ldap backup cronjob |
| opendj.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| opendj.dnsConfig | object | `{}` | Add custom dns config |
| opendj.dnsPolicy | string | `""` | Add custom dns policy |
| opendj.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| opendj.hpa.behavior | object | `{}` | Scaling Policies |
| opendj.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| opendj.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| opendj.image.pullSecrets | list | `[]` | Image Pull Secrets |
| opendj.image.repository | string | `"gluufederation/opendj"` | Image  to use for deploying. |
| opendj.image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| opendj.livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":20,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for OpenDJ if needed. https://github.com/GluuFederation/docker-opendj/blob/master/scripts/healthcheck.py |
| opendj.livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | Executes the python3 healthcheck. |
| opendj.pdb | object | `{"enabled":true,"maxUnavailable":1}` | Configure the PodDisruptionBudget |
| opendj.persistence.size | string | `"5Gi"` | OpenDJ volume size |
| opendj.readinessProbe | object | `{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5}` | Configure the readiness healthcheck for OpenDJ if needed. https://github.com/GluuFederation/docker-opendj/blob/master/scripts/healthcheck.py |
| opendj.replicas | int | `1` | Service replica number. |
| opendj.resources | object | `{"limits":{"cpu":"1500m","memory":"2000Mi"},"requests":{"cpu":"1500m","memory":"2000Mi"}}` | Resource specs. |
| opendj.resources.limits.cpu | string | `"1500m"` | CPU limit. |
| opendj.resources.limits.memory | string | `"2000Mi"` | Memory limit. |
| opendj.resources.requests.cpu | string | `"1500m"` | CPU request. |
| opendj.resources.requests.memory | string | `"2000Mi"` | Memory request. |
| opendj.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| opendj.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| opendj.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| opendj.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| opendj.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| opendj.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| persistence | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/persistence-loader","tag":"1.1.0_dev"},"lifecycle":{},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Job to generate data and initial config for Janssen Server persistence layer. |
| persistence.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| persistence.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| persistence.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| persistence.dnsConfig | object | `{}` | Add custom dns config |
| persistence.dnsPolicy | string | `""` | Add custom dns policy |
| persistence.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| persistence.image.pullSecrets | list | `[]` | Image Pull Secrets |
| persistence.image.repository | string | `"ghcr.io/janssenproject/jans/persistence-loader"` | Image  to use for deploying. |
| persistence.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
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
| saml | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/saml","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":10,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":10,"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"500m","memory":"1000Mi"},"requests":{"cpu":"500m","memory":"1000Mi"}},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | SAML. |
| saml.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| saml.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| saml.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| saml.dnsConfig | object | `{}` | Add custom dns config |
| saml.dnsPolicy | string | `""` | Add custom dns policy |
| saml.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| saml.hpa.behavior | object | `{}` | Scaling Policies |
| saml.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| saml.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| saml.image.pullSecrets | list | `[]` | Image Pull Secrets |
| saml.image.repository | string | `"ghcr.io/janssenproject/jans/saml"` | Image  to use for deploying. |
| saml.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| saml.livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":10,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| saml.livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | http liveness probe endpoint |
| saml.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| saml.readinessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | http readiness probe endpoint |
| saml.replicas | int | `1` | Service replica number. |
| saml.resources | object | `{"limits":{"cpu":"500m","memory":"1000Mi"},"requests":{"cpu":"500m","memory":"1000Mi"}}` | Resource specs. |
| saml.resources.limits.cpu | string | `"500m"` | CPU limit. |
| saml.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| saml.resources.requests.cpu | string | `"500m"` | CPU request. |
| saml.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| saml.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| saml.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| saml.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| saml.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| saml.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| saml.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| scim | object | `{"additionalAnnotations":{},"additionalLabels":{},"customScripts":[],"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/scim","tag":"1.1.0_dev"},"lifecycle":{},"livenessProbe":{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"pdb":{"enabled":true,"maxUnavailable":"90%"},"readinessProbe":{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"1000Mi"},"requests":{"cpu":"1000m","memory":"1000Mi"}},"service":{"name":"http-scim","port":8080},"topologySpreadConstraints":{},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | System for Cross-domain Identity Management (SCIM) version 2.0 |
| scim.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| scim.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| scim.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| scim.dnsConfig | object | `{}` | Add custom dns config |
| scim.dnsPolicy | string | `""` | Add custom dns policy |
| scim.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| scim.hpa.behavior | object | `{}` | Scaling Policies |
| scim.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| scim.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| scim.image.pullSecrets | list | `[]` | Image Pull Secrets |
| scim.image.repository | string | `"ghcr.io/janssenproject/jans/scim"` | Image  to use for deploying. |
| scim.image.tag | string | `"1.1.0_dev"` | Image  tag to use for deploying. |
| scim.livenessProbe | object | `{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for SCIM if needed. |
| scim.livenessProbe.httpGet.path | string | `"/jans-scim/sys/health-check"` | http liveness probe endpoint |
| scim.pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| scim.readinessProbe | object | `{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the SCIM if needed. |
| scim.readinessProbe.httpGet.path | string | `"/jans-scim/sys/health-check"` | http readiness probe endpoint |
| scim.replicas | int | `1` | Service replica number. |
| scim.resources.limits.cpu | string | `"1000m"` | CPU limit. |
| scim.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| scim.resources.requests.cpu | string | `"1000m"` | CPU request. |
| scim.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| scim.service.name | string | `"http-scim"` | The name of the scim port within the scim service. Please keep it as default. |
| scim.service.port | int | `8080` | Port of the scim service. Please keep it as default. |
| scim.topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| scim.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| scim.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| scim.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| scim.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| scim.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
