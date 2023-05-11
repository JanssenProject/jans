---
tags:
  - administration
  - scim
  - configuration
---

# SCIM configuration
[SCIM](https://github.com/JanssenProject/jans/tree/main/jans-scim) configuration enables to manage application-level configuration.


### Existing SCIM dynamic configuration
| Field named | Example | Description|
|--|--|--|
| baseDN| o=jans | Application config Base DN.|
| applicationUrl| https://my-jans-server.jans.io | Application base URL. |
| baseEndpoint | https://my-jans-server.jans.io/jans-scim/restv1| SCIM base endpoint URL.|
| personCustomObjectClass | jansCustomPerson| Person Object Class.|
| oxAuthIssuer | https://my-jans-server.jans.io | Jans Auth - Issuer identifier.|
| umaIssuer | https://my-jans-server.jans.io | Jans Auth -  UMA Issuer identifier.|
| maxCount|200| Maximum number of results per page.|
| bulkMaxOperations|30| Specifies maximum bulk operations in bulk request.|
| bulkMaxPayloadSize|3072000| Specifies maximum bulk operations.|
| userExtensionSchemaURI|urn:ietf:params:scim:schemas:extension:gluu:2.0:User| User Extension Schema URI.|
| useLocalCache| true | Boolean value specifying whether to enable local in-memory cache.|
| disableJdkLogger| true | Boolean value specifying whether to enable JDK Loggers.|
| loggingLevel| "INFO" | Logging level for scim logger.|
| loggingLayout |"text" | Logging layout used for Server loggers. |
| externalLoggerConfiguration|| Path to external log4j2 logging configuration.|
| metricReporterInterval|300| The interval for metric reporter in seconds.|
| metricReporterKeepDataDays|15| The number of days to retain metric reported data in the system.|
| metricReporterEnabled| true |Boolean value specifying metric reported data enabled flag.|


### Configuring the SCIM server:
#### 1. Read Configuration parameters:

Use the following command to obtain configuration parameters:

`/opt/jans/jans-cli/config-cli.py --operation-id get-scim-config`

> ```javascript
>{
>   "baseDN":"o=jans",
>
>   "applicationUrl":"https://my.jans.server",
>   "baseEndpoint":"https://my.jans.server/jans-scim/restv1",
>
>   "personCustomObjectClass":"jansCustomPerson",
>
>   "oxAuthIssuer":"https://my.jans.server",
>   "umaIssuer":"https://my.jans.server",
>
>   "maxCount": 200,
>   "bulkMaxOperations": 30,
>   "bulkMaxPayloadSize": 3072000,
>   "userExtensionSchemaURI": "urn:ietf:params:scim:schemas:extension:gluu:2.0:User",
>
>   "useLocalCache":true,
>
>   "disableJdkLogger":true,
>   "loggingLevel":"INFO",
>   "loggingLayout":"text",
>   "externalLoggerConfiguration":"",
>
>   "metricReporterInterval":300,
>   "metricReporterKeepDataDays":15,
>   "metricReporterEnabled":true
}
> ```


## Reflect update

`jansRevision` property of the configuration is used to manage any change

### Two options to make effect of the changes done to the configuration

1. Restart jans-scim
2. Increment the `jansRevision` property of the configuration without restarting the application. The timer job will detect the change and fetch the latest configuration from the DB.

