---
tags:
  - administration
  - fido2

---

### Configuration Parameters of Janssen's FIDO2 server:

| Field named                 | Example                                  | Description                                                                                                                     |
|-----------------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| issuer                      | https://my-jans-server.jans.io           | URL using the https scheme with no query or fragment component. The OP asserts this as its Issuer Identifier                    |
| baseEndpoint                | https://my-jans-server/jans-fido2/restv1 | Base URL of the FIDO2 server Endpoints                                                                                          |
| cleanServiceInterval        | 60                                       | Time interval for the Clean Service in seconds.                                                                                 |
| cleanServiceBatchChunkSize  | 10000                                    | Each clean up iteration fetches chunk of expired data per base dn and removes it from storage.                                  |
| useLocalCache               | true                                     | Boolean value specifying whether to enable local in-memory cache for attributes, scopes, clients and organization configuration |
| disableJdkLogger            | true                                     | Boolean value specifying whether to enable JDK Loggers                                                                          |
| loggingLevel                | "INFO" or "TRACE" or "DEBUG"             | Logging level for FIDO2 server                                                                                                  |
| loggingLayout               | "text" or "json"                         | Contents of logs as plain text or json format                                                                                   |
| externalLoggerConfiguration |                                          | Path to external log4j2 logging configuration                                                                                   |
| metricReporterInterval      | 300                                      | The interval for metric reporter in seconds.                                                                                    |
| metricReporterKeepDataDays  | 15                                       | The number of days to retain metric reported data in the system                                                                 |
| metricReporterEnabled       | true                                     | Boolean value specifying whether to enable Metric Reporter                                                                      |
| fido2Configuration          | See JSON contents in the below example   | FIDO2 Configuration                                                                                                             |

#### Fido2Configuration structure

| Field named                             | Example                                                                        | Description                                                                                             |
|-----------------------------------------|--------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| authenticatorCertsFolder                | /etc/jans/conf/fido2/authenticator_cert                                        | Authenticators certificates fodler.                                                                     |
| mdsCertsFolder                          | /etc/jans/conf/fido2/mds/cert                                                  | MDS TOC root certificates folder.                                                                       |
| mdsTocsFolder                           | /etc/jans/conf/fido2/mds/toc                                                   | MDS TOC files folder.                                                                                   |
| serverMetadataFolder                    | /etc/jans/conf/fido2/server_metadata                                           | Authenticators metadata in json format. Example: virtual devices.                                       |
| metadataUrlsProvider                    | https://mds3.fido.tools                                                        | String value to provide source of URLs with external metadata.                                          |
| enabledFidoAlgorithms                   | ["RS256","ES256"]                                                              |                                                                                                         |
| rp                                      | [{"id":"https://my-jans-server.jans.io","origins":["my-jans-server.jans.io"]}] | Requested party id.                                                                                     |
| debugUserAutoEnrollment                 | false                                                                          | Allow to enroll users on enrollment/authentication requests. (Useful while running tests)               |
| unfinishedRequestExpiration             | 180                                                                            | Expiration time in seconds for pending enrollment/authentication requests                               |
| authenticationHistoryExpiration         | 1296000                                                                        | Expiration time in seconds for approved authentication requests.                                        |
| disableMetadataService                  | false                                                                          | Boolean value indicating whether the MDS download should be omitted                                     |
| attestationMode                         | "monitor"                                                                      | Enum value indicating whether MDS validation should be omitted during attestation                       |
| assertionOptionsGenerateEndpointEnabled | false                                                                          | Boolean value indicating whether the assertion custom endpoint (used especially in passkey) is enabled. |

### Configuring the FIDO2 server:
#### 1. Read Configuration parameters:

Use the following command to obtain configuration parameters:

`jans cli --operation-id get-properties-fido2`

Response:
```
{
  "issuer": "https://my.jans.server",
  "baseEndpoint": "https://my.jans.server/jans-fido2/restv1",
  "cleanServiceInterval": 60,
  "cleanServiceBatchChunkSize": 10000,
  "useLocalCache": true,
  "disableJdkLogger": true,
  "loggingLevel": "DEBUG",
  "loggingLayout": "text",
  "metricReporterInterval": 300,
  "metricReporterKeepDataDays": 15,
  "metricReporterEnabled": true,
  "personCustomObjectClassList": [
    "jansCustomPerson",
    "jansPerson"
  ],
  "sessionIdPersistInCache": false,
  "fido2Configuration": {
    "authenticatorCertsFolder": "/etc/jans/conf/fido2/authenticator_cert",
    "mdsCertsFolder": "/etc/jans/conf/fido2/mds/cert",
    "mdsTocsFolder": "/etc/jans/conf/fido2/mds/toc",
    "checkU2fAttestations": false,
    "debugUserAutoEnrollment": false,
    "unfinishedRequestExpiration": 180,
    "authenticationHistoryExpiration": 1296000,
    "serverMetadataFolder": "/etc/jans/conf/fido2/server_metadata",
    "metadataUrlsProvider": "",
    "disableMetadataService": false,
    "attestationMode": "monitor",
    "assertionOptionsGenerateEndpointEnabled":true,
    "enabledFidoAlgorithms": [
      "RS256",
      "ES256"
    ],
    "rp": [
      {
        "id": "https://my.jans.server",
        "origins": [
          "my.jans.server"
        ]
      }
    ]
  }
}
```


#### 2. Update configuration parameters:
  Steps:
  A. Create a JSON file say `/tmp/config_values.json` by editing the JSON from Point 1
  B. Use the following command
  `jans cli --operation-id post-config-scripts --data /tmp/config_values.json`

#### 3. Change log level of FIDO2 server
  Steps:
  A. Create a JSON file say `/tmp/config_values.json` by editing the JSON from Point 1. Edit `loggingLevel` to `TRACE` or `DEBUG` or `INFO`
  B. Use the following command
    `jans cli --operation-id put-properties-fido --data /tmp/config_values.json`

#### 4. Locating FIDO2 configuration in Persistence Layer

While it is not recommended that an administrator directly edits a configuration at the persistence layer, it may be useful information for a developer.

##### MySQL
```mermaid
erDiagram
    jansAppConf {
        string doc_id PK ""
        string ou  "jans-fido2"
        string jansConfDyn "json configuration for the app"
    }
```

