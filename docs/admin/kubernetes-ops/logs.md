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

### auth-server
To get the log level of current log level of auth-server:

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

To override the current logging level in auth-server, add these changes to your `override.yaml` file

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

Apply the changes using the following command: 

```bash
helm upgrade janssen janssen/janssen -f override.yaml -n <namespace>
```

View the logs of auth-server:
```bash
kubectl logs <auth-server-pod> -n <namespace>
```


### config-api
To get the log level of current log level of config-api

```bash
kubectl get configmap -n jans  janssen-config-cm -o yaml | grep CN_CONFIG_API_APP_LOGGERS 
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

To override the current logging level in config-api, add these changes to your `override.yaml` file

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

Apply the changes using the following command: 

```bash
helm upgrade janssen janssen/janssen -f override.yaml -n <namespace>
```

View the logs of config-api:
```bash
kubectl logs <config-api-pod> -n jans
```