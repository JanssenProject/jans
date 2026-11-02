# Lock Configuration Properties

| Property Name                      | Description                                                                                             |                                                |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| auditPersistenceMode               | Audit persistence mode                                                                                  | [Details](#auditpersistencemode)               |
| baseDN                             | Entry Base distinguished name (DN) that identifies the starting point of a search                       | [Details](#basedn)                             |
| baseEndpoint                       | Lock base endpoint URL                                                                                  | [Details](#baseendpoint)                       |
| cedarlingConfiguration             | Cedarling configuration                                                                                 | [Details](#cedarlingconfiguration)             |
| cleanServiceBatchChunkSize         | Each clean up iteration fetches chunk of expired data per base dn and removes it from storage           | [Details](#cleanservicebatchchunksize)         |
| cleanServiceInterval               | Time interval for the Clean Service in seconds                                                          | [Details](#cleanserviceinterval)               |
| clientId                           | Lock Client ID                                                                                          | [Details](#clientid)                           |
| clientPassword                     | Lock client password                                                                                    | [Details](#clientpassword)                     |
| disableExternalLoggerConfiguration | Choose whether to disable external log4j configuration override                                         | [Details](#disableexternalloggerconfiguration) |
| disableJdkLogger                   | Choose whether to disable JDK loggers                                                                   | [Details](#disablejdklogger)                   |
| errorReasonEnabled                 | Boolean value specifying whether to return detailed reason of the error from AS. Default value is false | [Details](#errorreasonenabled)                 |
| externalLoggerConfiguration        | The path to the external log4j2 logging configuration                                                   | [Details](#externalloggerconfiguration)        |
| grpcConfiguration                  | gRPC server configuration                                                                               | [Details](#grpcconfiguration)                  |
| grpcPort                           | Specify grpc port                                                                                       | [Details](#grpcport)                           |
| loggingLayout                      | Logging layout used for Jans Authorization Server loggers                                               | [Details](#logginglayout)                      |
| loggingLevel                       | Specify the logging level of loggers                                                                    | [Details](#logginglevel)                       |
| messageConsumerType                | PubSub consumer service                                                                                 | [Details](#messageconsumertype)                |
| metricReporterEnabled              | Enable metric reporter                                                                                  | [Details](#metricreporterenabled)              |
| metricReporterInterval             | The interval for metric reporter in seconds                                                             | [Details](#metricreporterinterval)             |
| metricReporterKeepDataDays         | The days to keep metric reported data                                                                   | [Details](#metricreporterkeepdatadays)         |
| openIdIssuer                       | OpenID issuer URL                                                                                       | [Details](#openidissuer)                       |
| protectionMode                     | Protection mode for the Lock server (OAuth or Cedarling)                                                | [Details](#protectionmode)                     |
| serverMode                         | gRPC server mode                                                                                        | [Details](#servermode)                         |
| statEnabled                        | Active stat enabled                                                                                     | [Details](#statenabled)                        |
| statTimerIntervalInSeconds         | Statistical data capture time interval                                                                  | [Details](#stattimerintervalinseconds)         |
| tlsCertChainFilePath               | TLS Cert Chain File Path                                                                                | [Details](#tlscertchainfilepath)               |
| tlsPrivateKeyFilePath              | TLS Private Key File Path                                                                               | [Details](#tlsprivatekeyfilepath)              |
| tokenChannels                      | List of token channel names                                                                             | [Details](#tokenchannels)                      |
| useTls                             | Use TLS for gRPC communication                                                                          | [Details](#usetls)                             |

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

## clientId

- Description: Lock Client ID
- Required: No
- Default value: None

## clientPassword

- Description: Lock client password
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

## errorReasonEnabled

- Description: Boolean value specifying whether to return detailed reason of the error from AS. Default value is false
- Required: No
- Default value: false

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

## loggingLayout

- Description: Logging layout used for Jans Authorization Server loggers
- Required: No
- Default value: None

## loggingLevel

- Description: Specify the logging level of loggers
- Required: No
- Default value: None

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

## protectionMode

- Description: Protection mode for the Lock server (OAuth or Cedarling)
- Required: No
- Default value: None

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

## useTls

- Description: Use TLS for gRPC communication
- Required: No
- Default value: false
