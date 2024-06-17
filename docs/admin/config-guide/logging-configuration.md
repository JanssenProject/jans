---
tags:
  - administration
  - configuration
  - logging
---

# Log Management

The Janssen Server provides multiple configuration tools to perform these
tasks.

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
Logging configuration, you can check the operations under `ConfigurationLogging` task using the
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
Check the [update logging schema model](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-config-api/docs/jans-config-api-swagger.yaml#/Configuration%20%E2%80%93%20Logging/put-config-logging)

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

In Janssen, You can manage Logging configuration method using
the [Text-Based UI](./config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```
### Logging Screen

* Navigate to `Auth Server` -> `Logging` to open the Logging screen as shown
in the image below.

* Various fields are accessible on this page, where users can input
accurate data corresponding to each field.

* Once all valid information has been inputted, the user has the option
to save the logging configuration.

![image](../../assets/tui-logging-config.png)

## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring logging. Endpoint details are published in the [Swagger
document](./../reference/openapi.md).
