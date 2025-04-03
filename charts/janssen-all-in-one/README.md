# janssen-all-in-one

![Version: 2.0.0](https://img.shields.io/badge/Version-2.0.0-informational?style=flat-square) ![AppVersion: 2.0.0](https://img.shields.io/badge/AppVersion-2.0.0-informational?style=flat-square)

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
| auth-server | object | `{"appLoggers":{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","enableStdoutLogPrefix":"true","httpLogLevel":"INFO","httpLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"},"authEncKeys":"RSA1_5 RSA-OAEP","authSigKeys":"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512","cnCustomJavaOptions":"","enabled":true,"ingress":{"authServerAdditionalAnnotations":{},"authServerEnabled":true,"authServerLabels":{},"authzenAdditionalAnnotations":{},"authzenConfigEnabled":true,"authzenConfigLabels":{},"deviceCodeAdditionalAnnotations":{},"deviceCodeEnabled":true,"deviceCodeLabels":{},"firebaseMessagingAdditionalAnnotations":{},"firebaseMessagingEnabled":true,"firebaseMessagingLabels":{},"lockAdditionalAnnotations":{},"lockConfigAdditionalAnnotations":{},"lockConfigEnabled":false,"lockConfigLabels":{},"lockEnabled":false,"lockLabels":{},"openidAdditionalAnnotations":{},"openidConfigEnabled":true,"openidConfigLabels":{},"u2fAdditionalAnnotations":{},"u2fConfigEnabled":true,"u2fConfigLabels":{},"uma2AdditionalAnnotations":{},"uma2ConfigEnabled":true,"uma2ConfigLabels":{},"webdiscoveryAdditionalAnnotations":{},"webdiscoveryEnabled":true,"webdiscoveryLabels":{},"webfingerAdditionalAnnotations":{},"webfingerEnabled":true,"webfingerLabels":{}},"lockEnabled":false}` | Parameters used globally across all services helm charts. |
| auth-server-key-rotation | object | `{"additionalAnnotations":{},"additionalLabels":{},"customCommand":[],"customScripts":[],"dnsConfig":{},"dnsPolicy":"","enabled":true,"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/cloudtools","tag":"2.0.0-1"},"initKeysLife":48,"keysLife":48,"keysPushDelay":0,"keysPushStrategy":"NEWER","keysStrategy":"NEWER","lifecycle":{},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Responsible for regenerating auth-keys per x hours |
| auth-server-key-rotation.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| auth-server-key-rotation.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| auth-server-key-rotation.customCommand | list | `[]` | Add custom jobs's command. If passed, it will override the default conditional command. |
| auth-server-key-rotation.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| auth-server-key-rotation.dnsConfig | object | `{}` | Add custom dns config |
| auth-server-key-rotation.dnsPolicy | string | `""` | Add custom dns policy |
| auth-server-key-rotation.enabled | bool | `true` | Boolean flag to enable/disable the auth-server-key rotation cronjob. |
| auth-server-key-rotation.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| auth-server-key-rotation.image.pullSecrets | list | `[]` | Image Pull Secrets |
| auth-server-key-rotation.image.repository | string | `"ghcr.io/janssenproject/jans/cloudtools"` | Image  to use for deploying. |
| auth-server-key-rotation.image.tag | string | `"2.0.0-1"` | Image  tag to use for deploying. |
| auth-server-key-rotation.initKeysLife | int | `48` | The initial auth server key rotation keys life in hours |
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
| auth-server.appLoggers | object | `{"auditStatsLogLevel":"INFO","auditStatsLogTarget":"FILE","authLogLevel":"INFO","authLogTarget":"STDOUT","enableStdoutLogPrefix":"true","httpLogLevel":"INFO","httpLogTarget":"FILE","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| auth-server.appLoggers.auditStatsLogLevel | string | `"INFO"` | jans-auth_audit.log level |
| auth-server.appLoggers.auditStatsLogTarget | string | `"FILE"` | jans-auth_script.log target |
| auth-server.appLoggers.authLogLevel | string | `"INFO"` | jans-auth.log level |
| auth-server.appLoggers.authLogTarget | string | `"STDOUT"` | jans-auth.log target |
| auth-server.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e auth-server-script ===> 2022-12-20 17:49:55,744 INFO |
| auth-server.appLoggers.httpLogLevel | string | `"INFO"` | http_request_response.log level |
| auth-server.appLoggers.httpLogTarget | string | `"FILE"` | http_request_response.log target |
| auth-server.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | jans-auth_persistence_duration.log level |
| auth-server.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | jans-auth_persistence_duration.log target |
| auth-server.appLoggers.persistenceLogLevel | string | `"INFO"` | jans-auth_persistence.log level |
| auth-server.appLoggers.persistenceLogTarget | string | `"FILE"` | jans-auth_persistence.log target |
| auth-server.appLoggers.scriptLogLevel | string | `"INFO"` | jans-auth_script.log level |
| auth-server.appLoggers.scriptLogTarget | string | `"FILE"` | jans-auth_script.log target |
| auth-server.authEncKeys | string | `"RSA1_5 RSA-OAEP"` | space-separated key algorithm for encryption (default to `RSA1_5 RSA-OAEP`) |
| auth-server.authSigKeys | string | `"RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512"` | space-separated key algorithm for signing (default to `RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512`) |
| auth-server.cnCustomJavaOptions | string | `""` | passing custom java options to auth-server. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| auth-server.enabled | bool | `true` | Boolean flag to enable/disable auth-server chart. You should never set this to false. |
| auth-server.ingress | object | `{"authServerAdditionalAnnotations":{},"authServerEnabled":true,"authServerLabels":{},"authzenAdditionalAnnotations":{},"authzenConfigEnabled":true,"authzenConfigLabels":{},"deviceCodeAdditionalAnnotations":{},"deviceCodeEnabled":true,"deviceCodeLabels":{},"firebaseMessagingAdditionalAnnotations":{},"firebaseMessagingEnabled":true,"firebaseMessagingLabels":{},"lockAdditionalAnnotations":{},"lockConfigAdditionalAnnotations":{},"lockConfigEnabled":false,"lockConfigLabels":{},"lockEnabled":false,"lockLabels":{},"openidAdditionalAnnotations":{},"openidConfigEnabled":true,"openidConfigLabels":{},"u2fAdditionalAnnotations":{},"u2fConfigEnabled":true,"u2fConfigLabels":{},"uma2AdditionalAnnotations":{},"uma2ConfigEnabled":true,"uma2ConfigLabels":{},"webdiscoveryAdditionalAnnotations":{},"webdiscoveryEnabled":true,"webdiscoveryLabels":{},"webfingerAdditionalAnnotations":{},"webfingerEnabled":true,"webfingerLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| auth-server.ingress.authServerAdditionalAnnotations | object | `{}` | Auth server ingress resource additional annotations. |
| auth-server.ingress.authServerEnabled | bool | `true` | Enable Auth server endpoints /jans-auth |
| auth-server.ingress.authServerLabels | object | `{}` | Auth server ingress resource labels. key app is taken |
| auth-server.ingress.authzenAdditionalAnnotations | object | `{}` | authzen config ingress resource additional annotations. |
| auth-server.ingress.authzenConfigEnabled | bool | `true` | Enable endpoint /.well-known/authzen-configuration |
| auth-server.ingress.authzenConfigLabels | object | `{}` | authzen config ingress resource labels. key app is taken |
| auth-server.ingress.deviceCodeAdditionalAnnotations | object | `{}` | device-code ingress resource additional annotations. |
| auth-server.ingress.deviceCodeEnabled | bool | `true` | Enable endpoint /device-code |
| auth-server.ingress.deviceCodeLabels | object | `{}` | device-code ingress resource labels. key app is taken |
| auth-server.ingress.firebaseMessagingAdditionalAnnotations | object | `{}` | Firebase Messaging ingress resource additional annotations. |
| auth-server.ingress.firebaseMessagingEnabled | bool | `true` | Enable endpoint /firebase-messaging-sw.js |
| auth-server.ingress.firebaseMessagingLabels | object | `{}` | Firebase Messaging ingress resource labels. key app is taken |
| auth-server.ingress.lockAdditionalAnnotations | object | `{}` | Lock ingress resource additional annotations. |
| auth-server.ingress.lockConfigAdditionalAnnotations | object | `{}` | Lock config ingress resource additional annotations. |
| auth-server.ingress.lockConfigEnabled | bool | `false` | Enable endpoint /.well-known/lock-server-configuration |
| auth-server.ingress.lockConfigLabels | object | `{}` | Lock config ingress resource labels. key app is taken |
| auth-server.ingress.lockEnabled | bool | `false` | Enable endpoint /jans-lock |
| auth-server.ingress.lockLabels | object | `{}` | Lock ingress resource labels. key app is taken |
| auth-server.ingress.openidAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| auth-server.ingress.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| auth-server.ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| auth-server.ingress.u2fAdditionalAnnotations | object | `{}` | u2f config ingress resource additional annotations. |
| auth-server.ingress.u2fConfigEnabled | bool | `true` | Enable endpoint /.well-known/fido-configuration |
| auth-server.ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| auth-server.ingress.uma2AdditionalAnnotations | object | `{}` | uma2 config ingress resource additional annotations. |
| auth-server.ingress.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| auth-server.ingress.uma2ConfigLabels | object | `{}` | uma2 config ingress resource labels. key app is taken |
| auth-server.ingress.webdiscoveryAdditionalAnnotations | object | `{}` | webdiscovery ingress resource additional annotations. |
| auth-server.ingress.webdiscoveryEnabled | bool | `true` | Enable endpoint /.well-known/simple-web-discovery |
| auth-server.ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| auth-server.ingress.webfingerAdditionalAnnotations | object | `{}` | webfinger ingress resource additional annotations. |
| auth-server.ingress.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
| auth-server.ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| auth-server.lockEnabled | bool | `false` | Enable jans-lock as service running inside auth-server |
| casa.appLoggers | object | `{"casaLogLevel":"INFO","casaLogTarget":"STDOUT","enableStdoutLogPrefix":"true","timerLogLevel":"INFO","timerLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| casa.appLoggers.casaLogLevel | string | `"INFO"` | casa.log level |
| casa.appLoggers.casaLogTarget | string | `"STDOUT"` | casa.log target |
| casa.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e casa ===> 2022-12-20 17:49:55,744 INFO |
| casa.appLoggers.timerLogLevel | string | `"INFO"` | casa timer log level |
| casa.appLoggers.timerLogTarget | string | `"FILE"` | casa timer log target |
| casa.casaServiceName | string | `"casa"` | Name of the casa service. Please keep it as default. |
| casa.cnCustomJavaOptions | string | `""` | passing custom java options to casa. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| casa.enabled | bool | `true` | Boolean flag to enable/disable the casa chart. |
| casa.ingress | object | `{"casaAdditionalAnnotations":{},"casaEnabled":false,"casaLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| casa.ingress.casaAdditionalAnnotations | object | `{}` | Casa ingress resource additional annotations. |
| casa.ingress.casaEnabled | bool | `false` | Enable casa endpoints /casa |
| casa.ingress.casaLabels | object | `{}` | Casa ingress resource labels. key app is taken |
| certManager.certificate.enabled | bool | `false` |  |
| certManager.certificate.issuerGroup | string | `"cert-manager.io"` |  |
| certManager.certificate.issuerKind | string | `"ClusterIssuer"` |  |
| certManager.certificate.issuerName | string | `""` |  |
| certManager.certificate.tlsSecretName | string | `"tls-certificate"` |  |
| city | string | `"Austin"` | City. Used for certificate creation. |
| cleanup | object | `{"additionalAnnotations":{},"additionalLabels":{},"customCommand":[],"customScripts":[],"dnsConfig":{},"dnsPolicy":"","enabled":true,"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/cloudtools","tag":"2.0.0-1"},"interval":60,"lifecycle":{},"limit":1000,"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Cleanup expired entries in persistence |
| cleanup.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| cleanup.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| cleanup.customCommand | list | `[]` | Add custom job's command. If passed, it will override the default conditional command. |
| cleanup.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| cleanup.dnsConfig | object | `{}` | Add custom dns config |
| cleanup.dnsPolicy | string | `""` | Add custom dns policy |
| cleanup.enabled | bool | `true` | Boolean flag to enable/disable the cleanup cronjob chart. |
| cleanup.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| cleanup.image.pullSecrets | list | `[]` | Image Pull Secrets |
| cleanup.image.repository | string | `"ghcr.io/janssenproject/jans/cloudtools"` | Image  to use for deploying. |
| cleanup.image.tag | string | `"2.0.0-1"` | Image  tag to use for deploying. |
| cleanup.interval | int | `60` | Interval of running the cleanup process (in minutes) |
| cleanup.limit | int | `1000` | Max. numbers of entries to cleanup |
| cleanup.resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| cleanup.resources.limits.cpu | string | `"300m"` | CPU limit. |
| cleanup.resources.limits.memory | string | `"300Mi"` | Memory limit. |
| cleanup.resources.requests.cpu | string | `"300m"` | CPU request. |
| cleanup.resources.requests.memory | string | `"300Mi"` | Memory request. |
| cleanup.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| cleanup.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| cleanup.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| cleanup.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| cleanup.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| cnAwsConfigFile | string | `"/etc/jans/conf/aws_config_file"` |  |
| cnAwsSecretsReplicaRegionsFile | string | `"/etc/jans/conf/aws_secrets_replica_regions"` |  |
| cnAwsSharedCredentialsFile | string | `"/etc/jans/conf/aws_shared_credential_file"` |  |
| cnConfiguratorConfigurationFile | string | `"/etc/jans/conf/configuration.json"` | Path to configuration schema file |
| cnConfiguratorCustomSchema | object | `{"secretName":""}` | Use custom configuration schema in existing secrets. Note, the secrets has to contain the key configuration.json or any basename as specified in cnConfiguratorConfigurationFile. |
| cnConfiguratorCustomSchema.secretName | string | `""` | The name of the secrets used for storing custom configuration schema. |
| cnConfiguratorDumpFile | string | `"/etc/jans/conf/configuration.out.json"` | Path to dumped configuration schema file |
| cnConfiguratorKey | string | `""` | Key to encrypt/decrypt configuration schema file using AES-256 CBC mode. Set the value to empty string to disable encryption/decryption, or 32 alphanumeric characters to enable it. |
| cnConfiguratorKeyFile | string | `"/etc/jans/conf/configuration.key"` | Path to file contains key to encrypt/decrypt configuration schema file. |
| cnDocumentStoreType | string | `"DB"` | Document store type to use for shibboleth files DB. |
| cnGoogleApplicationCredentials | string | `"/etc/jans/conf/google-credentials.json"` | Base64 encoded service account. The sa must have roles/secretmanager.admin to use Google secrets. Leave as this is a sensible default. |
| cnPersistenceType | string | `"sql"` | Persistence backend to run Janssen with hybrid|sql. |
| cnPrometheusPort | string | `""` | Port used by Prometheus JMX agent (default to empty string). To enable Prometheus JMX agent, set the value to a number. |
| cnSqlPasswordFile | string | `"/etc/jans/conf/sql_password"` | Path to SQL password file |
| config-api.appLoggers | object | `{"configApiLogLevel":"INFO","configApiLogTarget":"STDOUT","enableStdoutLogPrefix":"true","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| config-api.appLoggers.configApiLogLevel | string | `"INFO"` | configapi.log level |
| config-api.appLoggers.configApiLogTarget | string | `"STDOUT"` | configapi.log target |
| config-api.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e config-api_persistence ===> 2022-12-20 17:49:55,744 INFO |
| config-api.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | config-api_persistence_duration.log level |
| config-api.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | config-api_persistence_duration.log target |
| config-api.appLoggers.persistenceLogLevel | string | `"INFO"` | config-api_persistence.log level |
| config-api.appLoggers.persistenceLogTarget | string | `"FILE"` | config-api_persistence.log target |
| config-api.appLoggers.scriptLogLevel | string | `"INFO"` | config-api_script.log level |
| config-api.appLoggers.scriptLogTarget | string | `"FILE"` | config-api_script.log target |
| config-api.cnCustomJavaOptions | string | `""` | passing custom java options to config-api. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| config-api.configApiServerServiceName | string | `"config-api"` | Name of the config-api service. Please keep it as default. |
| config-api.enabled | bool | `true` | Boolean flag to enable/disable the config-api chart. |
| config-api.ingress | object | `{"configApiAdditionalAnnotations":{},"configApiEnabled":true,"configApiLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| config-api.ingress.configApiAdditionalAnnotations | object | `{}` | ConfigAPI ingress resource additional annotations. |
| config-api.ingress.configApiLabels | object | `{}` | configAPI ingress resource labels. key app is taken |
| config-api.plugins | string | `"admin-ui,fido2,scim,user-mgt"` | Comma-separated values of enabled plugins (supported plugins are "admin-ui","fido2","scim","user-mgt","jans-link","kc-saml") |
| config.enabled | bool | `true` | Boolean flag to enable/disable the configuration job. This normally should never be false |
| configAdapterName | string | `"kubernetes"` | The config backend adapter that will hold Janssen configuration layer. aws|google|kubernetes |
| configSecretAdapter | string | `"kubernetes"` | The config backend adapter that will hold Janssen secret layer. vault|aws|google|kubernetes |
| configmap.cnAwsAccessKeyId | string | `""` |  |
| configmap.cnAwsDefaultRegion | string | `"us-west-1"` |  |
| configmap.cnAwsProfile | string | `"janssen"` |  |
| configmap.cnAwsSecretAccessKey | string | `""` |  |
| configmap.cnAwsSecretsEndpointUrl | string | `""` |  |
| configmap.cnAwsSecretsNamePrefix | string | `"janssen"` |  |
| configmap.cnAwsSecretsReplicaRegions | list | `[]` |  |
| configmap.cnCacheType | string | `"NATIVE_PERSISTENCE"` | Cache type. `NATIVE_PERSISTENCE`, `REDIS`. or `IN_MEMORY`. Defaults to `NATIVE_PERSISTENCE` . |
| configmap.cnConfigKubernetesConfigMap | string | `"cn"` | The name of the Kubernetes ConfigMap that will hold the configuration layer |
| configmap.cnGoogleProjectId | string | `"google-project-to-save-config-and-secrets-to"` | Project id of the Google project the secret manager belongs to. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSecretManagerServiceAccount | string | `"SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo="` | Service account with roles roles/secretmanager.admin base64 encoded string. This is used often inside the services to reach the configuration layer. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSecretNamePrefix | string | `"janssen"` | Prefix for Janssen secret in Google Secret Manager. Defaults to janssen. If left janssen-secret secret will be created. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnGoogleSecretVersionId | string | `"latest"` | Secret version to be used for secret configuration. Defaults to latest and should normally always stay that way. Used only when configAdapterName and configSecretAdapter is set to google. |
| configmap.cnJettyRequestHeaderSize | int | `8192` | Jetty header size in bytes in the auth server |
| configmap.cnMaxRamPercent | string | `"75.0"` | Value passed to Java option -XX:MaxRAMPercentage |
| configmap.cnMessageType | string | `"DISABLED"` | Message type (one of POSTGRES, REDIS, or DISABLED) |
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
| configmap.kcAdminPassword | string | `"Test1234#"` | Keycloak  admin UI password |
| configmap.kcAdminUsername | string | `"admin"` | Keycloak admin UI username |
| configmap.kcDbPassword | string | `"Test1234#"` | Password for Keycloak database access |
| configmap.kcDbSchema | string | `"keycloak"` | Keycloak database schema name (note that PostgreSQL may using "public" schema). |
| configmap.kcDbUrlDatabase | string | `"keycloak"` | Keycloak database name |
| configmap.kcDbUrlHost | string | `"mysql.kc.svc.cluster.local"` | Keycloak database host |
| configmap.kcDbUrlPort | int | `3306` | Keycloak database port (default to port 3306 for mysql). |
| configmap.kcDbUrlProperties | string | `"?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4"` | Keycloak database connection properties. If using postgresql, the value can be set to empty string. |
| configmap.kcDbUsername | string | `"keycloak"` | Keycloak database username |
| configmap.kcDbVendor | string | `"mysql"` | Keycloak database vendor name (default to MySQL server). To use PostgreSQL server, change the value to postgres. |
| configmap.kcLogLevel | string | `"INFO"` | Keycloak logging level |
| configmap.lbAddr | string | `""` | Load balancer address for AWS if the FQDN is not registered. |
| configmap.quarkusTransactionEnableRecovery | bool | `true` | Quarkus transaction recovery. When using MySQL, there could be issue regarding XA_RECOVER_ADMIN; refer to https://dev.mysql.com/doc/refman/8.0/en/privileges-provided.html#priv_xa-recover-admin for details. |
| countryCode | string | `"US"` | Country code. Used for certificate creation. |
| customAnnotations.certificate | object | `{}` |  |
| customAnnotations.clusterRoleBinding | object | `{}` |  |
| customAnnotations.configMap | object | `{}` |  |
| customAnnotations.cronjob | object | `{}` |  |
| customAnnotations.deployment | object | `{}` |  |
| customAnnotations.destinationRule | object | `{}` |  |
| customAnnotations.horizontalPodAutoscaler | object | `{}` |  |
| customAnnotations.pod | object | `{}` |  |
| customAnnotations.podDisruptionBudget | object | `{}` |  |
| customAnnotations.role | object | `{}` |  |
| customAnnotations.roleBinding | object | `{}` |  |
| customAnnotations.secret | object | `{}` |  |
| customCommand | list | `[]` | Add custom pod's command. If passed, it will override the default conditional command. |
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
| fido2.cnCustomJavaOptions | string | `""` | passing custom java options to fido2. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| fido2.enabled | bool | `true` | Boolean flag to enable/disable the fido2 chart. |
| fido2.fido2ServiceName | string | `"fido2"` | Name of the fido2 service. Please keep it as default. |
| fido2.ingress | object | `{"fido2AdditionalAnnotations":{},"fido2ConfigAdditionalAnnotations":{},"fido2ConfigEnabled":false,"fido2ConfigLabels":{},"fido2Enabled":false,"fido2Labels":{},"fido2WebauthnAdditionalAnnotations":{},"fido2WebauthnEnabled":false,"fido2WebauthnLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| fido2.ingress.fido2AdditionalAnnotations | object | `{}` | fido2 ingress resource additional annotations. |
| fido2.ingress.fido2ConfigAdditionalAnnotations | object | `{}` | fido2 config ingress resource additional annotations. |
| fido2.ingress.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| fido2.ingress.fido2ConfigLabels | object | `{}` | fido2 config ingress resource labels. key app is taken |
| fido2.ingress.fido2Enabled | bool | `false` | Enable endpoint /jans-fido2 |
| fido2.ingress.fido2Labels | object | `{}` | fido2 ingress resource labels. key app is taken |
| fido2.ingress.fido2WebauthnAdditionalAnnotations | object | `{}` | fido2 webauthn ingress resource additional annotations. |
| fido2.ingress.fido2WebauthnEnabled | bool | `false` | Enable endpoint /.well-known/webauthn |
| fido2.ingress.fido2WebauthnLabels | object | `{}` | fido2 webauthn ingress resource labels. key app is taken |
| fqdn | string | `"demoexample.jans.io"` | Fully qualified domain name to be used for Janssen installation. This address will be used to reach Janssen services. |
| fullNameOverride | string | `""` |  |
| hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| hpa.behavior | object | `{}` | Scaling Policies |
| hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| image.pullSecrets | list | `[]` | Image Pull Secrets |
| image.repository | string | `"ghcr.io/janssenproject/jans/all-in-one"` | Image  to use for deploying. |
| image.tag | string | `"2.0.0-1"` | Image  tag to use for deploying. |
| isFqdnRegistered | bool | `false` | Boolean flag to enable mapping lbIp  to fqdn inside pods on clouds that provide static ip for load balancers. On cloud that provide only addresses to the LB this flag will enable a script to actively scan config.configmap.lbAddr and update the hosts file inside the pods automatically. |
| istio.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| istio.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| istio.enabled | bool | `false` | Boolean flag that enables using istio side-cars with Janssen services. |
| istio.gateways | list | `[]` | Override the gateway that can be created by default. This is used when istio ingress has already been setup and the gateway exists. |
| istio.ingress | bool | `false` | Boolean flag that enables using istio gateway for Janssen. This assumes istio ingress is installed and hence the LB is available. |
| istio.namespace | string | `"istio-system"` | The namespace istio is deployed in. The is normally istio-system. |
| istio.tlsSecretName | string | `"tls-certificate"` |  |
| kc-scheduler | object | `{"additionalAnnotations":{},"additionalLabels":{},"customCommand":[],"customScripts":[],"dnsConfig":{},"dnsPolicy":"","enabled":false,"image":{"pullPolicy":"IfNotPresent","pullSecrets":[],"repository":"ghcr.io/janssenproject/jans/cloudtools","tag":"2.0.0-1"},"interval":10,"lifecycle":{},"resources":{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}},"usrEnvs":{"normal":{},"secret":{}},"volumeMounts":[],"volumes":[]}` | Responsible for synchronizing Keycloak SAML clients |
| kc-scheduler.additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| kc-scheduler.additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| kc-scheduler.customCommand | list | `[]` | Add custom job's command. If passed, it will override the default conditional command. |
| kc-scheduler.customScripts | list | `[]` | Add custom scripts that have been mounted to run before the entrypoint. - /tmp/custom.sh - /tmp/custom2.sh |
| kc-scheduler.dnsConfig | object | `{}` | Add custom dns config |
| kc-scheduler.dnsPolicy | string | `""` | Add custom dns policy |
| kc-scheduler.enabled | bool | `false` | Boolean flag to enable/disable the kc-scheduler cronjob chart. |
| kc-scheduler.image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| kc-scheduler.image.pullSecrets | list | `[]` | Image Pull Secrets |
| kc-scheduler.image.repository | string | `"ghcr.io/janssenproject/jans/cloudtools"` | Image  to use for deploying. |
| kc-scheduler.image.tag | string | `"2.0.0-1"` | Image  tag to use for deploying. |
| kc-scheduler.interval | int | `10` | Interval of running the scheduler (in minutes) |
| kc-scheduler.resources | object | `{"limits":{"cpu":"300m","memory":"300Mi"},"requests":{"cpu":"300m","memory":"300Mi"}}` | Resource specs. |
| kc-scheduler.resources.limits.cpu | string | `"300m"` | CPU limit. |
| kc-scheduler.resources.limits.memory | string | `"300Mi"` | Memory limit. |
| kc-scheduler.resources.requests.cpu | string | `"300m"` | CPU request. |
| kc-scheduler.resources.requests.memory | string | `"300Mi"` | Memory request. |
| kc-scheduler.usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| kc-scheduler.usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| kc-scheduler.usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| kc-scheduler.volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| kc-scheduler.volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
| lbIp | string | `"22.22.22.22"` | The Loadbalancer IP created by nginx or istio on clouds that provide static IPs. This is not needed if `fqdn` is globally resolvable. |
| lifecycle | object | `{}` |  |
| link.appLoggers | object | `{"enableStdoutLogPrefix":"true","linkLogLevel":"INFO","linkLogTarget":"STDOUT","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| link.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e link-persistence ===> 2022-12-20 17:49:55,744 INFO |
| link.appLoggers.linkLogLevel | string | `"INFO"` | cacherefresh.log level |
| link.appLoggers.linkLogTarget | string | `"STDOUT"` | cacherefresh.log target |
| link.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | cacherefresh_persistence_duration.log level |
| link.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | cacherefresh_persistence_duration.log target |
| link.appLoggers.persistenceLogLevel | string | `"INFO"` | cacherefresh_persistence.log level |
| link.appLoggers.persistenceLogTarget | string | `"FILE"` | cacherefresh_persistence.log target |
| link.appLoggers.scriptLogLevel | string | `"INFO"` | cacherefresh_script.log level |
| link.appLoggers.scriptLogTarget | string | `"FILE"` | cacherefresh_script.log target |
| link.cnCustomJavaOptions | string | `""` | passing custom java options to link. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| link.enabled | bool | `false` | Boolean flag to enable/disable the link chart. |
| link.ingress | object | `{"linkAdditionalAnnotations":{},"linkEnabled":true,"linkLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| link.ingress.linkAdditionalAnnotations | object | `{}` | link ingress resource additional annotations. |
| link.ingress.linkLabels | object | `{}` | link ingress resource labels. key app is taken |
| link.linkServiceName | string | `"link"` | Name of the link service. Please keep it as default. |
| livenessProbe | object | `{"exec":{"command":["python3","/app/jans_aio/jans_auth/healthcheck.py"]},"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for the auth server if needed. |
| livenessProbe.exec | object | `{"command":["python3","/app/jans_aio/jans_auth/healthcheck.py"]}` | Executes the python3 healthcheck. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| nameOverride | string | `""` |  |
| nginx-ingress.enabled | bool | `true` | Boolean flag to enable/disable the nginx-ingress definitions chart. |
| nginx-ingress.ingress.additionalAnnotations | object | `{}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"} Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "janssen/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| nginx-ingress.ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| nginx-ingress.ingress.ingressClassName | string | `"nginx"` |  |
| nginx-ingress.ingress.path | string | `"/"` |  |
| nginx-ingress.ingress.tlsSecretName | string | `"tls-certificate"` | Secrets holding HTTPS CA cert and key. |
| orgName | string | `"Janssen"` | Organization name. Used for certificate creation. |
| pdb | object | `{"enabled":true,"maxUnavailable":"90%"}` | Configure the PodDisruptionBudget |
| persistence.enabled | bool | `true` | Boolean flag to enable/disable the persistence job. |
| readinessProbe | object | `{"exec":{"command":["python3","/app/jans_aio/jans_auth/healthcheck.py"]},"initialDelaySeconds":25,"periodSeconds":25,"timeoutSeconds":5}` | Configure the readiness healthcheck for the auth server if needed. https://github.com/JanssenProject/docker-jans-auth-server/blob/master/scripts/healthcheck.py |
| redisPassword | string | `"P@assw0rd"` | Redis admin password if `configmap.cnCacheType` is set to `REDIS`. |
| replicas | int | `1` | Service replica number. |
| resources | object | `{"limits":{"cpu":"16000m","memory":"16000Mi"},"requests":{"cpu":"2500m","memory":"2500Mi"}}` | Resource specs. |
| resources.limits.cpu | string | `"16000m"` | CPU limit. |
| resources.limits.memory | string | `"16000Mi"` | Memory limit. |
| resources.requests.cpu | string | `"2500m"` | CPU request. |
| resources.requests.memory | string | `"2500Mi"` | Memory request. |
| salt | string | `""` | Salt. Used for encoding/decoding sensitive data. If omitted or set to empty string, the value will be self-generated. Otherwise, a 24 alphanumeric characters are allowed as its value. |
| saml.cnCustomJavaOptions | string | `""` | passing custom java options to saml. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| saml.enabled | bool | `false` | Boolean flag to enable/disable the saml chart. |
| saml.ingress | object | `{"samlAdditionalAnnotations":{},"samlEnabled":false,"samlLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| saml.ingress.samlAdditionalAnnotations | object | `{}` | SAML ingress resource additional annotations. |
| saml.ingress.samlLabels | object | `{}` | SAML config ingress resource labels. key app is taken |
| saml.samlServiceName | string | `"saml"` | Name of the saml service. Please keep it as default. |
| scim.appLoggers | object | `{"enableStdoutLogPrefix":"true","persistenceDurationLogLevel":"INFO","persistenceDurationLogTarget":"FILE","persistenceLogLevel":"INFO","persistenceLogTarget":"FILE","scimLogLevel":"INFO","scimLogTarget":"STDOUT","scriptLogLevel":"INFO","scriptLogTarget":"FILE"}` | App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed. |
| scim.appLoggers.enableStdoutLogPrefix | string | `"true"` | Enable log prefixing which enables prepending the STDOUT logs with the file name. i.e jans-scim ===> 2022-12-20 17:49:55,744 INFO |
| scim.appLoggers.persistenceDurationLogLevel | string | `"INFO"` | jans-scim_persistence_duration.log level |
| scim.appLoggers.persistenceDurationLogTarget | string | `"FILE"` | jans-scim_persistence_duration.log target |
| scim.appLoggers.persistenceLogLevel | string | `"INFO"` | jans-scim_persistence.log level |
| scim.appLoggers.persistenceLogTarget | string | `"FILE"` | jans-scim_persistence.log target |
| scim.appLoggers.scimLogLevel | string | `"INFO"` | jans-scim.log level |
| scim.appLoggers.scimLogTarget | string | `"STDOUT"` | jans-scim.log target |
| scim.appLoggers.scriptLogLevel | string | `"INFO"` | jans-scim_script.log level |
| scim.appLoggers.scriptLogTarget | string | `"FILE"` | jans-scim_script.log target |
| scim.cnCustomJavaOptions | string | `""` | passing custom java options to scim. Notice you do not need to pass in any loggers options as they are introduced below in appLoggers. DO NOT PASS JAVA_OPTIONS in envs. |
| scim.enabled | bool | `true` | Boolean flag to enable/disable the SCIM chart. |
| scim.ingress | object | `{"scimAdditionalAnnotations":{},"scimConfigAdditionalAnnotations":{},"scimConfigEnabled":false,"scimConfigLabels":{},"scimEnabled":false,"scimLabels":{}}` | Enable endpoints in either istio or nginx ingress depending on users choice |
| scim.ingress.scimAdditionalAnnotations | object | `{}` | SCIM ingress resource additional annotations. |
| scim.ingress.scimConfigAdditionalAnnotations | object | `{}` | SCIM config ingress resource additional annotations. |
| scim.ingress.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| scim.ingress.scimConfigLabels | object | `{}` | SCIM config ingress resource labels. key app is taken |
| scim.ingress.scimEnabled | bool | `false` | Enable SCIM endpoints /jans-scim |
| scim.ingress.scimLabels | object | `{}` | SCIM ingress resource labels. key app is taken |
| scim.scimServiceName | string | `"scim"` | Name of the scim service. Please keep it as default. |
| service.name | string | `"http-aio"` | The name of the aio port within the aio service. Please keep it as default. |
| service.port | int | `8080` | Port of the fido2 service. Please keep it as default. |
| service.sessionAffinity | string | `"None"` | Default set to None If you want to make sure that connections from a particular client are passed to the same Pod each time, you can select the session affinity based on the client's IP addresses by setting this to ClientIP |
| service.sessionAffinityConfig | object | `{"clientIP":{"timeoutSeconds":10800}}` | the maximum session sticky time if sessionAffinity is ClientIP |
| serviceAccountName | string | `"default"` | service account used by Kubernetes resources |
| state | string | `"TX"` | State code. Used for certificate creation. |
| testEnviroment | bool | `false` | Boolean flag if enabled will strip resources requests and limits from all services. |
| tolerations | list | `[]` | Add tolerations for the pods |
| topologySpreadConstraints | object | `{}` | Configure the topology spread constraints. Notice this is a map NOT a list as in the upstream API https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/ |
| usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |
