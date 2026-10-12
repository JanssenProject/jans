---
tags:
- administration
- reference
- json
- properties
---

# Lock Configuration Properties

| Property Name | Description |  | 
|-----|-----|-----|
| allowedProducerIds | Producer ids the client is allowed to submit records for; "*" allows any producer | [Details](#allowedproducerids) |
| auditPersistenceMode | Audit persistence mode | [Details](#auditpersistencemode) |
| baseDN | Entry Base distinguished name (DN) that identifies the starting point of a search | [Details](#basedn) |
| baseEndpoint | Lock base endpoint URL | [Details](#baseendpoint) |
| cedarlingConfiguration | Cedarling configuration | [Details](#cedarlingconfiguration) |
| cleanServiceBatchChunkSize | Each clean up iteration fetches chunk of expired data per base dn and removes it from storage | [Details](#cleanservicebatchchunksize) |
| cleanServiceInterval | Time interval for the Clean Service in seconds | [Details](#cleanserviceinterval) |
| clientDomainBindings | OAuth client to evidence-domain bindings | [Details](#clientdomainbindings) |
| clientId | Lock Client ID | [Details](#clientid) |
| clientId | OAuth client id bound to the evidence domain | [Details](#clientid) |
| clientPassword | Lock client password | [Details](#clientpassword) |
| defaultEvidenceDomainId | Evidence domain id used when a submitting client has no explicit binding | [Details](#defaultevidencedomainid) |
| disableExternalLoggerConfiguration | Choose whether to disable external log4j configuration override | [Details](#disableexternalloggerconfiguration) |
| disableJdkLogger | Choose whether to disable JDK loggers | [Details](#disablejdklogger) |
| enabled | Enable TRACE evidence ingestion endpoints | [Details](#enabled) |
| errorReasonEnabled | Boolean value specifying whether to return detailed reason of the error from AS. Default value is false | [Details](#errorreasonenabled) |
| evidenceDomainId | Evidence domain id the client is bound to | [Details](#evidencedomainid) |
| externalLoggerConfiguration | The path to the external log4j2 logging configuration | [Details](#externalloggerconfiguration) |
| grpcConfiguration | gRPC server configuration | [Details](#grpcconfiguration) |
| grpcPort | Specify grpc port | [Details](#grpcport) |
| latenessThresholdSeconds | Seconds after signed_at a record is accepted before being flagged late | [Details](#latenessthresholdseconds) |
| loggingLayout | Logging layout used for Jans Authorization Server loggers | [Details](#logginglayout) |
| loggingLevel | Specify the logging level of loggers | [Details](#logginglevel) |
| maxArrayLength | Maximum accepted JSON array length in a TRACE assertion | [Details](#maxarraylength) |
| maxBulkRecords | Maximum number of assertions accepted in one POST /audit/trace/bulk request | [Details](#maxbulkrecords) |
| maxBulkRequestBytes | Maximum total body size, in bytes, buffered for one POST /audit/trace/bulk request; clamped to 16777216 regardless of this value | [Details](#maxbulkrequestbytes) |
| maxJsonDepth | Maximum accepted JSON nesting depth of a TRACE assertion | [Details](#maxjsondepth) |
| maxObjectMembers | Maximum accepted JSON object member count in a TRACE assertion | [Details](#maxobjectmembers) |
| maxRequestBytes | Maximum accepted TRACE request body size in bytes | [Details](#maxrequestbytes) |
| maxStringLength | Maximum accepted JSON string length (UTF-16 units) in a TRACE assertion | [Details](#maxstringlength) |
| messageConsumerType | PubSub consumer service | [Details](#messageconsumertype) |
| metricReporterEnabled | Enable metric reporter | [Details](#metricreporterenabled) |
| metricReporterInterval | The interval for metric reporter in seconds | [Details](#metricreporterinterval) |
| metricReporterKeepDataDays | The days to keep metric reported data | [Details](#metricreporterkeepdatadays) |
| openIdIssuer | OpenID issuer URL | [Details](#openidissuer) |
| pendingReceiptTimeoutSeconds | Seconds a PENDING receipt may stay unresolved before the repair timer settles it | [Details](#pendingreceipttimeoutseconds) |
| protectionMode | Protection mode for the Lock server (OAuth or Cedarling) | [Details](#protectionmode) |
| receiptAllocationRetryLimit | Maximum retries when allocating a receipt-chain sequence number | [Details](#receiptallocationretrylimit) |
| receiptRepairIntervalSeconds | Interval in seconds between receipt repair timer runs | [Details](#receiptrepairintervalseconds) |
| serverMode | gRPC server mode | [Details](#servermode) |
| statEnabled | Active stat enabled | [Details](#statenabled) |
| statTimerIntervalInSeconds | Statistical data capture time interval | [Details](#stattimerintervalinseconds) |
| tlsCertChainFilePath | TLS Cert Chain File Path | [Details](#tlscertchainfilepath) |
| tlsPrivateKeyFilePath | TLS Private Key File Path | [Details](#tlsprivatekeyfilepath) |
| tokenChannels | List of token channel names | [Details](#tokenchannels) |
| traceConfiguration | TRACE evidence ingestion configuration | [Details](#traceconfiguration) |
| useTls | Use TLS for gRPC communication | [Details](#usetls) |


## allowedProducerIds

- Description: Producer ids the client is allowed to submit records for; "*" allows any producer

- Required: No

- Default value: None


## auditPersistenceMode

- Description: Audit persistence mode

- Required: No

- Default value: None


## baseDN

- Description: Entry Base distinguished name (DN) that identifies the starting point of a search

- Required: No

- Default value: None


## baseEndpoint

- Description: Lock base endpoint URL

- Required: No

- Default value: None


## cedarlingConfiguration

- Description: Cedarling configuration

- Required: No

- Default value: None


## cleanServiceBatchChunkSize

- Description: Each clean up iteration fetches chunk of expired data per base dn and removes it from storage

- Required: No

- Default value: None


## cleanServiceInterval

- Description: Time interval for the Clean Service in seconds

- Required: No

- Default value: None


## clientDomainBindings

- Description: OAuth client to evidence-domain bindings

- Required: No

- Default value: None


## clientId

- Description: Lock Client ID

- Required: No

- Default value: None


## clientId

- Description: OAuth client id bound to the evidence domain

- Required: No

- Default value: None


## clientPassword

- Description: Lock client password

- Required: No

- Default value: None


## defaultEvidenceDomainId

- Description: Evidence domain id used when a submitting client has no explicit binding

- Required: No

- Default value: None


## disableExternalLoggerConfiguration

- Description: Choose whether to disable external log4j configuration override

- Required: No

- Default value: true


## disableJdkLogger

- Description: Choose whether to disable JDK loggers

- Required: No

- Default value: true


## enabled

- Description: Enable TRACE evidence ingestion endpoints

- Required: No

- Default value: true


## errorReasonEnabled

- Description: Boolean value specifying whether to return detailed reason of the error from AS. Default value is false

- Required: No

- Default value: false


## evidenceDomainId

- Description: Evidence domain id the client is bound to

- Required: No

- Default value: None


## externalLoggerConfiguration

- Description: The path to the external log4j2 logging configuration

- Required: No

- Default value: None


## grpcConfiguration

- Description: gRPC server configuration

- Required: No

- Default value: None


## grpcPort

- Description: Specify grpc port

- Required: No

- Default value: 50051


## latenessThresholdSeconds

- Description: Seconds after signed_at a record is accepted before being flagged late

- Required: No

- Default value: 300


## loggingLayout

- Description: Logging layout used for Jans Authorization Server loggers

- Required: No

- Default value: None


## loggingLevel

- Description: Specify the logging level of loggers

- Required: No

- Default value: None


## maxArrayLength

- Description: Maximum accepted JSON array length in a TRACE assertion

- Required: No

- Default value: 256


## maxBulkRecords

- Description: Maximum number of assertions accepted in one POST /audit/trace/bulk request

- Required: No

- Default value: 100


## maxBulkRequestBytes

- Description: Maximum total body size, in bytes, buffered for one POST /audit/trace/bulk request; clamped to 16777216 regardless of this value

- Required: No

- Default value: 4194304


## maxJsonDepth

- Description: Maximum accepted JSON nesting depth of a TRACE assertion

- Required: No

- Default value: 32


## maxObjectMembers

- Description: Maximum accepted JSON object member count in a TRACE assertion

- Required: No

- Default value: 256


## maxRequestBytes

- Description: Maximum accepted TRACE request body size in bytes

- Required: No

- Default value: 262144


## maxStringLength

- Description: Maximum accepted JSON string length (UTF-16 units) in a TRACE assertion

- Required: No

- Default value: 8192


## messageConsumerType

- Description: PubSub consumer service

- Required: No

- Default value: None


## metricReporterEnabled

- Description: Enable metric reporter

- Required: No

- Default value: None


## metricReporterInterval

- Description: The interval for metric reporter in seconds

- Required: No

- Default value: None


## metricReporterKeepDataDays

- Description: The days to keep metric reported data

- Required: No

- Default value: None


## openIdIssuer

- Description: OpenID issuer URL

- Required: No

- Default value: None


## pendingReceiptTimeoutSeconds

- Description: Seconds a PENDING receipt may stay unresolved before the repair timer settles it

- Required: No

- Default value: 120


## protectionMode

- Description: Protection mode for the Lock server (OAuth or Cedarling)

- Required: No

- Default value: None


## receiptAllocationRetryLimit

- Description: Maximum retries when allocating a receipt-chain sequence number

- Required: No

- Default value: 8


## receiptRepairIntervalSeconds

- Description: Interval in seconds between receipt repair timer runs

- Required: No

- Default value: 300


## serverMode

- Description: gRPC server mode

- Required: No

- Default value: None


## statEnabled

- Description: Active stat enabled

- Required: No

- Default value: None


## statTimerIntervalInSeconds

- Description: Statistical data capture time interval

- Required: No

- Default value: None


## tlsCertChainFilePath

- Description: TLS Cert Chain File Path

- Required: No

- Default value: 


## tlsPrivateKeyFilePath

- Description: TLS Private Key File Path

- Required: No

- Default value: 


## tokenChannels

- Description: List of token channel names

- Required: No

- Default value: jans_token


## traceConfiguration

- Description: TRACE evidence ingestion configuration

- Required: No

- Default value: None


## useTls

- Description: Use TLS for gRPC communication

- Required: No

- Default value: false


