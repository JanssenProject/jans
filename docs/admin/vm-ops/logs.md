---
tags:
  - administration
  - vm
  - operations
---

## Overview
The Janssen logs can be viewed via SSH access to the server running the Janssen installation.

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
Use the following commands to get information on the logging module configuration:

- To get the logging configuration operation ID: `/opt/jans/jans-cli/config-cli.py --info ConfigurationLogging`
  - Output would be: 
    ```
    # Log configuration operations
    Operation ID: get-config-logging
    Description: Returns Jans Authorization Server logging settings.
    Operation ID: put-config-logging
    Description: Updates Jans Authorization Server logging settings.
    Schema: /components/schemas/LoggingConfiguration
    ```
- To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schma>, 
  for example `/opt/jans/jans-cli/config-cli.py --schema /components/schemas/LoggingConfiguration`
   - Output: 
    ```
    # Generic configuration schema
     {
       "loggingLevel": "TRACE",
       "loggingLayout": "json",
       "httpLoggingEnabled": true,
       "disableJdkLogger": false,
       "enabledOAuthAuditLogging": true,
       "externalLoggerConfiguration": null,
       "httpLoggingExcludePaths": [
         "/auth/img",
         "/auth/stylesheet"
       ]
     }
    ``` 
- Status of current configuration logging: `/opt/jans/jans-cli/config-cli.py --operation-id get-config-logging`
  - Output: 
    ``` 
      # Current log configuration
      {
        "loggingLevel": "INFO",
        "loggingLayout": "text",
        "httpLoggingEnabled": false,
        "disableJdkLogger": true,
        "enabledOAuthAuditLogging": false,
        "externalLoggerConfiguration": null,
        "httpLoggingExcludePaths": null
      }
    ```

Let's assume we want to update logging configuration to `TRACE`. To do this, create a file `/tmp/log.json` with the following content:
```json
{
  "loggingLevel": "TRACE",
  "loggingLayout": "text",
  "httpLoggingEnabled": false,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": false,
  "externalLoggerConfiguration": null,
  "httpLoggingExcludePaths": null
}
```
And use the PUT operation with this file as the payload:
```
/opt/jans/jans-cli/config-cli.py --operation-id put-config-logging --data /tmp/log.json
```
The server will now have logs set to TRACE.

## Setup Logs
The Jans setup logs are available under `/opt/jans/jans-setup/logs/`. There are several log files available involving the setup process:

1. mysql.log (Only used if MySQL backend is chosen during setup)
1. os-changes.log
1. setup.log
1. setup_error.log

## Core Logs
The available logs for Jans server are listed below:

### Config API logs
`/opt/jans/jetty/jans-config-api/logs/`

| Log File | Description |  
|--------- |-------------|
| **configapi.log** | Config API main log |
| **configapi_persistence.log** | Config API LDAP log |
| **configapi_persistence_duration.log** | Config API LDAP operation duration log |
| **configapi_persistence_ldap_statistics.log**| Config API LDAP statistics |
| **[date].jetty.log** | Config API Jetty log |
| **configapi_script.log** | Config API custom script log |

### Jans Auth server logs
`/opt/jans/jetty/jans-auth/logs/`

The most important log files here are described below:

1. `jans-auth.log`: This log file contains most authentication related information. Generally this is the first log to review for any authentication-related troubleshooting, like authentication failure or missing clients etc.
1. `jans-auth_persistence.log`: This log file contains information about the Jans Auth server communicating with the persistence backend. 
1. `jans-auth_script.log`: This log file contains debug messages printed from [interception scripts](../developer/interception-scripts.md).

## Server Logs
In some cases, it may be necessary to examine the server logs themselves.

### OS Logs
- For Debian based systems: `/var/log/syslog`
- For RPM based systems: `/var/log/messages`

### Apache2 Server Logs
- For Debian based systems: `/var/log/apache2/`
- For RPM based systems: `/var/log/httpd/`

Apache2 logs are as follows:

1. `access_log`: This log contains information about requests coming into the Jans Server, success status or requests, execution time for any request etc.     

1. `error_log`: This log shows error messages if the web server encounter any issue while processing incoming requests.    

1. `other_vhosts_access.log`: This log is specific to the Jans Server setup and those links which are being requested by a user from a web browser.

