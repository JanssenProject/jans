---
tags:
- administration
- reference
- json
- properties
---

# Fido2 Configuration Properties

| Property Name | Description |  | 
|-----|-----|-----|
| authenticationHistoryExpiration | Expiration time in seconds for approved authentication requests | [Details](#authenticationhistoryexpiration) |
| authenticatorCertsFolder | Authenticators certificates folder | [Details](#authenticatorcertsfolder) |
| baseEndpoint | The base URL for Fido2 endpoints | [Details](#baseendpoint) |
| checkU2fAttestations | Boolean value indicating if U2f attestation needs to be checked | [Details](#checku2fattestations) |
| cleanServiceBatchChunkSize | Each clean up iteration fetches chunk of expired data per base dn and removes it from storage | [Details](#cleanservicebatchchunksize) |
| cleanServiceInterval | Time interval for the Clean Service in seconds | [Details](#cleanserviceinterval) |
| disableJdkLogger | Boolean value specifying whether to enable JDK Loggers | [Details](#disablejdklogger) |
| externalLoggerConfiguration | Path to external Fido2 logging configuration | [Details](#externalloggerconfiguration) |
| issuer | URL using the https scheme for Issuer identifier | [Details](#issuer) |
| loggingLayout | Logging layout used for Fido2 | [Details](#logginglayout) |
| loggingLevel | Logging level for Fido2 logger | [Details](#logginglevel) |
| mdsAccessToken | MDS access token | [Details](#mdsaccesstoken) |
| mdsCertsFolder | MDS TOC root certificates folder | [Details](#mdscertsfolder) |
| mdsTocsFolder | MDS TOC files folder | [Details](#mdstocsfolder) |
| metricReporterEnabled | Boolean value specifying whether metric reporter is enabled | [Details](#metricreporterenabled) |
| metricReporterInterval | The interval for metric reporter in seconds | [Details](#metricreporterinterval) |
| metricReporterKeepDataDays | The days to keep report data | [Details](#metricreporterkeepdatadays) |
| personCustomObjectClassList | Custom object class list for dynamic person enrolment | [Details](#personcustomobjectclasslist) |
| requestedCredentialTypes | List of Requested Credential Types | [Details](#requestedcredentialtypes) |
| requestedParties | Authenticators metadata in json format | [Details](#requestedparties) |
| serverMetadataFolder | Authenticators metadata in json format | [Details](#servermetadatafolder) |
| unfinishedRequestExpiration | Expiration time in seconds for pending enrollment/authentication requests | [Details](#unfinishedrequestexpiration) |
| useLocalCache | Boolean value to indicate if Local Cache is to be used | [Details](#uselocalcache) |
| userAutoEnrollment | Allow to enroll users on enrollment/authentication requests | [Details](#userautoenrollment) |


### authenticationHistoryExpiration

- Description: Expiration time in seconds for approved authentication requests

- Required: No

- Default value: None


### authenticatorCertsFolder

- Description: Authenticators certificates folder

- Required: No

- Default value: None


### baseEndpoint

- Description: The base URL for Fido2 endpoints

- Required: No

- Default value: None


### checkU2fAttestations

- Description: Boolean value indicating if U2f attestation needs to be checked

- Required: No

- Default value: None


### cleanServiceBatchChunkSize

- Description: Each clean up iteration fetches chunk of expired data per base dn and removes it from storage

- Required: No

- Default value: None


### cleanServiceInterval

- Description: Time interval for the Clean Service in seconds

- Required: No

- Default value: None


### disableJdkLogger

- Description: Boolean value specifying whether to enable JDK Loggers

- Required: No

- Default value: None


### externalLoggerConfiguration

- Description: Path to external Fido2 logging configuration

- Required: No

- Default value: None


### issuer

- Description: URL using the https scheme for Issuer identifier

- Required: No

- Default value: None


### loggingLayout

- Description: Logging layout used for Fido2

- Required: No

- Default value: None


### loggingLevel

- Description: Logging level for Fido2 logger

- Required: No

- Default value: None


### mdsAccessToken

- Description: MDS access token

- Required: No

- Default value: None


### mdsCertsFolder

- Description: MDS TOC root certificates folder

- Required: No

- Default value: None


### mdsTocsFolder

- Description: MDS TOC files folder

- Required: No

- Default value: None


### metricReporterEnabled

- Description: Boolean value specifying whether metric reporter is enabled

- Required: No

- Default value: None


### metricReporterInterval

- Description: The interval for metric reporter in seconds

- Required: No

- Default value: None


### metricReporterKeepDataDays

- Description: The days to keep report data

- Required: No

- Default value: None


### personCustomObjectClassList

- Description: Custom object class list for dynamic person enrolment

- Required: No

- Default value: None


### requestedCredentialTypes

- Description: List of Requested Credential Types

- Required: No

- Default value: None


### requestedParties

- Description: Authenticators metadata in json format

- Required: No

- Default value: None


### serverMetadataFolder

- Description: Authenticators metadata in json format

- Required: No

- Default value: None


### unfinishedRequestExpiration

- Description: Expiration time in seconds for pending enrollment/authentication requests

- Required: No

- Default value: None


### useLocalCache

- Description: Boolean value to indicate if Local Cache is to be used

- Required: No

- Default value: None


### userAutoEnrollment

- Description: Allow to enroll users on enrollment/authentication requests

- Required: No

- Default value: None


