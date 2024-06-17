---
tags:
  - administration
  - scim
  - configuration
---

# SCIM configuration

Relevant configuration properties of the Jans SCIM server are summarized in the table below:

|Property|Default value|Description|
|-|-|-|
|maxCount|200|Maximum number of results per page in search endpoints|
|bulkMaxOperations|30|Maximum number of operations admitted in a single bulk request|
|bulkMaxPayloadSize|3072000|Maximum payload size in bytes admitted in a single bulk request|
|userExtensionSchemaURI|`urn:ietf:params:scim:schemas:extension:gluu:2.0:User`|URI schema associated to the User Extension|
|skipDefinedPasswordValidation|false|Whether the validation rules defined for the password attribute in the server should be bypassed when a user is created/updated|
|loggingLevel|`INFO`|The logging [level](./logs.md)|

## Configuration management using CLI

To retrieve the current server configuration run the command `python3 /opt/jans/jans-cli/config-cli.py --operation-id get-scim-config`

To modify some aspect of the retrieved configuration prepare a PATCH request in JSON format. For instance:

```
[
{ 
  "op":"replace",
  "path": "bulkMaxOperations",
  "value": 100
},
{ 
  "op":"replace",
  "path": "loggingLevel",
  "value": "DEBUG"
}
]

```

These contents should be then passed to the `patch-scim-config` operation, e.g. `python3 /opt/jans/jans-cli/config-cli.py --operation-id patch-scim-config --data <path-to-JSON-file>`.

## Configuration management using TUI

To retrieve the current server configuration using TUI proceed as below:

1. Launch TUI, e.g. by running `python3 /opt/jans/jans-cli/jans_cli_tui.py`, and follow the prompts
1. Highlight the SCIM tab using your keyboard's left/right arrow key
1. Highlight the "Get Scim Configuration" button using the tab key
1. Press enter

You can modify the configuration in place by editing the fields of your interest. To persist the changes, highlight the "Save" button at the bottom and press enter 

## When will changes take effect?

Any configuration update will take effect one minute after it has been applied whether via CLI or TUI. 
