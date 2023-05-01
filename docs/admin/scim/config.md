---
tags:
  - administration
  - scim
  - configuration
---

# SCMI configuration
[SCMI](https://github.com/JanssenProject/jans/tree/main/jans-scim) configuration enables to manage application-level configuration.


### Existing SCIM dynamic configuration

> ```javascript
>{
>    "baseDN":"o=jans",
>
>    "applicationUrl":"https://pujavs-kind-oryx.gluu.info",
>    "baseEndpoint":"https://pujavs-kind-oryx.gluu.info/jans-scim/restv1",
>
>    "personCustomObjectClass":"jansCustomPerson",
>
>    "oxAuthIssuer":"https://pujavs-kind-oryx.gluu.info",
>    "umaIssuer":"https://pujavs-kind-oryx.gluu.info",
>
>    "maxCount": 200,
>    "bulkMaxOperations": 30,
>    "bulkMaxPayloadSize": 3072000,
>    "userExtensionSchemaURI": "urn:ietf:params:scim:schemas:extension:gluu:2.0:User",
>
>    "useLocalCache":true,
>
>    "disableJdkLogger":true,
>    "loggingLevel":"INFO",
>    "loggingLayout":"text",
>    "externalLoggerConfiguration":"",
>
>    "metricReporterInterval":300,
>    "metricReporterKeepDataDays":15,
>    "metricReporterEnabled":true
>}
> ```


## Reflect update

`jansRevision` property of the configuration is used to manage any change

### Two options to make effect of the changes done to the configuration

1. Restart jans-scim
2. Increment the `jansRevision` property of the configuration without restarting the application. The timer job will detect the change and fetch the latest configuration from the DB.

