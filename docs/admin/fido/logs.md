---
tags:
  - administration
  - fido
---

### Log level and Logging Layout Parameters of Janssen's FIDO2 server:

| Field named | Example | Description|
|--|--|--|
|loggingLevel | "INFO" or "TRACE" or "DEBUG" | Logging level for FIDO2 server|
|loggingLayout |"text" or "json" |Contents of logs as plain text or json format|

#### 1. Read Configuration parameters:

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

#### 2. Update `loggingLevel` or `loggingLayout`:
  Steps:
  A. Create a JSON file say `/tmp/config_values.json` by editing the JSON from Point 1 and
     - edit `loggingLevel` to `TRACE` or `DEBUG` or `INFO`
     - edit `loggingLayout` to `text` or `json`

  B. Use the following command to update the logging level
  `/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/config_values.json`

  C. restart `jans-fido2`
     `service fido2 restart` or `systemctl restart fido2`

### Location of logs in FIDO2 server:

Logs can be found at `/opt/jans/jetty/jans-fido2/logs`
