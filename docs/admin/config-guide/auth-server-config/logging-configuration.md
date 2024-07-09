---
tags:
  - administration
  - configuration
  - logging
---

# Log Management

Logging for Janssen Server modules is managed at the individual module level.
Meaning there is no single switch or configuration that will enable or disable
logs for all modules. Each module can be individually configured and can be
configured differently when it comes to logging.

Logging for `jans-auth` service is what we are going to discuss in detail 
in this document.
Logging for other modules is configured by changing the module's 
property values. Use the documentation on how to update configuration 
properties for 
corresponding modules to update logging related properties. For instance:

[//]: # (TODO: not sure if the sections below exist in respective docs. But)
[//]: # (if not then we need to add this content and link it here)
- [Updating properties for Fido2 module]()
- [Updating properties for SCIM module]()

## Log Configuration For jans-auth

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](./config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](./config-tools/jans-tui/README.md) or jump straight to the
    [Using-text-based-ui](#using-text-based-ui)


=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](./config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

##  Using Command Line

In the Janssen Server, you can deploy and customize the Logging Configuration using the
command line. To get the details of Janssen command line operations relevant to
Logging configuration, you can check the operations under 
`ConfigurationLogging` task using the
command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info ConfigurationLogging
```

```text title="Sample Output"
Operation ID: get-config-logging
  Description: Returns Jans Authorization Server logging settings
Operation ID: put-config-logging
  Description: Updates Jans Authorization Server logging settings
  Schema: Logging

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schema>, for example /opt/jans/jans-cli/config-cli.py --schema Logging
```

### Find Logging Configuration

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-config-logging
```

```json title="Sample Output" linenums="1"
{
  "loggingLevel": "DEBUG",
  "loggingLayout": "string",
  "httpLoggingEnabled": true,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": true,
  "externalLoggerConfiguration": "string",
  "httpLoggingExcludePaths": [
    "string"
  ]
}

```


### Update Logging Configuration

To update logging configuration, get the schema first:
```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema Logging > /tmp/log-config.json
```
The schema can now be found in the log-config.json file.

For your information, you can obtain the format of the `Logging`
schema by running the aforementioned command without a file.

```text title="Schema Format"
loggingLevel                  string
loggingLayout                 string
httpLoggingEnabled            boolean
disableJdkLogger              boolean
enabledOAuthAuditLogging      boolean
externalLoggerConfiguration   string
httpLoggingExcludePaths       array of string
                              uniqueItems: True
```

you can also use the following command for `Logging` schema example.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample Logging
```
```text title="Schema Example"
{
  "loggingLevel": "string",
  "loggingLayout": "string",
  "httpLoggingEnabled": true,
  "disableJdkLogger": false,
  "enabledOAuthAuditLogging": true,
  "externalLoggerConfiguration": "string",
  "httpLoggingExcludePaths": [
    "string"
  ]
}
```

let's update the schema:
```bash title="Command"
nano /tmp/log-config.json
```

As seen below, I have added `loggingLevel` for the value `INFO` 
and `enabledOAuditLogging` for the value `false`.

```json title="Input"
{
  "loggingLevel": "INFO",
  "loggingLayout": "string",
  "httpLoggingEnabled": true,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": false,
  "externalLoggerConfiguration": "string",
  "httpLoggingExcludePaths": [
    "string"
  ]
}
```

Let's do the operation:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id put-config-logging \
 --data /tmp/log-config.json
```

You will get the updated result as below:

```json  title="Sample Output" linenums="1"
{
  "loggingLevel": "INFO",
  "loggingLayout": "string",
  "httpLoggingEnabled": true,
  "disableJdkLogger": true,
  "enabledOAuthAuditLogging": false,
  "externalLoggerConfiguration": "string",
  "httpLoggingExcludePaths": [
    "string"
  ]
}
```

## Using-text-based-ui

In Janssen, You can manage Logging configuration using
the [Text-Based UI](./config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```
### Logging Screen

* Navigate to `Auth Server` -> `Logging` to open the Logging screen as shown
in the image below.

* Logging screen allows the administrator to set logging parameters as per
the requirements.


![image](../../assets/tui-logging-config.png)

## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring logging. Endpoint details are published in the [Swagger
document](./../reference/openapi.md).

## Default Log Location

On a VM installation, logs for `jans-auth` module are generated at
`/opt/jans/jetty/jans-auth/logs/`.

## Cloud-Native Deployments

Logging configuration for a cloud-native deployment is [detailed here](../kubernetes-ops/logs.md)