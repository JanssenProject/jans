---
tags:
  - administration
  - fido
---

# FIDO server Logs

## Log level and Logging Layout

| Field named | Example | Description|
|--|--|--|
|loggingLevel | "INFO" or "TRACE" or "DEBUG" | Logging level for FIDO2 server|
|loggingLayout |"text" or "json" |Contents of logs as plain text or json format|

## Configure logging

### Read Configuration parameters

Use the following command to obtain configuration parameters.

```bash title="Command"
jans cli --operation-id get-properties-fido2
```

```json title="Response"
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
      "enabledFidoAlgorithms":[
         "RS256",
         "ES256"
      ],
      "rp":[
         {
            "id":"https://my-jans-server.jans.io",
            "origins":[
               "my-jans-server.jans.io"
            ]
         }
      ],
      "debugUserAutoEnrollment":false,
      "unfinishedRequestExpiration":180,
      "authenticationHistoryExpiration":1296000
   }
}

```

Store this content in a file, say `/tmp/config_values.json`.

### Update logging level

1. Create a JSON file say `/tmp/config_values.json` by editing the JSON from Point 1

      - edit `loggingLevel` to `TRACE` or `DEBUG` or `INFO`
      
      - edit `loggingLayout` to `text` or `json`

2. Use the following command to update the logging level

      ```bash
      jans cli \
      --operation-id put-properties-fido2 \
      --data /tmp/config_values.json
      ```

3. Restart `jans-fido2`

      ```shell
      systemctl restart fido2
      ```

### Location of logs in FIDO2 server:

Logs can be found at `/opt/jans/jetty/jans-fido2/logs`
