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
| abandonedRequestExpiration | Expiration time in seconds for abandoned assertion ceremonies. Kept much shorter than authenticationHistoryExpiration because conditional-UI ceremonies start on nearly every login page load, making abandonment the highest-volume outcome | [Details](#abandonedrequestexpiration) |
| abandonedRequestSweepInterval | Interval in seconds between sweeps for lapsed assertion ceremonies. Must stay below unfinishedRequestExpiration so a ceremony cannot lapse and be deleted between two sweeps | [Details](#abandonedrequestsweepinterval) |
| attestationMode | String value indicating whether MDS validation should be omitted during attestation | [Details](#attestationmode) |
| authenticationHistoryExpiration | Expiration time in seconds for approved authentication requests | [Details](#authenticationhistoryexpiration) |
| authenticatorCertsFolder | Authenticators certificates folder | [Details](#authenticatorcertsfolder) |
| baseEndpoint | The base URL for Fido2 endpoints | [Details](#baseendpoint) |
| cleanServiceBatchChunkSize | Each clean up iteration fetches chunk of expired data per base dn and removes it from storage | [Details](#cleanservicebatchchunksize) |
| cleanServiceInterval | Time interval for the Clean Service in seconds | [Details](#cleanserviceinterval) |
| disableExternalLoggerConfiguration | Choose whether to disable external log4j configuration override | [Details](#disableexternalloggerconfiguration) |
| disableJdkLogger | Boolean value specifying whether to enable JDK Loggers | [Details](#disablejdklogger) |
| disableMetadataService | Boolean value indicating whether the MDS download should be omitted | [Details](#disablemetadataservice) |
| enabledFidoAlgorithms | List of Requested Credential Types | [Details](#enabledfidoalgorithms) |
| enterpriseAttestation | If authenticators have been enabled for use in a specific protected envt (enterprise authenticators) | [Details](#enterpriseattestation) |
| externalLoggerConfiguration | Path to external Fido2 logging configuration | [Details](#externalloggerconfiguration) |
| fido2DeviceInfoCollection | Boolean value specifying whether to collect device information in FIDO2 metrics | [Details](#fido2deviceinfocollection) |
| fido2ErrorCategorization | Boolean value specifying whether to categorize errors in FIDO2 metrics | [Details](#fido2errorcategorization) |
| fido2MetricsAggregationEnabled | Boolean value specifying whether FIDO2 metrics aggregation is enabled | [Details](#fido2metricsaggregationenabled) |
| fido2MetricsAggregationInterval | Interval in minutes for FIDO2 metrics aggregation | [Details](#fido2metricsaggregationinterval) |
| fido2MetricsEnabled | Boolean value specifying whether FIDO2 passkey metrics collection is enabled | [Details](#fido2metricsenabled) |
| fido2MetricsRetentionDays | Number of days to keep FIDO2 passkey metrics data | [Details](#fido2metricsretentiondays) |
| fido2PerformanceMetrics | Boolean value specifying whether to collect detailed performance metrics for FIDO2 operations | [Details](#fido2performancemetrics) |
| fido2TrustedClientContextSources | Callers whose forwarded client-context headers (X-Jans-Client-IP, X-Jans-Client-User-Agent) are honoured, given as remote addresses. Empty means the headers are ignored; a single entry of * trusts every caller and must only be used where the FIDO2 server is not reachable by end users | [Details](#fido2trustedclientcontextsources) |
| hints | Hints to the RP - security-key, client-device, hybrid | [Details](#hints) |
| issuer | URL using the https scheme for Issuer identifier | [Details](#issuer) |
| loggingLayout | Logging layout used for Fido2 | [Details](#logginglayout) |
| loggingLevel | Logging level for Fido2 logger | [Details](#logginglevel) |
| mdsCertsFolder | MDS TOC root certificates folder | [Details](#mdscertsfolder) |
| mdsDownloadStartupRetries | Number of times the MDS TOC download is retried at server startup when the TOC blob is missing (a missing TOC prevents attestation validation) | [Details](#mdsdownloadstartupretries) |
| mdsDownloadStartupRetryInterval | Delay in seconds between MDS TOC download retries at server startup when the TOC blob is missing | [Details](#mdsdownloadstartupretryinterval) |
| mdsTocsFolder | MDS TOC files folder | [Details](#mdstocsfolder) |
| metadataServers | String value to provide source of URLs with external metadata | [Details](#metadataservers) |
| metricReporterEnabled | Boolean value specifying whether metric reporter is enabled | [Details](#metricreporterenabled) |
| metricReporterInterval | The interval for metric reporter in seconds | [Details](#metricreporterinterval) |
| metricReporterKeepDataDays | The days to keep report data | [Details](#metricreporterkeepdatadays) |
| personCustomObjectClassList | Custom object class list for dynamic person enrolment | [Details](#personcustomobjectclasslist) |
| recordAbandonedAssertions | Boolean value indicating whether assertion ceremonies that lapse without being completed are relabelled as abandoned instead of being deleted unlabelled | [Details](#recordabandonedassertions) |
| requestedParties | Authenticators metadata in json format | [Details](#requestedparties) |
| serverMetadataFolder | Authenticators metadata in json format | [Details](#servermetadatafolder) |
| unfinishedRequestExpiration | Expiration time in seconds for pending enrollment/authentication requests | [Details](#unfinishedrequestexpiration) |
| useLocalCache | Boolean value to indicate if Local Cache is to be used | [Details](#uselocalcache) |
| userAutoEnrollment | Allow to enroll users on enrollment/authentication requests | [Details](#userautoenrollment) |


## abandonedRequestExpiration

- Description: Expiration time in seconds for abandoned assertion ceremonies. Kept much shorter than authenticationHistoryExpiration because conditional-UI ceremonies start on nearly every login page load, making abandonment the highest-volume outcome

- Required: No

- Default value: 86400


## abandonedRequestSweepInterval

- Description: Interval in seconds between sweeps for lapsed assertion ceremonies. Must stay below unfinishedRequestExpiration so a ceremony cannot lapse and be deleted between two sweeps

- Required: No

- Default value: 30


## attestationMode

- Description: String value indicating whether MDS validation should be omitted during attestation

- Required: No

- Default value: monitor


## authenticationHistoryExpiration

- Description: Expiration time in seconds for approved authentication requests

- Required: No

- Default value: None


## authenticatorCertsFolder

- Description: Authenticators certificates folder

- Required: No

- Default value: None


## baseEndpoint

- Description: The base URL for Fido2 endpoints

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


## disableExternalLoggerConfiguration

- Description: Choose whether to disable external log4j configuration override

- Required: No

- Default value: true


## disableJdkLogger

- Description: Boolean value specifying whether to enable JDK Loggers

- Required: No

- Default value: None


## disableMetadataService

- Description: Boolean value indicating whether the MDS download should be omitted

- Required: No

- Default value: false


## enabledFidoAlgorithms

- Description: List of Requested Credential Types

- Required: No

- Default value: None


## enterpriseAttestation

- Description: If authenticators have been enabled for use in a specific protected envt (enterprise authenticators)

- Required: No

- Default value: false


## externalLoggerConfiguration

- Description: Path to external Fido2 logging configuration

- Required: No

- Default value: None


## fido2DeviceInfoCollection

- Description: Boolean value specifying whether to collect device information in FIDO2 metrics

- Required: No

- Default value: true


## fido2ErrorCategorization

- Description: Boolean value specifying whether to categorize errors in FIDO2 metrics

- Required: No

- Default value: true


## fido2MetricsAggregationEnabled

- Description: Boolean value specifying whether FIDO2 metrics aggregation is enabled

- Required: No

- Default value: true


## fido2MetricsAggregationInterval

- Description: Interval in minutes for FIDO2 metrics aggregation

- Required: No

- Default value: 60


## fido2MetricsEnabled

- Description: Boolean value specifying whether FIDO2 passkey metrics collection is enabled

- Required: No

- Default value: true


## fido2MetricsRetentionDays

- Description: Number of days to keep FIDO2 passkey metrics data

- Required: No

- Default value: 90


## fido2PerformanceMetrics

- Description: Boolean value specifying whether to collect detailed performance metrics for FIDO2 operations

- Required: No

- Default value: true


## fido2TrustedClientContextSources

- Description: Callers whose forwarded client-context headers (X-Jans-Client-IP, X-Jans-Client-User-Agent) are honoured, given as remote addresses. Empty means the headers are ignored; a single entry of * trusts every caller and must only be used where the FIDO2 server is not reachable by end users

- Required: No

- Default value: empty list


## hints

- Description: Hints to the RP - security-key, client-device, hybrid

- Required: No

- Default value: None


## issuer

- Description: URL using the https scheme for Issuer identifier

- Required: No

- Default value: None


## loggingLayout

- Description: Logging layout used for Fido2

- Required: No

- Default value: None


## loggingLevel

- Description: Logging level for Fido2 logger

- Required: No

- Default value: None


## mdsCertsFolder

- Description: MDS TOC root certificates folder

- Required: No

- Default value: None


## mdsDownloadStartupRetries

- Description: Number of times the MDS TOC download is retried at server startup when the TOC blob is missing (a missing TOC prevents attestation validation)

- Required: No

- Default value: 3


## mdsDownloadStartupRetryInterval

- Description: Delay in seconds between MDS TOC download retries at server startup when the TOC blob is missing

- Required: No

- Default value: 30


## mdsTocsFolder

- Description: MDS TOC files folder

- Required: No

- Default value: None


## metadataServers

- Description: String value to provide source of URLs with external metadata

- Required: No

- Default value: None


## metricReporterEnabled

- Description: Boolean value specifying whether metric reporter is enabled

- Required: No

- Default value: None


## metricReporterInterval

- Description: The interval for metric reporter in seconds

- Required: No

- Default value: None


## metricReporterKeepDataDays

- Description: The days to keep report data

- Required: No

- Default value: None


## personCustomObjectClassList

- Description: Custom object class list for dynamic person enrolment

- Required: No

- Default value: None


## recordAbandonedAssertions

- Description: Boolean value indicating whether assertion ceremonies that lapse without being completed are relabelled as abandoned instead of being deleted unlabelled

- Required: No

- Default value: true


## requestedParties

- Description: Authenticators metadata in json format

- Required: No

- Default value: None


## serverMetadataFolder

- Description: Authenticators metadata in json format

- Required: No

- Default value: None


## unfinishedRequestExpiration

- Description: Expiration time in seconds for pending enrollment/authentication requests

- Required: No

- Default value: None


## useLocalCache

- Description: Boolean value to indicate if Local Cache is to be used

- Required: No

- Default value: None


## userAutoEnrollment

- Description: Allow to enroll users on enrollment/authentication requests

- Required: No

- Default value: None


