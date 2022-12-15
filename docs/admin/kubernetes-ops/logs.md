---
tags:
  - administration
  - kubernetes
  - operations
  - logs
---

## Overview
The Janssen logs can be viewed using the following command:

```
kubectl logs <pod-name> -n <namespace> 
```

## Log Levels
The following log levels can be configured through the configuration CLI:

| Log Level | Messages Logged |  
|---------- |------------                  |  
|Trace      | All messages                 |  
|Debug      | Debug level and above        |  
|Info       | Informational level and above|  
|Warn       | Warning level and above      |  
|Error      | Error level and above        |  
|Fatal      | Only fatal errors            |  
|Off        | Logging is disabled          |

## Configuring Log Levels
To get the current log level of any component, run the following command:

```bash
kubectl get configmap -n <namespace> <helm-release-name>-config-cm -o yaml | grep CN_<service-name>_APP_LOGGERS
```


### auth-server
To get the current log level of auth-server:

```bash
kubectl get configmap -n <namespace> janssen-config-cm -o yaml | grep CN_AUTH_APP_LOGGERS 
```

Example output:
```yaml
CN_AUTH_APP_LOGGERS: 
'{
"audit_log_level":"INFO",
"audit_log_target":"FILE",
"auth_log_level":"INFO",
"auth_log_target":"STDOUT",
"http_log_level":"INFO",
"http_log_target":"FILE",
"ldap_stats_log_level":"INFO",
"ldap_stats_log_target":"FILE",
"persistence_duration_log_level":"INFO",
"persistence_duration_log_target":"FILE",
"persistence_log_level":"INFO",
"persistence_log_target":"FILE",
"script_log_level":"INFO",
"script_log_target":"FILE"
}'
```

To override the current logging level in auth-server, you can either change it directly using `kubectl edit` command, or add the desired changed to a yaml file and apply it using `helm`: 

- Hack it - Change it directly:

> **Warning**
> This can cause the deployments to break, but if you wish you may edit it directly and restart the wanted deployment


Edit using the following command: 
```bash
kubectl edit configmap <helm-release-name>-config-cm -n <namespace>
```

Restart the wanted deployment:
```bash
kubectl rollout restart deployment <auth-server-deployment> -n <namespace>
```

- Add changes to yaml:

add these changes to your `override.yaml` file:

```yaml
............
............
global:
  auth-server:
    appLoggers:
      authLogLevel: "TRACE"
      httpLogLevel: "TRACE"
      persistenceLogLevel: "TRACE"
      persistenceDurationLogLevel: "TRACE"
      ldapStatsLogLevel: "TRACE"
      scriptLogLevel: "TRACE"
      auditStatsLogLevel: "TRACE"
............
............      
```

Apply the changes: 

```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

View the logs of auth-server:
```bash
kubectl logs <auth-server-pod> -n <namespace>
```


### config-api
To get the current log level of config-api:

```bash
kubectl get configmap -n <namspace> janssen-config-cm -o yaml | grep CN_CONFIG_API_APP_LOGGERS 
```

Example output:
```yaml
CN_CONFIG_API_APP_LOGGERS: 
'{"
config_api_log_level":"INFO",
"config_api_log_target":"STDOUT",
"ldap_stats_log_level":"INFO",
"ldap_stats_log_target":"FILE",
"persistence_duration_log_level":"INFO",
"persistence_duration_log_target":"FILE",
"persistence_log_level":"INFO",
"persistence_log_target":"FILE",
"script_log_level":"INFO",
"script_log_target":"FILE"}'
```

To override the current logging level in config-api, you can either change it directly using `kubectl edit` command or, add the desired changed to a yaml file and apply it using `helm`: 

- Hack it - Change it directly:

> **Warning**
> This can cause the deployments to break, but if you wish you may edit it directly and restart the wanted deployment


Edit using the following command: 
```bash
kubectl edit configmap <helm-release-name>-config-cm -n <namespace>
```

Restart the wanted deployment:
```bash
kubectl rollout restart deployment <auth-server-deployment> -n <namespace>
```

- Add changes to yaml:

add these changes to your `override.yaml` file:

```yaml
............
............
global:
  config-api:
    appLoggers:
      configApiLogLevel: "TRACE"
      persistenceLogLevel: "TRACE"
      persistenceDurationLogLevel: "TRACE"
      ldapStatsLogLevel: "TRACE"
      scriptLogLevel: "TRACE"
............
............      
```

Apply the changes: 

```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

View the logs of config-api:
```bash
kubectl logs <config-api-pod> -n <namespace>
```


### fido2
To get the current log level of fido2:

```bash
kubectl get configmap -n <namespace> janssen-config-cm -o yaml | grep CN_FIDO2_APP_LOGGERS 
```

Example output:
```yaml
CN_FIDO2_APP_LOGGERS: 
'{"
fido2_log_level":"INFO",
"fido2_log_target":"STDOUT",
"persistence_log_level":"INFO",
"persistence_log_target":"FILE"}'
```

To override the current logging level in fido2, you can either change it directly using `kubectl edit` command, or add the desired changed to a yaml file and apply it using `helm`: 

- Hack it - Change it directly:

> **Warning**
> This can cause the deployments to break, but if you wish you may edit it directly and restart the wanted deployment


Edit using the following command: 
```bash
kubectl edit configmap <helm-release-name>-config-cm -n <namespace>
```

Restart the wanted deployment:
```bash
kubectl rollout restart deployment <fido2-deployment> -n <namespace>
```

- Add changes to yaml:

add these changes to your `override.yaml` file:

```yaml
............
............
global:
  fido2:
    appLoggers:
      fido2LogLevel: "TRACE"
      persistenceLogLevel: "TRACE"
............
............      
```

Apply the changes: 

```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

View the logs of fido2:
```bash
kubectl logs <fido2-pod> -n <namespace>
```


### scim
To get the current log level of scim:

```bash
kubectl get configmap -n <namespace> janssen-config-cm -o yaml | grep CN_SCIM_APP_LOGGERS 
```

Example output:
```yaml
CN_SCIM_APP_LOGGERS: 
'{"
ldap_stats_log_level":"INFO",
"ldap_stats_log_target":"FILE",
"persistence_duration_log_level":"INFO",
"persistence_duration_log_target":"FILE",
"persistence_log_level":"INFO",
"persistence_log_target":"FILE",
"scim_log_level":"INFO",
"scim_log_target":"STDOUT",
"script_log_level":"INFO",
"script_log_target":"FILE"}'
```

To override the current logging level in scim, you can either change it directly using `kubectl edit` command, or add the desired changed to a yaml file and apply it using `helm`: 

- Hack it - Change it directly:

> **Warning**
> This can cause the deployments to break, but if you wish you may edit it directly and restart the wanted deployment


Edit using the following command: 
```bash
kubectl edit configmap <helm-release-name>-config-cm -n <namespace>
```

Restart the wanted deployment:
```bash
kubectl rollout restart deployment <scim-deployment> -n <namespace>
```

- Add changes to yaml:

add these changes to your `override.yaml` file:

```yaml
............
............
global:
  scim:
    appLoggers:
      scimLogLevel: "TRACE"
      persistenceLogLevel: "TRACE"
      persistenceDurationLogLevel: "TRACE"
      ldapStatsLogLevel: "TRACE"
      scriptLogLevel: "TRACE"
............
............      
```

Apply the changes: 

```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

View the logs of scim:
```bash
kubectl logs <scim-pod> -n <namespace>
```