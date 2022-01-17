# gluu

![Version: 5.0.2](https://img.shields.io/badge/Version-5.0.2-informational?style=flat-square) ![AppVersion: 5.0.0](https://img.shields.io/badge/AppVersion-5.0.0-informational?style=flat-square)

Gluu Access and Identity Management OpenBanking distribution

**Homepage:** <https://www.gluu.org>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| moabu | support@gluu.org |  |

## Source Code

* <https://gluu.org/docs/gluu-server>
* <https://github.com/GluuFederation/cloud-native-edition>

## Requirements

Kubernetes: `>=v1.21.0-0`

| Repository | Name | Version |
|------------|------|---------|
|  | admin-ui | 5.0.2 |
|  | auth-server | 5.0.2 |
|  | auth-server-key-rotation | 5.0.2 |
|  | casa | 5.0.2 |
|  | client-api | 5.0.2 |
|  | cn-istio-ingress | 5.0.2 |
|  | config | 5.0.2 |
|  | config-api | 5.0.2 |
|  | cr-rotate | 5.0.2 |
|  | fido2 | 5.0.2 |
|  | jackrabbit | 5.0.2 |
|  | nginx-ingress | 5.0.2 |
|  | opendj | 5.0.2 |
|  | oxpassport | 5.0.2 |
|  | oxshibboleth | 5.0.2 |
|  | persistence | 5.0.2 |
|  | scim | 5.0.2 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| admin-ui | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/admin-ui","tag":"1.0.0-beta.14"},"livenessProbe":{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5},"readinessProbe":{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Admin GUI for configuration of the auth-server |
| admin-ui.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| admin-ui.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| admin-ui.dnsConfig | object | `{}` | Add custom dns config |
| admin-ui.dnsPolicy | string | `""` | Add custom dns policy |
| admin-ui.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| admin-ui.hpa.behavior | object | `{}` | Scaling Policies |
| admin-ui.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| admin-ui.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| admin-ui.image.pullSecrets | list | `[]` | Image Pull Secrets |
| admin-ui.image.repository | string | `"gluufederation/admin-ui"` | Image  to use for deploying. |
| admin-ui.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
| admin-ui.livenessProbe | object | `{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5}` | Configure the liveness healthcheck for the admin ui if needed. |
| admin-ui.readinessProbe | object | `{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5}` | Configure the readiness healthcheck for the admin ui if needed. |
| admin-ui.replicas | int | `1` | Service replica number. |
| admin-ui.resources | object | `{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}}` | Resource specs. |
| admin-ui.resources.limits.cpu | string | `"2500m"` | CPU limit. |
| admin-ui.resources.limits.memory | string | `"2500Mi"` | Memory limit. |
| admin-ui.resources.requests.cpu | string | `"2500m"` | CPU request. |
| admin-ui.resources.requests.memory | string | `"2500Mi"` | Memory request. |
| admin-ui.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| admin-ui.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| admin-ui.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| admin-ui.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| admin-ui.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| auth-server | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/auth-server","tag":"1.0.0-beta.14"},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"2500m","memory":"2500Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | OAuth Authorization Server, the OpenID Connect Provider, the UMA Authorization Server--this is the main Internet facing component of Gluu. It's the service that returns tokens, JWT's and identity assertions. This service must be Internet facing. |
| auth-server-key-rotation | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/certmanager","tag":"1.0.0-beta.14"},"keysLife":48,"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Responsible for regenerating auth-keys per x hours |
| auth-server-key-rotation.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| auth-server-key-rotation.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| auth-server-key-rotation.dnsConfig | object | `{}` | Add custom dns config |
| auth-server-key-rotation.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server-key-rotation.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server-key-rotation.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server-key-rotation.image.repository | string | `"janssenproject/certmanager"` | Image  to use for deploying. |
| auth-server-key-rotation.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
| auth-server-key-rotation.keysLife | int | `48` | Auth server key rotation keys life in hours |
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
| auth-server.dnsConfig | object | `{}` | Add custom dns config |
| auth-server.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| auth-server.hpa.behavior | object | `{}` | Scaling Policies |
| auth-server.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| auth-server.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server.image.repository | string | `"janssenproject/auth-server"` | Image  to use for deploying. |
| auth-server.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
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
| casa | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/casa","tag":"5.0.0_dev"},"livenessProbe":{"httpGet":{"path":"/casa/health-check","port":"http-casa"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"readinessProbe":{"httpGet":{"path":"/casa/health-check","port":"http-casa"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Gluu Casa ("Casa") is a self-service web portal for end-users to manage authentication and authorization preferences for their account in a Gluu Server. |
| casa.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| casa.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| casa.dnsConfig | object | `{}` | Add custom dns config |
| casa.dnsPolicy | string | `""` | Add custom dns policy |
| casa.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| casa.hpa.behavior | object | `{}` | Scaling Policies |
| casa.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| casa.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| casa.image.pullSecrets | list | `[]` | Image Pull Secrets |
| casa.image.repository | string | `"gluufederation/casa"` | Image  to use for deploying. |
| casa.image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| casa.livenessProbe | object | `{"httpGet":{"path":"/casa/health-check","port":"http-casa"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the liveness healthcheck for casa if needed. |
| casa.livenessProbe.httpGet.path | string | `"/casa/health-check"` | http liveness probe endpoint |
| casa.readinessProbe | object | `{"httpGet":{"path":"/casa/health-check","port":"http-casa"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the readiness healthcheck for the casa if needed. |
| casa.readinessProbe.httpGet.path | string | `"/casa/health-check"` | http readiness probe endpoint |
| casa.replicas | int | `1` | Service replica number. |
| casa.resources | object | `{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}}` | Resource specs. |
| casa.resources.limits.cpu | string | `"500m"` | CPU limit. |
| casa.resources.limits.memory | string | `"500Mi"` | Memory limit. |
| casa.resources.requests.cpu | string | `"500m"` | CPU request. |
| casa.resources.requests.memory | string | `"500Mi"` | Memory request. |
| casa.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| casa.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| casa.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| casa.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| casa.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| client-api | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/client-api","tag":"1.0.0-beta.14"},"livenessProbe":{"exec":{"command":["curl","-k","https://localhost:8443/health-check"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":8443},"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"400Mi"},"requests":{"cpu":"1000m","memory":"400Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Middleware API to help application developers call an OAuth, OpenID or UMA server. You may wonder why this is necessary. It makes it easier for client developers to use OpenID signing and encryption features, without becoming crypto experts. This API provides some high level endpoints to do some of the heavy lifting. |
| client-api.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| client-api.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| client-api.dnsConfig | object | `{}` | Add custom dns config |
| client-api.dnsPolicy | string | `""` | Add custom dns policy |
| client-api.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| client-api.hpa.behavior | object | `{}` | Scaling Policies |
| client-api.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| client-api.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| client-api.image.pullSecrets | list | `[]` | Image Pull Secrets |
| client-api.image.repository | string | `"janssenproject/client-api"` | Image  to use for deploying. |
| client-api.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
| client-api.livenessProbe | object | `{"exec":{"command":["curl","-k","https://localhost:8443/health-check"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| client-api.livenessProbe.exec | object | `{"command":["curl","-k","https://localhost:8443/health-check"]}` | Executes the python3 healthcheck. |
| client-api.readinessProbe | object | `{"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":8443},"timeoutSeconds":5}` | Configure the readiness healthcheck for the auth server if needed. |
| client-api.replicas | int | `1` | Service replica number. |
| client-api.resources | object | `{"limits":{"cpu":"1000m","memory":"400Mi"},"requests":{"cpu":"1000m","memory":"400Mi"}}` | Resource specs. |
| client-api.resources.limits.cpu | string | `"1000m"` | CPU limit. |
| client-api.resources.limits.memory | string | `"400Mi"` | Memory limit. |
| client-api.resources.requests.cpu | string | `"1000m"` | CPU request. |
| client-api.resources.requests.memory | string | `"400Mi"` | Memory request. |
| client-api.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| client-api.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| client-api.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| client-api.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| client-api.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| config | object | `{"additionalAnnotations":{},"additionalLabels":{},"adminPassword":"Test1234#","city":"Austin","configmap":{"cnCacheType":"NATIVE_PERSISTENCE","cnCasaEnabled":false,"cnClientApiAdminCertCn":"client-api","cnClientApiApplicationCertCn":"client-api","cnClientApiBindIpAddresses":"*","cnConfigGoogleSecretNamePrefix":"gluu","cnConfigGoogleSecretVersionId":"latest","cnConfigKubernetesConfigMap":"cn","cnCouchbaseBucketPrefix":"jans","cnCouchbaseCertFile":"/etc/certs/couchbase.crt","cnCouchbaseCrt":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnCouchbaseIndexNumReplica":0,"cnCouchbasePassword":"P@ssw0rd","cnCouchbasePasswordFile":"/etc/gluu/conf/couchbase_password","cnCouchbaseSuperUser":"admin","cnCouchbaseSuperUserPassword":"Test1234#","cnCouchbaseSuperUserPasswordFile":"/etc/gluu/conf/couchbase_superuser_password","cnCouchbaseUrl":"cbgluu.default.svc.cluster.local","cnCouchbaseUser":"gluu","cnDocumentStoreType":"JCA","cnGoogleProjectId":"google-project-to-save-config-and-secrets-to","cnGoogleSecretManagerPassPhrase":"Test1234#","cnGoogleSecretManagerServiceAccount":"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=","cnGoogleSpannerDatabaseId":"","cnGoogleSpannerInstanceId":"","cnJackrabbitAdminId":"admin","cnJackrabbitAdminIdFile":"/etc/gluu/conf/jackrabbit_admin_id","cnJackrabbitAdminPasswordFile":"/etc/gluu/conf/jackrabbit_admin_password","cnJackrabbitPostgresDatabaseName":"jackrabbit","cnJackrabbitPostgresHost":"postgresql.postgres.svc.cluster.local","cnJackrabbitPostgresPasswordFile":"/etc/gluu/conf/postgres_password","cnJackrabbitPostgresPort":5432,"cnJackrabbitPostgresUser":"jackrabbit","cnJackrabbitSyncInterval":300,"cnJackrabbitUrl":"http://jackrabbit:8080","cnJettyRequestHeaderSize":8192,"cnLdapUrl":"opendj:1636","cnMaxRamPercent":"75.0","cnPassportEnabled":false,"cnPersistenceLdapMapping":"default","cnRedisSentinelGroup":"","cnRedisSslTruststore":"","cnRedisType":"STANDALONE","cnRedisUrl":"redis.redis.svc.cluster.local:6379","cnRedisUseSsl":false,"cnSamlEnabled":false,"cnScimProtectionMode":"OAUTH","cnSecretGoogleSecretNamePrefix":"gluu","cnSecretGoogleSecretVersionId":"latest","cnSecretKubernetesSecret":"cn","cnSqlDbDialect":"mysql","cnSqlDbHost":"my-release-mysql.default.svc.cluster.local","cnSqlDbName":"jans","cnSqlDbPort":3306,"cnSqlDbTimezone":"UTC","cnSqlDbUser":"jans","cnSqlPasswordFile":"/etc/jans/conf/sql_password","cnSqldbUserPassword":"Test1234#","lbAddr":""},"countryCode":"US","dnsConfig":{},"dnsPolicy":"","email":"support@gluu.org","image":{"pullSecrets":[],"repository":"janssenproject/configurator","tag":"1.0.0-beta.14"},"ldapPassword":"P@ssw0rds","migration":{"enabled":false,"migrationDataFormat":"ldif","migrationDir":"/ce-migration"},"orgName":"Gluu","redisPassword":"P@assw0rd","resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"state":"TX","usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Configuration parameters for setup and initial configuration secret and config layers used by Gluu services. |
| config-api | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/config-api","tag":"1.0.0-beta.14"},"livenessProbe":{"httpGet":{"path":"/jans-config-api/api/v1/health/live","port":8074},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"httpGet":{"path":"jans-config-api/api/v1/health/ready","port":8074},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"400Mi"},"requests":{"cpu":"1000m","memory":"400Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Config Api endpoints can be used to configure the auth-server, which is an open-source OpenID Connect Provider (OP) and UMA Authorization Server (AS). |
| config-api.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| config-api.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| config-api.dnsConfig | object | `{}` | Add custom dns config |
| config-api.dnsPolicy | string | `""` | Add custom dns policy |
| config-api.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| config-api.hpa.behavior | object | `{}` | Scaling Policies |
| config-api.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| config-api.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| config-api.image.pullSecrets | list | `[]` | Image Pull Secrets |
| config-api.image.repository | string | `"janssenproject/config-api"` | Image  to use for deploying. |
| config-api.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
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
| config.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| config.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| config.adminPassword | string | `"Test1234#"` | Admin password to log in to the UI. |
| config.city | string | `"Austin"` | City. Used for certificate creation. |
| config.configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| config.configmap.cnCasaEnabled | bool | `false` | Enable Casa flag . |
| config.configmap.cnClientApiAdminCertCn | string | `"client-api"` | Client-api OAuth client admin certificate common name. This should be left to the default value client-api . |
| config.configmap.cnClientApiApplicationCertCn | string | `"client-api"` | Client-api OAuth client application certificate common name. This should be left to the default value client-api. |
| config.configmap.cnClientApiBindIpAddresses | string | `"*"` | Client-api bind address. This limits what ip ranges can access the client-api. This should be left as * and controlled by a NetworkPolicy |
| config.configmap.cnConfigGoogleSecretNamePrefix | string | `"gluu"` | Prefix for Gluu configuration secret in Google Secret Manager. Defaults to gluu. If left intact gluu-configuration secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnConfigGoogleSecretVersionId | string | `"latest"` | Secret version to be used for configuration. Defaults to latest and should normally always stay that way. Used only when global.configAdapterName and global.configSecretAdapter is set to google. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| config.configmap.cnCouchbaseBucketPrefix | string | `"jans"` | The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Gluu. |
| config.configmap.cnCouchbaseCertFile | string | `"/etc/certs/couchbase.crt"` | Location of `couchbase.crt` used by Couchbase SDK for tls termination. The file path must end with couchbase.crt. In mTLS setups this is not required. |
| config.configmap.cnCouchbaseCrt | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Couchbase certificate authority string. This must be encoded using base64. This can also be found in your couchbase UI Security > Root Certificate. In mTLS setups this is not required. |
| config.configmap.cnCouchbaseIndexNumReplica | int | `0` | The number of replicas per index created. Please note that the number of index nodes must be one greater than the number of index replicas. That means if your couchbase cluster only has 2 index nodes you cannot place the number of replicas to be higher than 1. |
| config.configmap.cnCouchbasePassword | string | `"P@ssw0rd"` | Couchbase password for the restricted user config.configmap.cnCouchbaseUser  that is often used inside the services. The password must contain one digit, one uppercase letter, one lower case letter and one symbol . |
| config.configmap.cnCouchbasePasswordFile | string | `"/etc/gluu/conf/couchbase_password"` | The location of the Couchbase restricted user config.configmap.cnCouchbaseUser password. The file path must end with couchbase_password |
| config.configmap.cnCouchbaseSuperUser | string | `"admin"` | The Couchbase super user (admin) user name. This user is used during initialization only. |
| config.configmap.cnCouchbaseSuperUserPassword | string | `"Test1234#"` | Couchbase password for the super user config.configmap.cnCouchbaseSuperUser  that is used during the initialization process. The password must contain one digit, one uppercase letter, one lower case letter and one symbol |
| config.configmap.cnCouchbaseSuperUserPasswordFile | string | `"/etc/gluu/conf/couchbase_superuser_password"` | The location of the Couchbase restricted user config.configmap.cnCouchbaseSuperUser password. The file path must end with couchbase_superuser_password. |
| config.configmap.cnCouchbaseUrl | string | `"cbgluu.default.svc.cluster.local"` | Couchbase URL. Used only when global.cnPersistenceType is hybrid or couchbase. This should be in FQDN format for either remote or local Couchbase clusters. The address can be an internal address inside the kubernetes cluster |
| config.configmap.cnCouchbaseUser | string | `"gluu"` | Couchbase restricted user. Used only when global.cnPersistenceType is hybrid or couchbase. |
| config.configmap.cnDocumentStoreType | string | `"JCA"` | Document store type to use for shibboleth files JCA or LOCAL. Note that if JCA is selected Apache Jackrabbit will be used. Jackrabbit also enables loading custom files across all services easily. |
| config.configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the google project the secret manager belongs to. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSecretManagerPassPhrase | string | `"Test1234#"` | Passphrase for Gluu secret in Google Secret Manager. This is used for encrypting and decrypting data from the Google Secret Manager. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
| config.configmap.cnGoogleSpannerDatabaseId | string | `""` | Google Spanner Database ID. Used only when global.cnPersistenceType is spanner. |
| config.configmap.cnJackrabbitAdminId | string | `"admin"` | Jackrabbit admin uid. |
| config.configmap.cnJackrabbitAdminIdFile | string | `"/etc/gluu/conf/jackrabbit_admin_id"` | The location of the Jackrabbit admin uid config.cnJackrabbitAdminId. The file path must end with jackrabbit_admin_id. |
| config.configmap.cnJackrabbitAdminPasswordFile | string | `"/etc/gluu/conf/jackrabbit_admin_password"` | The location of the Jackrabbit admin password jackrabbit.secrets.cnJackrabbitAdminPassword. The file path must end with jackrabbit_admin_password. |
| config.configmap.cnJackrabbitPostgresDatabaseName | string | `"jackrabbit"` | Jackrabbit postgres database name. |
| config.configmap.cnJackrabbitPostgresHost | string | `"postgresql.postgres.svc.cluster.local"` | Postgres url |
| config.configmap.cnJackrabbitPostgresPasswordFile | string | `"/etc/gluu/conf/postgres_password"` | The location of the Jackrabbit postgres password file jackrabbit.secrets.cnJackrabbitPostgresPassword. The file path must end with postgres_password. |
| config.configmap.cnJackrabbitPostgresPort | int | `5432` | Jackrabbit Postgres port |
| config.configmap.cnJackrabbitPostgresUser | string | `"jackrabbit"` | Jackrabbit Postgres uid |
| config.configmap.cnJackrabbitSyncInterval | int | `300` | Interval between files sync (default to 300 seconds). |
| config.configmap.cnJackrabbitUrl | string | `"http://jackrabbit:8080"` | Jackrabbit internal url. Normally left as default. |
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
| config.configmap.cnScimProtectionMode | string | `"OAUTH"` | SCIM protection mode OAUTH|TEST|UMA |
| config.configmap.cnSecretGoogleSecretNamePrefix | string | `"gluu"` | Prefix for Gluu secret in Google Secret Manager. Defaults to gluu. If left gluu-secret secret will be created. Used only when global.configAdapterName and global.configSecretAdapter is set to google. |
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
| config.email | string | `"support@gluu.org"` | Email address of the administrator usually. Used for certificate creation. |
| config.image.pullSecrets | list | `[]` | Image Pull Secrets |
| config.image.repository | string | `"janssenproject/configurator"` | Image  to use for deploying. |
| config.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
| config.ldapPassword | string | `"P@ssw0rds"` | LDAP admin password if OpennDJ is used for persistence. |
| config.migration | object | `{"enabled":false,"migrationDataFormat":"ldif","migrationDir":"/ce-migration"}` | CE to CN Migration section |
| config.migration.enabled | bool | `false` | Boolean flag to enable migration from CE |
| config.migration.migrationDataFormat | string | `"ldif"` | migration data-format depending on persistence backend. Supported data formats are ldif, couchbase+json, spanner+avro, postgresql+json, and mysql+json. |
| config.migration.migrationDir | string | `"/ce-migration"` | Directory holding all migration files |
| config.orgName | string | `"Gluu"` | Organization name. Used for certificate creation. |
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
| cr-rotate | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/cr-rotate","tag":"5.0.0_dev"},"resources":{"limits":{"cpu":"200m","memory":"200Mi"},"requests":{"cpu":"200m","memory":"200Mi"}},"service":{"crRotateServiceName":"cr-rotate"},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | CacheRefreshRotation is a special container to monitor cache refresh on oxTrust containers. This may be depreciated. |
| cr-rotate.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| cr-rotate.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| cr-rotate.dnsConfig | object | `{}` | Add custom dns config |
| cr-rotate.dnsPolicy | string | `""` | Add custom dns policy |
| cr-rotate.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| cr-rotate.image.pullSecrets | list | `[]` | Image Pull Secrets |
| cr-rotate.image.repository | string | `"gluufederation/cr-rotate"` | Image  to use for deploying. |
| cr-rotate.image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| cr-rotate.resources | object | `{"limits":{"cpu":"200m","memory":"200Mi"},"requests":{"cpu":"200m","memory":"200Mi"}}` | Resource specs. |
| cr-rotate.resources.limits.cpu | string | `"200m"` | CPU limit. |
| cr-rotate.resources.limits.memory | string | `"200Mi"` | Memory limit. |
| cr-rotate.resources.requests.cpu | string | `"200m"` | CPU request. |
| cr-rotate.resources.requests.memory | string | `"200Mi"` | Memory request. |
| cr-rotate.service.crRotateServiceName | string | `"cr-rotate"` | Name of the cr-rotate service. Please keep it as default. |
| cr-rotate.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| cr-rotate.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| cr-rotate.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| cr-rotate.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| cr-rotate.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| fido2 | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/fido2","tag":"1.0.0-beta.14"},"livenessProbe":{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"readinessProbe":{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}},"service":{"name":"http-fido2","port":8080},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | FIDO 2.0 (FIDO2) is an open authentication standard that enables leveraging common devices to authenticate to online services in both mobile and desktop environments. |
| fido2.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| fido2.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| fido2.dnsConfig | object | `{}` | Add custom dns config |
| fido2.dnsPolicy | string | `""` | Add custom dns policy |
| fido2.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| fido2.hpa.behavior | object | `{}` | Scaling Policies |
| fido2.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| fido2.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| fido2.image.pullSecrets | list | `[]` | Image Pull Secrets |
| fido2.image.repository | string | `"janssenproject/fido2"` | Image  to use for deploying. |
| fido2.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
| fido2.livenessProbe | object | `{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the liveness healthcheck for the fido2 if needed. |
| fido2.livenessProbe.httpGet | object | `{"path":"/jans-fido2/sys/health-check","port":"http-fido2"}` | http liveness probe endpoint |
| fido2.readinessProbe | object | `{"httpGet":{"path":"/jans-fido2/sys/health-check","port":"http-fido2"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the readiness healthcheck for the fido2 if needed. |
| fido2.replicas | int | `1` | Service replica number. |
| fido2.resources | object | `{"limits":{"cpu":"500m","memory":"500Mi"},"requests":{"cpu":"500m","memory":"500Mi"}}` | Resource specs. |
| fido2.resources.limits.cpu | string | `"500m"` | CPU limit. |
| fido2.resources.limits.memory | string | `"500Mi"` | Memory limit. |
| fido2.resources.requests.cpu | string | `"500m"` | CPU request. |
| fido2.resources.requests.memory | string | `"500Mi"` | Memory request. |
| fido2.service.name | string | `"http-fido2"` | The name of the fido2 port within the fido2 service. Please keep it as default. |
| fido2.service.port | int | `8080` | Port of the fido2 service. Please keep it as default. |
| fido2.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| fido2.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| fido2.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| fido2.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| fido2.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| global | object | `{"admin-ui":{"adminUiApiKey":"xxxxxxxxxxx","adminUiApiKeyFile":"/etc/jans/conf/admin_ui_api_key","adminUiManagementKey":"xxxxxxxxxxx","adminUiManagementKeyFile":"/etc/jans/conf/admin_ui_management_key","adminUiProductCode":"xxxxxxxxxxx","adminUiProductCodeFile":"/etc/jans/conf/admin_ui_product_code","adminUiServiceName":"admin-ui","adminUiSharedKey":"xxxxxxxxxxx","adminUiSharedKeyFile":"/etc/jans/conf/admin_ui_shared_key","enabled":false},"alb":{"ingress":false},"auth-server":{"appLoggers":{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","httpLogLevel":"INFO","httpLogTarget":"FILE","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"authEncKeys":"RSA1_5 RSA-OAEP","authServerServiceName":"auth-server","authSigKeys":"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512","enabled":true},"auth-server-key-rotation":{"enabled":false},"awsStorageType":"io1","azureStorageAccountType":"Standard_LRS","azureStorageKind":"Managed","casa":{"casaServiceName":"casa"},"client-api":{"appLoggers":{"clientApiLogLevel":"INFO","clientApiLogTarget":"STDOUT"},"clientApiServerServiceName":"client-api","enabled":false},"cloud":{"testEnviroment":false},"cnGoogleApplicationCredentials":"/etc/jans/conf/google-credentials.json","cnJackrabbitCluster":false,"cnObExtSigningAlias":"","cnObExtSigningJwksCrt":"","cnObExtSigningJwksKey":"","cnObExtSigningJwksKeyPassPhrase":"","cnObExtSigningJwksUri":"","cnObStaticSigningKeyKid":"","cnObTransportAlias":"","cnObTransportCrt":"","cnObTransportKey":"","cnObTransportKeyPassPhrase":"","cnObTransportTrustStore":"","cnPersistenceType":"sql","config":{"enabled":true},"config-api":{"appLoggers":{"configApiLogLevel":"INFO","configApiLogTarget":"STDOUT"},"configApiServerServiceName":"config-api","enabled":true},"configAdapterName":"kubernetes","configSecretAdapter":"kubernetes","cr-rotate":{"enabled":false},"distribution":"default","fido2":{"appLoggers":{"fido2LogLevel":"INFO","fido2LogTarget":"STDOUT","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE"},"enabled":true,"fido2ServiceName":"fido2"},"fqdn":"demoexample.gluu.org","gcePdStorageType":"pd-standard","isFqdnRegistered":false,"istio":{"additionalAnnotations":{},"additionalLabels":{},"enabled":false,"ingress":false,"namespace":"istio-system"},"jackrabbit":{"enabled":false,"jackRabbitServiceName":"jackrabbit"},"lbIp":"22.22.22.22","nginx-ingress":{"enabled":true},"opendj":{"enabled":false,"ldapServiceName":"opendj"},"oxpassport":{"oxPassportServiceName":"oxpassport"},"oxshibboleth":{"enabled":false,"oxShibbolethServiceName":"oxshibboleth"},"persistence":{"enabled":true},"scim":{"appLoggers":{"ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scimLogLevel":"INFO","scimLogTarget":"STDOUT","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"enabled":true,"scimServiceName":"scim"},"storageClass":{"allowVolumeExpansion":true,"allowedTopologies":[],"mountOptions":["debug"],"parameters":{},"provisioner":"microk8s.io/hostpath","reclaimPolicy":"Retain","volumeBindingMode":"WaitForFirstConsumer"},"upgrade":{"enabled":false},"usrEnvs":{"normal":{},"secret":{}}}` | Parameters used globally across all services helm charts. |
| global.admin-ui.adminUiApiKeyFile | string | `"/etc/jans/conf/admin_ui_api_key"` | Admin UI license API key mount location. |
| global.admin-ui.adminUiManagementKey | string | `"xxxxxxxxxxx"` | Admin UI license management key. |
| global.admin-ui.adminUiManagementKeyFile | string | `"/etc/jans/conf/admin_ui_management_key"` | Admin UI license management key mount location. |
| global.admin-ui.adminUiProductCode | string | `"xxxxxxxxxxx"` | Admin UI license product code. |
| global.admin-ui.adminUiProductCodeFile | string | `"/etc/jans/conf/admin_ui_product_code"` | Admin UI license product code mount location. |
| global.admin-ui.adminUiServiceName | string | `"admin-ui"` | Name of the admin-ui service. Please keep it as default. |
| global.admin-ui.adminUiSharedKey | string | `"xxxxxxxxxxx"` | Admin UI license shared key. |
| global.admin-ui.adminUiSharedKeyFile | string | `"/etc/jans/conf/admin_ui_shared_key"` | Admin UI license shared key mount location. |
| global.admin-ui.enabled | bool | `false` | Boolean flag to enable/disable the admin-ui chart and admin ui config api plugin. |
| global.alb.ingress | bool | `false` | Activates ALB ingress |
| global.auth-server-key-rotation.enabled | bool | `false` | Boolean flag to enable/disable the auth-server-key rotation cronjob chart. |
| global.auth-server.appLoggers | object | `{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","httpLogLevel":"INFO","httpLogTarget":"FILE","ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.auth-server.appLoggers.auditStatsLogLevel | string | `"INFO"` | jans-auth_audit.log level |
| global.auth-server.appLoggers.auditStatsLogTarget | string | `"FILE"` | jans-auth_script.log target |
| global.auth-server.appLoggers.authLogLevel | string | `"INFO"` | jans-auth.log level |
| global.auth-server.appLoggers.authLogTarget | string | `"STDOUT"` | jans-auth.log target |
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
| global.awsStorageType | string | `"io1"` | Volume storage type if using AWS volumes. |
| global.azureStorageAccountType | string | `"Standard_LRS"` | Volume storage type if using Azure disks. |
| global.azureStorageKind | string | `"Managed"` | Azure storage kind if using Azure disks |
| global.casa.casaServiceName | string | `"casa"` | Name of the casa service. Please keep it as default. |
| global.client-api.appLoggers | object | `{"clientApiLogLevel":"INFO","clientApiLogTarget":"STDOUT"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.client-api.appLoggers.clientApiLogLevel | string | `"INFO"` | client-api.log level |
| global.client-api.appLoggers.clientApiLogTarget | string | `"STDOUT"` | client-api.log target |
| global.client-api.clientApiServerServiceName | string | `"client-api"` | Name of the client-api service. Please keep it as default. |
| global.client-api.enabled | bool | `false` | Boolean flag to enable/disable the client-api chart. |
| global.cloud.testEnviroment | bool | `false` | Boolean flag if enabled will strip resources requests and limits from all services. |
| global.cnGoogleApplicationCredentials | string | `"/etc/jans/conf/google-credentials.json"` | Base64 encoded service account. The sa must have roles/secretmanager.admin to use Google secrets and roles/spanner.databaseUser to use Spanner. |
| global.cnJackrabbitCluster | bool | `false` | Boolean flag if enabled will enable jackrabbit in cluster mode with Postgres. |
| global.cnObExtSigningAlias | string | `""` | Open banking external signing AS Alias. This is a kid value.Used in SSA Validation, kid used while encoding a JWT sent to token URL i.e XkwIzWy44xWSlcWnMiEc8iq9s2G |
| global.cnObExtSigningJwksCrt | string | `""` | Open banking external signing jwks AS certificate authority string. Used in SSA Validation. This must be encoded using base64.. Used when `.global.cnObExtSigningJwksUri` is set. |
| global.cnObExtSigningJwksKey | string | `""` | Open banking external signing jwks AS key string. Used in SSA Validation. This must be encoded using base64. Used when `.global.cnObExtSigningJwksUri` is set. |
| global.cnObExtSigningJwksKeyPassPhrase | string | `""` | Open banking external signing jwks AS key passphrase to unlock provided key. This must be encoded using base64. Used when `.global.cnObExtSigningJwksUri` is set. |
| global.cnObExtSigningJwksUri | string | `""` | Open banking external signing jwks uri. Used in SSA Validation. |
| global.cnObStaticSigningKeyKid | string | `""` | Open banking  signing AS kid to force the AS to use a specific signing key. i.e Wy44xWSlcWnMiEc8iq9s2G |
| global.cnObTransportAlias | string | `""` | Open banking transport Alias used inside the JVM. |
| global.cnObTransportCrt | string | `""` | Open banking AS transport crt. Used in SSA Validation. This must be encoded using base64. |
| global.cnObTransportKey | string | `""` | Open banking AS transport key. Used in SSA Validation. This must be encoded using base64. |
| global.cnObTransportKeyPassPhrase | string | `""` | Open banking AS transport key pas`sphrase to unlock AS transport key. This must be encoded using base64. |
| global.cnObTransportTrustStore | string | `""` | Open banking AS transport truststore crt. This is normally generated from the OB issuing CA, OB Root CA and Signing CA. Used when .global.cnObExtSigningJwksUri is set. Used in SSA Validation. This must be encoded using base64. |
| global.cnPersistenceType | string | `"sql"` | Persistence backend to run Gluu with ldap|couchbase|hybrid|sql|spanner. |
| global.config-api.appLoggers | object | `{"configApiLogLevel":"INFO","configApiLogTarget":"STDOUT"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.config-api.appLoggers.configApiLogLevel | string | `"INFO"` | configapi.log level |
| global.config-api.appLoggers.configApiLogTarget | string | `"STDOUT"` | configapi.log target |
| global.config-api.configApiServerServiceName | string | `"config-api"` | Name of the config-api service. Please keep it as default. |
| global.config-api.enabled | bool | `true` | Boolean flag to enable/disable the config-api chart. |
| global.config.enabled | bool | `true` | Boolean flag to enable/disable the configuration chart. This normally should never be false |
| global.configAdapterName | string | `"kubernetes"` | The config backend adapter that will hold Gluu configuration layer. google|kubernetes |
| global.configSecretAdapter | string | `"kubernetes"` | The config backend adapter that will hold Gluu secret layer. google|kubernetes |
| global.cr-rotate.enabled | bool | `false` | Boolean flag to enable/disable the cr-rotate chart. |
| global.distribution | string | `"default"` | Gluu distributions supported are: default|openbanking. |
| global.fido2.appLoggers | object | `{"fido2LogLevel":"INFO","fido2LogTarget":"STDOUT","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| global.fido2.appLoggers.fido2LogLevel | string | `"INFO"` | fido2.log level |
| global.fido2.appLoggers.fido2LogTarget | string | `"STDOUT"` | fido2.log target |
| global.fido2.appLoggers.persistenceLogLevel | string | `"INFO"` | fido2_persistence.log level |
| global.fido2.appLoggers.persistenceLogTarget | string | `"FILE"` | fido2_persistence.log target |
| global.fido2.enabled | bool | `true` | Boolean flag to enable/disable the fido2 chart. |
| global.fido2.fido2ServiceName | string | `"fido2"` | Name of the fido2 service. Please keep it as default. |
| global.fqdn | string | `"demoexample.gluu.org"` | Fully qualified domain name to be used for Gluu installation. This address will be used to reach Gluu services. |
| global.gcePdStorageType | string | `"pd-standard"` | GCE storage kind if using Google disks |
| global.isFqdnRegistered | bool | `false` | Boolean flag to enable mapping global.lbIp  to global.fqdn inside pods on clouds that provide static ip for loadbalancers. On cloud that provide only addresses to the LB this flag will enable a script to actively scan config.configmap.lbAddr and update the hosts file inside the pods automatically. |
| global.istio.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| global.istio.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| global.istio.enabled | bool | `false` | Boolean flag that enables using istio side cars with Gluu services. |
| global.istio.ingress | bool | `false` | Boolean flag that enables using istio gateway for Gluu. This assumes istio ingress is installed and hence the LB is available. |
| global.istio.namespace | string | `"istio-system"` | The namespace istio is deployed in. The is normally istio-system. |
| global.jackrabbit.enabled | bool | `false` | Boolean flag to enable/disable the jackrabbit chart. For more information on how it is used inside Gluu https://gluu.org/docs/gluu-server/4.2/installation-guide/install-kubernetes/#working-with-jackrabbit. If disabled oxShibboleth cannot be run. |
| global.jackrabbit.jackRabbitServiceName | string | `"jackrabbit"` | Name of the Jackrabbit service. Please keep it as default. |
| global.lbIp | string | `"22.22.22.22"` | The Loadbalancer IP created by nginx or istio on clouds that provide static IPs. This is not needed if `global.fqdn` is globally resolvable. |
| global.nginx-ingress.enabled | bool | `true` | Boolean flag to enable/disable the nginx-ingress definitions chart. |
| global.opendj.enabled | bool | `false` | Boolean flag to enable/disable the OpenDJ  chart. |
| global.opendj.ldapServiceName | string | `"opendj"` | Name of the OpenDJ service. Please keep it as default. |
| global.oxpassport.oxPassportServiceName | string | `"oxpassport"` | Name of the oxPassport service. Please keep it as default. |
| global.oxshibboleth.enabled | bool | `false` | Boolean flag to enable/disable the oxShibbboleth chart. |
| global.oxshibboleth.oxShibbolethServiceName | string | `"oxshibboleth"` | Name of the oxShibboleth service. Please keep it as default. |
| global.persistence.enabled | bool | `true` | Boolean flag to enable/disable the persistence chart. |
| global.scim.appLoggers | object | `{"ldapStatsLogLevel":"INFO","ldapStatsLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scimLogLevel":"INFO","scimLogTarget":"STDOUT","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
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
| global.scim.scimServiceName | string | `"scim"` | Name of the scim service. Please keep it as default. |
| global.storageClass | object | `{"allowVolumeExpansion":true,"allowedTopologies":[],"mountOptions":["debug"],"parameters":{},"provisioner":"microk8s.io/hostpath","reclaimPolicy":"Retain","volumeBindingMode":"WaitForFirstConsumer"}` | StorageClass section for Jackrabbit and OpenDJ charts. This is not currently used by the openbanking distribution. You may specify custom parameters as needed. |
| global.storageClass.parameters | object | `{}` | parameters: |
| global.upgrade.enabled | bool | `false` | Boolean flag used when running upgrading through versions command. Used when upgrading with LDAP as the persistence to load the 101x ldif. |
| global.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service. Envs defined in global.userEnvs will be globally available to all services |
| global.usrEnvs.normal | object | `{}` | Add custom normal envs to the service. variable1: value1 |
| global.usrEnvs.secret | object | `{}` | Add custom secret envs to the service. variable1: value1 |
| installer-settings | object | `{"acceptLicense":"","aws":{"arn":{"arnAcmCert":"","enabled":""},"lbType":"","vpcCidr":"0.0.0.0/0"},"confirmSettings":false,"couchbase":{"backup":{"fullSchedule":"","incrementalSchedule":"","retentionTime":"","storageSize":""},"clusterName":"","commonName":"","customFileOverride":"","install":"","lowResourceInstall":"","namespace":"","subjectAlternativeName":"","totalNumberOfExpectedTransactionsPerSec":"","totalNumberOfExpectedUsers":"","volumeType":""},"currentVersion":"","google":{"useSecretManager":""},"images":{"edit":""},"jackrabbit":{"clusterMode":""},"ldap":{"backup":{"fullSchedule":""},"multiClusterIds":[],"subsequentCluster":""},"namespace":"","nginxIngress":{"namespace":"","releaseName":""},"nodes":{"ips":"","names":"","zones":""},"openbanking":{"cnObTransportTrustStoreP12password":"","hasCnObTransportTrustStore":false},"postgres":{"install":"","namespace":""},"redis":{"install":"","namespace":""},"releaseName":"","sql":{"install":"","namespace":""},"upgrade":{"image":{"repository":"","tag":""},"targetVersion":""},"volumeProvisionStrategy":""}` | Only used by the installer. These settings do not affect nor are used by the chart |
| jackrabbit | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/jackrabbit","tag":"5.0.0_dev"},"livenessProbe":{"initialDelaySeconds":25,"periodSeconds":25,"tcpSocket":{"port":"http-jackrabbit"},"timeoutSeconds":5},"readinessProbe":{"initialDelaySeconds":30,"periodSeconds":30,"tcpSocket":{"port":"http-jackrabbit"},"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1500m","memory":"1000Mi"},"requests":{"cpu":"1500m","memory":"1000Mi"}},"secrets":{"cnJackrabbitAdminPassword":"Test1234#","cnJackrabbitPostgresPassword":"P@ssw0rd"},"storage":{"size":"5Gi"},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Jackrabbit Oak is a complementary implementation of the JCR specification. It is an effort to implement a scalable and performant hierarchical content repository for use as the foundation of modern world-class web sites and other demanding content applications https://jackrabbit.apache.org/jcr/index.html |
| jackrabbit.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| jackrabbit.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| jackrabbit.dnsConfig | object | `{}` | Add custom dns config |
| jackrabbit.dnsPolicy | string | `""` | Add custom dns policy |
| jackrabbit.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| jackrabbit.hpa.behavior | object | `{}` | Scaling Policies |
| jackrabbit.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| jackrabbit.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| jackrabbit.image.pullSecrets | list | `[]` | Image Pull Secrets |
| jackrabbit.image.repository | string | `"gluufederation/jackrabbit"` | Image  to use for deploying. |
| jackrabbit.image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| jackrabbit.livenessProbe | object | `{"initialDelaySeconds":25,"periodSeconds":25,"tcpSocket":{"port":"http-jackrabbit"},"timeoutSeconds":5}` | Configure the liveness healthcheck for the Jackrabbit if needed. |
| jackrabbit.livenessProbe.tcpSocket | object | `{"port":"http-jackrabbit"}` | Executes tcp healthcheck. |
| jackrabbit.readinessProbe | object | `{"initialDelaySeconds":30,"periodSeconds":30,"tcpSocket":{"port":"http-jackrabbit"},"timeoutSeconds":5}` | Configure the readiness healthcheck for the Jackrabbit if needed. |
| jackrabbit.readinessProbe.tcpSocket | object | `{"port":"http-jackrabbit"}` | Executes tcp healthcheck. |
| jackrabbit.replicas | int | `1` | Service replica number. |
| jackrabbit.resources | object | `{"limits":{"cpu":"1500m","memory":"1000Mi"},"requests":{"cpu":"1500m","memory":"1000Mi"}}` | Resource specs. |
| jackrabbit.resources.limits.cpu | string | `"1500m"` | CPU limit. |
| jackrabbit.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| jackrabbit.resources.requests.cpu | string | `"1500m"` | CPU request. |
| jackrabbit.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| jackrabbit.secrets.cnJackrabbitAdminPassword | string | `"Test1234#"` | Jackrabbit admin uid password |
| jackrabbit.secrets.cnJackrabbitPostgresPassword | string | `"P@ssw0rd"` | Jackrabbit Postgres uid password |
| jackrabbit.storage.size | string | `"5Gi"` | Jackrabbit volume size |
| jackrabbit.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| jackrabbit.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| jackrabbit.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| jackrabbit.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| jackrabbit.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| nginx-ingress | object | `{"ingress":{"additionalAnnotations":{},"additionalLabels":{},"adminUiAdditionalAnnotations":{},"adminUiEnabled":false,"adminUiLabels":{},"authServerAdditionalAnnotations":{},"authServerEnabled":true,"authServerLabels":{},"authServerProtectedRegister":false,"authServerProtectedRegisterAdditionalAnnotations":{},"authServerProtectedRegisterLabels":{},"authServerProtectedToken":false,"authServerProtectedTokenAdditionalAnnotations":{},"authServerProtectedTokenLabels":{},"configApiAdditionalAnnotations":{},"configApiEnabled":true,"configApiLabels":{},"fido2ConfigAdditionalAnnotations":{},"fido2ConfigEnabled":false,"fido2ConfigLabels":{},"hosts":["demoexample.gluu.org"],"openidAdditionalAnnotations":{},"openidConfigEnabled":true,"openidConfigLabels":{},"path":"/","scimAdditionalAnnotations":{},"scimConfigAdditionalAnnotations":{},"scimConfigEnabled":false,"scimConfigLabels":{},"scimEnabled":false,"scimLabels":{},"tls":[{"hosts":["demoexample.gluu.org"],"secretName":"tls-certificate"}],"u2fAdditionalAnnotations":{},"u2fConfigEnabled":true,"u2fConfigLabels":{},"uma2AdditionalAnnotations":{},"uma2ConfigEnabled":true,"uma2ConfigLabels":{},"webdiscoveryAdditionalAnnotations":{},"webdiscoveryEnabled":true,"webdiscoveryLabels":{},"webfingerAdditionalAnnotations":{},"webfingerEnabled":true,"webfingerLabels":{}}}` | Nginx ingress definitions chart |
| nginx-ingress.ingress.additionalAnnotations | object | `{}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"} Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "gluu/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| nginx-ingress.ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| nginx-ingress.ingress.adminUiAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| nginx-ingress.ingress.adminUiEnabled | bool | `false` | Enable Admin UI endpoints. COMING SOON. |
| nginx-ingress.ingress.adminUiLabels | object | `{}` | Admin UI ingress resource labels. key app is taken. |
| nginx-ingress.ingress.authServerAdditionalAnnotations | object | `{}` | Auth server ingress resource additional annotations. |
| nginx-ingress.ingress.authServerEnabled | bool | `true` | Enable Auth server endpoints /jans-auth |
| nginx-ingress.ingress.authServerLabels | object | `{}` | Auth server ingress resource labels. key app is taken |
| nginx-ingress.ingress.authServerProtectedRegister | bool | `false` | Enable mTLS onn Auth server endpoint /jans-auth/restv1/register |
| nginx-ingress.ingress.authServerProtectedRegisterAdditionalAnnotations | object | `{}` | Auth server protected register ingress resource additional annotations. |
| nginx-ingress.ingress.authServerProtectedRegisterLabels | object | `{}` | Auth server protected token ingress resource labels. key app is taken |
| nginx-ingress.ingress.authServerProtectedToken | bool | `false` | Enable mTLS on Auth server endpoint /jans-auth/restv1/token |
| nginx-ingress.ingress.authServerProtectedTokenAdditionalAnnotations | object | `{}` | Auth server protected token ingress resource additional annotations. |
| nginx-ingress.ingress.authServerProtectedTokenLabels | object | `{}` | Auth server protected token ingress resource labels. key app is taken |
| nginx-ingress.ingress.configApiAdditionalAnnotations | object | `{}` | ConfigAPI ingress resource additional annotations. |
| nginx-ingress.ingress.configApiLabels | object | `{}` | configAPI ingress resource labels. key app is taken |
| nginx-ingress.ingress.fido2ConfigAdditionalAnnotations | object | `{}` | fido2 config ingress resource additional annotations. |
| nginx-ingress.ingress.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| nginx-ingress.ingress.fido2ConfigLabels | object | `{}` | fido2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.openidAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| nginx-ingress.ingress.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| nginx-ingress.ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimAdditionalAnnotations | object | `{}` | SCIM ingress resource additional annotations. |
| nginx-ingress.ingress.scimConfigAdditionalAnnotations | object | `{}` | SCIM config ingress resource additional annotations. |
| nginx-ingress.ingress.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| nginx-ingress.ingress.scimConfigLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.scimEnabled | bool | `false` | Enable SCIM endpoints /jans-scim |
| nginx-ingress.ingress.scimLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| nginx-ingress.ingress.tls | list | `[{"hosts":["demoexample.gluu.org"],"secretName":"tls-certificate"}]` | Secrets holding HTTPS CA cert and key. |
| nginx-ingress.ingress.u2fAdditionalAnnotations | object | `{}` | u2f config ingress resource additional annotations. |
| nginx-ingress.ingress.u2fConfigEnabled | bool | `true` | Enable endpoint /.well-known/fido-configuration |
| nginx-ingress.ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| nginx-ingress.ingress.uma2AdditionalAnnotations | object | `{}` | uma2 config ingress resource additional annotations. |
| nginx-ingress.ingress.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| nginx-ingress.ingress.uma2ConfigLabels | object | `{}` | uma2 config ingress resource labels. key app is taken |
| nginx-ingress.ingress.webdiscoveryAdditionalAnnotations | object | `{}` | webdiscovery ingress resource additional annotations. |
| nginx-ingress.ingress.webdiscoveryEnabled | bool | `true` | Enable endpoint /.well-known/simple-web-discovery |
| nginx-ingress.ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| nginx-ingress.ingress.webfingerAdditionalAnnotations | object | `{}` | webfinger ingress resource additional annotations. |
| nginx-ingress.ingress.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
| nginx-ingress.ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| opendj | object | `{"additionalAnnotations":{},"additionalLabels":{},"backup":{"cronJobSchedule":"*/59 * * * *","enabled":true},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/opendj","tag":"5.0.0_dev"},"livenessProbe":{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":20,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"multiCluster":{"clusterId":"","enabled":false,"namespaceIntId":0,"replicaCount":1,"serfAdvertiseAddrSuffix":"regional.gluu.org:30946","serfKey":"Z51b6PgKU1MZ75NCZOTGGoc0LP2OF3qvF6sjxHyQCYk=","serfPeers":["gluu-opendj-regional-0-regional.gluu.org:30946","gluu-opendj-regional-0-regional.gluu.org:31946"]},"persistence":{"size":"5Gi"},"ports":{"tcp-admin":{"nodePort":"","port":4444,"protocol":"TCP","targetPort":4444},"tcp-ldap":{"nodePort":"","port":1389,"protocol":"TCP","targetPort":1389},"tcp-ldaps":{"nodePort":"","port":1636,"protocol":"TCP","targetPort":1636},"tcp-repl":{"nodePort":"","port":8989,"protocol":"TCP","targetPort":8989},"tcp-serf":{"nodePort":"","port":7946,"protocol":"TCP","targetPort":7946},"udp-serf":{"nodePort":"","port":7946,"protocol":"UDP","targetPort":7946}},"readinessProbe":{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1500m","memory":"2000Mi"},"requests":{"cpu":"1500m","memory":"2000Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | OpenDJ is a directory server which implements a wide range of Lightweight Directory Access Protocol and related standards, including full compliance with LDAPv3 but also support for Directory Service Markup Language (DSMLv2).Written in Java, OpenDJ offers multi-master replication, access control, and many extensions. |
| opendj.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| opendj.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| opendj.backup | object | `{"cronJobSchedule":"*/59 * * * *","enabled":true}` | Configure ldap backup cronjob |
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
| opendj.multiCluster.clusterId | string | `""` | This id needs to be unique to each kubernetes cluster in a multi cluster setup west, east, south, north, region ...etc If left empty it will be randomly generated. |
| opendj.multiCluster.enabled | bool | `false` | Enable OpenDJ multiCluster mode. This flag enables loading keys under `opendj.multiCluster` |
| opendj.multiCluster.namespaceIntId | int | `0` | Namespace int id. This id needs to be a unique number 0-9 per gluu installation per namespace. Used when gluu is installed in the same kubernetes cluster more than once. |
| opendj.multiCluster.replicaCount | int | `1` | The number of opendj non scalabble statefulsets to create. Each pod created must be resolvable as it follows the patterm RELEASE-NAME-opendj-regional-{{statefulset pod number}}-{{ $.Values.multiCluster.serfAdvertiseAddrSuffix }} If set to 1, with a release name of gluu,  the address of the pod would be gluu-opendj-regional-0-regional.gluu.org |
| opendj.multiCluster.serfAdvertiseAddrSuffix | string | `"regional.gluu.org:30946"` | OpenDJ Serf advertise address suffix that will be added to each opendj replica. i.e RELEASE-NAME-opendj-regional-{{statefulset pod number}}-{{ $.Values.multiCluster.serfAdvertiseAddrSuffix }} |
| opendj.multiCluster.serfKey | string | `"Z51b6PgKU1MZ75NCZOTGGoc0LP2OF3qvF6sjxHyQCYk="` | Serf key. This key will automatically sync across clusters. |
| opendj.multiCluster.serfPeers | list | `["gluu-opendj-regional-0-regional.gluu.org:30946","gluu-opendj-regional-0-regional.gluu.org:31946"]` | Serf peer addresses. One per cluster. |
| opendj.persistence.size | string | `"5Gi"` | OpenDJ volume size |
| opendj.readinessProbe | object | `{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5}` | Configure the readiness healthcheck for OpenDJ if needed. https://github.com/GluuFederation/docker-opendj/blob/master/scripts/healthcheck.py |
| opendj.replicas | int | `1` | Service replica number. |
| opendj.resources | object | `{"limits":{"cpu":"1500m","memory":"2000Mi"},"requests":{"cpu":"1500m","memory":"2000Mi"}}` | Resource specs. |
| opendj.resources.limits.cpu | string | `"1500m"` | CPU limit. |
| opendj.resources.limits.memory | string | `"2000Mi"` | Memory limit. |
| opendj.resources.requests.cpu | string | `"1500m"` | CPU request. |
| opendj.resources.requests.memory | string | `"2000Mi"` | Memory request. |
| opendj.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| opendj.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| opendj.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| opendj.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| opendj.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| oxpassport | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/oxpassport","tag":"5.0.0_dev"},"livenessProbe":{"failureThreshold":20,"httpGet":{"path":"/passport/health-check","port":"http-passport"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"failureThreshold":20,"httpGet":{"path":"/passport/health-check","port":"http-passport"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"700m","memory":"900Mi"},"requests":{"cpu":"700m","memory":"900Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Gluu interface to Passport.js to support social login and inbound identity. |
| oxpassport.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| oxpassport.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| oxpassport.dnsConfig | object | `{}` | Add custom dns config |
| oxpassport.dnsPolicy | string | `""` | Add custom dns policy |
| oxpassport.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| oxpassport.hpa.behavior | object | `{}` | Scaling Policies |
| oxpassport.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| oxpassport.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| oxpassport.image.pullSecrets | list | `[]` | Image Pull Secrets |
| oxpassport.image.repository | string | `"gluufederation/oxpassport"` | Image  to use for deploying. |
| oxpassport.image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| oxpassport.livenessProbe | object | `{"failureThreshold":20,"httpGet":{"path":"/passport/health-check","port":"http-passport"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for oxPassport if needed. |
| oxpassport.livenessProbe.httpGet.path | string | `"/passport/health-check"` | http liveness probe endpoint |
| oxpassport.readinessProbe | object | `{"failureThreshold":20,"httpGet":{"path":"/passport/health-check","port":"http-passport"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the oxPassport if needed. |
| oxpassport.readinessProbe.httpGet.path | string | `"/passport/health-check"` | http readiness probe endpoint |
| oxpassport.replicas | int | `1` | Service replica number |
| oxpassport.resources | object | `{"limits":{"cpu":"700m","memory":"900Mi"},"requests":{"cpu":"700m","memory":"900Mi"}}` | Resource specs. |
| oxpassport.resources.limits.cpu | string | `"700m"` | CPU limit. |
| oxpassport.resources.limits.memory | string | `"900Mi"` | Memory limit. |
| oxpassport.resources.requests.cpu | string | `"700m"` | CPU request. |
| oxpassport.resources.requests.memory | string | `"900Mi"` | Memory request. |
| oxpassport.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| oxpassport.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| oxpassport.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| oxpassport.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| oxpassport.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| oxshibboleth | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"gluufederation/oxshibboleth","tag":"5.0.0_dev"},"livenessProbe":{"httpGet":{"path":"/idp","port":"http-oxshib"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"httpGet":{"path":"/idp","port":"http-oxshib"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"1000Mi"},"requests":{"cpu":"1000m","memory":"1000Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Shibboleth project for the Gluu Server's SAML IDP functionality. |
| oxshibboleth.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| oxshibboleth.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| oxshibboleth.dnsConfig | object | `{}` | Add custom dns config |
| oxshibboleth.dnsPolicy | string | `""` | Add custom dns policy |
| oxshibboleth.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| oxshibboleth.hpa.behavior | object | `{}` | Scaling Policies |
| oxshibboleth.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| oxshibboleth.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| oxshibboleth.image.pullSecrets | list | `[]` | Image Pull Secrets |
| oxshibboleth.image.repository | string | `"gluufederation/oxshibboleth"` | Image  to use for deploying. |
| oxshibboleth.image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| oxshibboleth.livenessProbe | object | `{"httpGet":{"path":"/idp","port":"http-oxshib"},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the oxShibboleth if needed. |
| oxshibboleth.livenessProbe.httpGet.path | string | `"/idp"` | http liveness probe endpoint |
| oxshibboleth.readinessProbe | object | `{"httpGet":{"path":"/idp","port":"http-oxshib"},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the casa if needed. |
| oxshibboleth.readinessProbe.httpGet.path | string | `"/idp"` | http liveness probe endpoint |
| oxshibboleth.replicas | int | `1` | Service replica number. |
| oxshibboleth.resources | object | `{"limits":{"cpu":"1000m","memory":"1000Mi"},"requests":{"cpu":"1000m","memory":"1000Mi"}}` | Resource specs. |
| oxshibboleth.resources.limits.cpu | string | `"1000m"` | CPU limit. |
| oxshibboleth.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| oxshibboleth.resources.requests.cpu | string | `"1000m"` | CPU request. |
| oxshibboleth.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| oxshibboleth.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| oxshibboleth.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| oxshibboleth.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| oxshibboleth.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| oxshibboleth.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| persistence | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/persistence-loader","tag":"1.0.0-beta.14"},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Job to generate data and intial config for Gluu Server persistence layer. |
| persistence.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| persistence.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| persistence.dnsConfig | object | `{}` | Add custom dns config |
| persistence.dnsPolicy | string | `""` | Add custom dns policy |
| persistence.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| persistence.image.pullSecrets | list | `[]` | Image Pull Secrets |
| persistence.image.repository | string | `"janssenproject/persistence-loader"` | Image  to use for deploying. |
| persistence.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
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
| scim | object | `{"additionalAnnotations":{},"additionalLabels":{},"dnsConfig":{},"dnsPolicy":"","hpa":{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50},"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"janssenproject/scim","tag":"1.0.0-beta.14"},"livenessProbe":{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5},"readinessProbe":{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5},"replicas":1,"resources":{"limits":{"cpu":"1000m","memory":"1000Mi"},"requests":{"cpu":"1000m","memory":"1000Mi"}},"service":{"name":"http-scim","port":8080},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | System for Cross-domain Identity Management (SCIM) version 2.0 |
| scim.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| scim.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| scim.dnsConfig | object | `{}` | Add custom dns config |
| scim.dnsPolicy | string | `""` | Add custom dns policy |
| scim.hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| scim.hpa.behavior | object | `{}` | Scaling Policies |
| scim.hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| scim.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| scim.image.pullSecrets | list | `[]` | Image Pull Secrets |
| scim.image.repository | string | `"janssenproject/scim"` | Image  to use for deploying. |
| scim.image.tag | string | `"1.0.0-beta.14"` | Image  tag to use for deploying. |
| scim.livenessProbe | object | `{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for SCIM if needed. |
| scim.livenessProbe.httpGet.path | string | `"/jans-scim/sys/health-check"` | http liveness probe endpoint |
| scim.readinessProbe | object | `{"httpGet":{"path":"/jans-scim/sys/health-check","port":8080},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the SCIM if needed. |
| scim.readinessProbe.httpGet.path | string | `"/jans-scim/sys/health-check"` | http readiness probe endpoint |
| scim.replicas | int | `1` | Service replica number. |
| scim.resources.limits.cpu | string | `"1000m"` | CPU limit. |
| scim.resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| scim.resources.requests.cpu | string | `"1000m"` | CPU request. |
| scim.resources.requests.memory | string | `"1000Mi"` | Memory request. |
| scim.service.name | string | `"http-scim"` | The name of the scim port within the scim service. Please keep it as default. |
| scim.service.port | int | `8080` | Port of the scim service. Please keep it as default. |
| scim.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| scim.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| scim.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| scim.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| scim.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.5.0](https://github.com/norwoodj/helm-docs/releases/v1.5.0)
