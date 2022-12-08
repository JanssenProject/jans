---
tags:
  - administration
  - scim
---

# Installation
The API is available as a component of Janssen Server. Upon [installation](https://docs.jans.io/head/admin/install/)  select  SCIM included in 
environment. To add SCIM **post-install** do the following:

```
python3 /opt/jans/jans-setup/setup.py --install-scim
```

# Configuration Parameters of Janssen's SCIM server
| SCIM named | Example | Description|
|--|--|--|
|baseDN| "o=jans" |
|applicationUrl| https://<your_jans_server> | This is our applicationUrl|
  |baseEndpoint| https://my-jans-server/jans-scim/restv1 | Base URL of the SCIM server Endpoints |
  | personCustomObjectClassList | ["jansCustomPerson", "jansPerson"  ] |LDAP custom object class list for dynamic person enrollment.|
|oxAuthIssuer| https://my-jans-server.jans.io | URL using the https scheme with no query or fragment component. The OP asserts this as its Issuer Identifier|
  |protectionMode| "OAUTH" or "TEST" or "UMA" | This mode is used for API protection from un-authorized access| 
|maxCount| 200 |
  |userExtensionSchemaURI| "urn:ietf:params:scim:schemas:extension:gluu:2.0:User" 
  |loggingLevel | "INFO" or "TRACE" or "DEBUG" | Logging level for SCIM server| 
  |loggingLayout |"text" or "json"|Contents of logs as plain text or json format|
  |externalLoggerConfiguration||Path to external log4j2 logging configuration|
  |metricReporterInterval|300|The interval for metric reporter in seconds.|
  |metricReporterKeepDataDays|15|The number of days to retain metric reported data in the system|
  |metricReporterEnabled| true |Boolean value specifying whether to enable Metric Reporter|
  | disableJdkLogger |true| Boolean value specifying whether to enable JDK Loggers |
| useLocalCache | true| Boolean value specifying whether to enable local in-memory cache for attributes, scopes, clients and organization configuration|
  |bulkMaxOperations| 30 |
  |bulkMaxPayloadSize| 3072000 |
# Configuring the SCIM server
#### 1. Read Configuration parameters
Use the following command to obtain configuration parameters:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-scim-config
```
response
```commandline
{
  "baseDN": "o=jans",
  "applicationUrl": "https://<your_jans_server>",
  "baseEndpoint": "https://<your_jans_server>/jans-scim/restv1",
  "personCustomObjectClass": "jansCustomPerson",
  "oxAuthIssuer": "https://<your_jans_server>",
  "protectionMode": null,
  "maxCount": 200,
  "userExtensionSchemaURI": "urn:ietf:params:scim:schemas:extension:gluu:2.0:User",
  "loggingLevel": "DEBUG",
  "loggingLayout": "text",
  "externalLoggerConfiguration": null,
  "metricReporterInterval": 300,
  "metricReporterKeepDataDays": 15,
  "metricReporterEnabled": true,
  "disableJdkLogger": true,
  "useLocalCache": true,
  "bulkMaxOperations": 30,
  "bulkMaxPayloadSize": 3072000
}
```
#### 2. Update configuration parameters
We can update configuration parameters using **TUI** or **CLI**. Here we using **CLI** for updating configuration parameters.
In **CLI** we use `operation-id` as `patch-scim-config`.


Get patch schema and modify it. It should be an array.
```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > patch-scim.json
```

```commandline
#patch-scim.json
[
  {
    "op": "replace",
    "path": "/protectionMode",
    "value": "OAUTH"
  }
]
```

Now update **SCIM** configuration using below command.

```
/opt/jans/jans-cli/config-cli.py --operation-id patch-scim-config --data pathc-scim.json
```
response
```commandline
{
  "baseDN": "o=jans",
  "applicationUrl": "https://test.jans.io",
  "baseEndpoint": "https://test.jans.io/jans-scim/restv1",
  "personCustomObjectClass": "jansCustomPerson",
  "oxAuthIssuer": "https://test.jans.io",
  "protectionMode": "OAUTH",
  "maxCount": 200,
  "userExtensionSchemaURI": "urn:ietf:params:scim:schemas:extension:gluu:2.0:User",
  "loggingLevel": "INFO",
  "loggingLayout": "text",
  "externalLoggerConfiguration": null,
  "metricReporterInterval": 300,
  "metricReporterKeepDataDays": 15,
  "metricReporterEnabled": true,
  "disableJdkLogger": true,
  "useLocalCache": true,
  "bulkMaxOperations": 30,
  "bulkMaxPayloadSize": 3072000
}
```

