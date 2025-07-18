---
tags:
  - administration
  - vm
  - operations
  - log-levels
---

# Janssen Log Configuration

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

- To get the logging configuration operation ID: `jans cli --info ConfigurationLogging`
  - Output would be: 
    ```
    # Log configuration operations
    Operation ID: get-config-logging
    Description: Returns Jans Authorization Server logging settings.
    Operation ID: put-config-logging
    Description: Updates Jans Authorization Server logging settings.
    Schema: /components/schemas/LoggingConfiguration
    ```
- To get sample schema type `jans cli --schema <schma>`, 
  for example `jans cli --schema /components/schemas/LoggingConfiguration`
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
- Status of current configuration logging: `jans cli --operation-id get-config-logging`
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
jans cli --operation-id put-config-logging --data /tmp/log.json
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
| **configapi_persistence.log** | Config API persistence log |
| **configapi_persistence_duration.log** | Config API persistence operation duration log |
| **[date].jetty.log** | Config API Jetty log |
| **configapi_script.log** | Config API custom script log |

### Jans Auth server logs
`/opt/jans/jetty/jans-auth/logs/`

The most important log files here are described below:

1. `jans-auth.log`: This log file contains most authentication related information. Generally this is the first log to review for any authentication-related troubleshooting, like authentication failure or missing clients etc.
1. `jans-auth_persistence.log`: This log file contains information about the Jans Auth server communicating with the persistence backend. 
1. `jans-auth_script.log`: This log file contains debug messages printed from [interception scripts](../developer/interception-scripts.md).
### Jans Fido2 server logs
`/opt/jans/jetty/jans-fido2/logs/`
1. `jetty.log`: Logs web server activity and HTTP request handling by the Jetty server.
1. `fido2.log`: Logs FIDO2 authentication and registration events.
1. `fido2_persistence.log`: Logs database operations for storing FIDO2 credentials and metadata.
1. `fido2_persistence_duration.log`:  Logs the time taken for FIDO2 data persistence operations (database-related action when storing & retrieving data).
1. `fido2_script.log`: Logs execution and outcomes of custom scripts used in FIDO2 flows.
we can change logging level of fido server from TUI or CLI as well , check more details [here](../config-guide/fido2-config/janssen-fido2-configuration.md)
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

