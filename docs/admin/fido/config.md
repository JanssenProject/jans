---
tags:
  - administration
  - fido2

---

### Configuration Parameters of Janssen's FIDO2 server:
| Field named | Example | Description|
|--|--|--|
|issuer| https://my-jans-server.jans.io | URL using the https scheme with no query or fragment component. The OP asserts this as its Issuer Identifier|
  |baseEndpoint| https://my-jans-server/jans-fido2/restv1 | Base URL of the FIDO2 server Endpoints |
  | cleanServiceInterval | 60| Time interval for the Clean Service in seconds. |
  |cleanServiceBatchChunkSize | 10000| Each clean up iteration fetches chunk of expired data per base dn and removes it from storage. |
  | useLocalCache | true| Boolean value specifying whether to enable local in-memory cache for attributes, scopes, clients and organization configuration|
  | disableJdkLogger |true| Boolean value specifying whether to enable JDK Loggers |
  |loggingLevel | "INFO" or "TRACE" or "DEBUG" | Logging level for FIDO2 server|
  |loggingLayout |"text" or "json"|Contents of logs as plain text or json format|
  |externalLoggerConfiguration||Path to external log4j2 logging configuration|
  |metricReporterInterval|300|The interval for metric reporter in seconds.|
  |metricReporterKeepDataDays|15|The number of days to retain metric reported data in the system|
  |metricReporterEnabled| true |Boolean value specifying whether to enable Metric Reporter|
  | personCustomObjectClassList | ["jansCustomPerson", "jansPerson"  ] |LDAP custom object class list for dynamic person enrollment.|
  |fido2Configuration|See JSON contents in the below example | FIDO2 Configuration |
| authenticatorCertsFolder | /etc/jans/conf/fido2/authenticator_cert |Authenticators certificates fodler. |
| mdsCertsFolder | /etc/jans/conf/fido2/mds/cert |MDS TOC root certificates folder. |
| mdsTocsFolder | /etc/jans/conf/fido2/mds/toc |MDS TOC files folder. |
| serverMetadataFolder | /etc/jans/conf/fido2/server_metadata | Authenticators metadata in json format. Example: virtual devices.|
|requestedCredentialTypes|["RS256","ES256"]| |
|  requestedParties| [{"name":"https://my-jans-server.jans.io","domains":["my-jans-server.jans.io"]}]| Requested party name.|
  |userAutoEnrollment |false|Allow to enroll users on enrollment/authentication requests.|
  |unfinishedRequestExpiration| 180|Expiration time in seconds for pending enrollment/authentication requests|
  |authenticationHistoryExpiration|1296000|Expiration time in seconds for approved authentication requests.|

### Configuring the FIDO2 server:
#### 1. Read Configuration parameters:

Use the following command to obtain configuration parameters:

`/opt/jans/jans-cli/config-cli.py --operation-id get-properties-fido2`

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
  "superGluuEnabled": true,
  "oldU2fMigrationEnabled": true,
  "fido2Configuration": {
    "authenticatorCertsFolder": "/etc/jans/conf/fido2/authenticator_cert",
    "mdsCertsFolder": "/etc/jans/conf/fido2/mds/cert",
    "mdsTocsFolder": "/etc/jans/conf/fido2/mds/toc",
    "checkU2fAttestations": false,
    "userAutoEnrollment": false,
    "unfinishedRequestExpiration": 180,
    "authenticationHistoryExpiration": 1296000,
    "serverMetadataFolder": "/etc/jans/conf/fido2/server_metadata",
    "requestedCredentialTypes": [
      "RS256",
      "ES256"
    ],
    "requestedParties": [
      {
        "name": "https://my.jans.server",
        "domains": [
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
  `/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/config_values.json`

#### 3. Change log level of FIDO2 server
  Steps:
  A. Create a JSON file say `/tmp/config_values.json` by editing the JSON from Point 1. Edit `loggingLevel` to `TRACE` or `DEBUG` or `INFO`
  B. Use the following command
    `/opt/jans/jans-cli/config-cli.py --operation-id put-properties-fido --data /tmp/config_values.json`

#### 4. Locating FIDO2 configuration in Persistence Layer

While it is not recommended that an administrator directly edits a configuration at the persistence layer, it may be useful information for a developer.

##### A. MySQL
```mermaid
erDiagram
    jansAppConf {
        string doc_id PK ""
        string ou  "jans-fido2"
        string jansConfDyn "json configuration for the app"
    }
```

##### B. LDAP

```mermaid
graph LR
A[ou=jans] --> V(ou=configuration)
     V --> V5[ou=jans-fido2]
```
