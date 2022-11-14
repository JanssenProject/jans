---
tags:
  - administration
  - reference
  - json
---

Use the following command to obtain configuration parameters:

`/opt/jans/jans-cli/config-cli.py --operation-id get-properties-fido2`

Response:
```
{
   "issuer":"https://.jans.io",
   "baseEndpoint":"https://my-jans-server.jans.io/jans-fido2/restv1",
   "cleanServiceInterval":60,
   "cleanServiceBatchChunkSize":10000,
   "useLocalCache":true,
   "disableJdkLogger":true,
   "loggingLevel":"INFO",
   "loggingLayout":"text",
   "externalLoggerConfiguration":"",
   "metricReporterInterval":300,
   "metricReporterKeepDataDays":15,
   "metricReporterEnabled":true,
   "personCustomObjectClassList":[
      "jansCustomPerson",
      "jansPerson"
   ],
   "fido2Configuration":{
      "authenticatorCertsFolder":"/etc/jans/conf/fido2/authenticator_cert",
      "mdsCertsFolder":"/etc/jans/conf/fido2/mds/cert",
      "mdsTocsFolder":"/etc/jans/conf/fido2/mds/toc",
      "serverMetadataFolder":"/etc/jans/conf/fido2/server_metadata",
      "requestedCredentialTypes":[
         "RS256",
         "ES256"
      ],
      "requestedParties":[
         {
            "name":"https://my-jans-server.jans.io",
            "domains":[
               "my-jans-server.jans.io"
            ]
         }
      ],
      "userAutoEnrollment":false,
      "unfinishedRequestExpiration":180,
      "authenticationHistoryExpiration":1296000
   }
}

```
### References:

1. [Configuring the FIDO2 server](../../fido/config)
