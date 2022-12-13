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
kubectl logs -n jans <pod-name>
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

### Configuring Log Levels
To get the log level of components deployed

```bash
kubectl get configmap -n jans janssen-config-cm -o yaml```
```

Example output of auth-server:
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

To override the default logging level in auth-server, create an override.yaml file

```yaml
auth-server:
  appLoggers:
      authLogTarget: "STDOUT"
      authLogLevel: "TRACE"
      httpLogLevel: "TRACE"
      persistenceLogLevel: "TRACE"
      persistenceDurationLogLevel: "TRACE"
      ldapStatsLogLevel: "TRACE"
      scriptLogLevel: "TRACE"
      auditStatsLogLevel: "TRACE"
```

If want to change the log levels of components that are already deployed, run the following command: 

```bash
kubectl edit configmap -n jans janssen-config-cm
```

