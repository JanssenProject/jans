---
tags:
  - administration
  - scim
---

# Logs

## SCIM-related logs

Janssen Server logs usually reveal the source of problems when things are going wrong: the first place to look is the SCIM log. Authorization issues (access tokens problems, for instance) are on the side of the Jans-auth (authorization server).

* SCIM log is located at `/opt/jans/jetty/jans-scim/logs/scim.log`

* Jans-auth log is at `/opt/jans/jetty/jans-auth/logs/jans-auth.log`

* If using the SCIM custom script in order to intercept API calls and apply custom logic, the script log is also useful: `/opt/jans/jetty/jans-scim/logs/scim_script.log`


## Logging Level and Logging Layout Parameters of Janssen's SCIM server

| Field named | Example | Description|
|--|--|--|
|loggingLevel | "INFO" or "TRACE" or "DEBUG" | Logging level for SCIM server|
|loggingLayout |"text" or "json" |Contents of logs as plain text or json format|

Generally it's convenient to set the logging level for SCIM to **DEBUG**. Let's see how to update **loggingLevel**.

**SCIM** configuration json file looks like that.

```
/opt/jans/jans-cli/config-cli.py --operation-id get-scim-config
```

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

### Update loggingLevel

Get patch schema and modify it. It should be an array.
```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > patch-scim.json
```

```commandline
#patch-scim.json

[
    {
      "op": "replace",
      "path": "/loggingLevel",
      "value": "DEBUG"
    }
]
```

Now update **SCIM** configuration 

```
/opt/jans/jans-cli/config-cli.py --operation-id patch-scim-config --data pathc-scim.json
```

Lets see our updated scim configuration file.
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
