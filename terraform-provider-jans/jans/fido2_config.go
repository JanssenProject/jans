package jans

import (
        "context"
        "fmt"
)

type RequestedPartyPolicy struct {
        AttestationMode string `schema:"attestation_mode" json:"attestationMode,omitempty"`
}

type RequestedParties struct {
        Id      string                `schema:"name" json:"id"`
        Origins []string              `schema:"domains" json:"origins"`
        Policy  *RequestedPartyPolicy `schema:"policy" json:"policy,omitempty"`
}

type MetadataServer struct {
        Url      string `schema:"url" json:"url"`
        RootCert string `schema:"root_cert" json:"rootCert"`
}

// Fido2Configuration represents the Fido2 configuration properties
type Fido2Configuration struct {
        AuthenticatorCertsFolder        string             `schema:"authenticator_certs_folder" json:"authenticatorCertsFolder"`
        MdsCertsFolder                  string             `schema:"mds_certs_folder" json:"mdsCertsFolder"`
        MdsTocsFolder                   string             `schema:"mds_tocs_folder" json:"mdsTocsFolder"`
        ServerMetadataFolder            string             `schema:"server_metadata_folder" json:"serverMetadataFolder"`
        RequestedParties                []RequestedParties `schema:"requested_parties" json:"requestedParties"`
        UnfinishedRequestExpiration     int                `schema:"unfinished_request_expiration" json:"unfinishedRequestExpiration"`
        AuthenticationHistoryExpiration int                `schema:"authentication_history_expiration" json:"authenticationHistoryExpiration"`
        DebugUserAutoEnrollment         bool               `schema:"user_auto_enrollment" json:"userAutoEnrollment"`
        EnabledFidoAlgorithms           []string           `schema:"requested_credential_types" json:"enabledFidoAlgorithms"`
        RecordAbandonedAssertions       bool               `schema:"record_abandoned_assertions" json:"recordAbandonedAssertions"`
        AbandonedRequestExpiration      int                `schema:"abandoned_request_expiration" json:"abandonedRequestExpiration"`
        AbandonedRequestSweepInterval   int                `schema:"abandoned_request_sweep_interval" json:"abandonedRequestSweepInterval"`
        MetadataServers                 []MetadataServer   `schema:"metadata_servers" json:"metadataServers"`
        DisableMetadataService          bool               `schema:"disable_metadata_service" json:"disableMetadataService"`
        MdsDownloadStartupRetries       int                `schema:"mds_download_startup_retries" json:"mdsDownloadStartupRetries"`
        MdsDownloadStartupRetryInterval int                `schema:"mds_download_startup_retry_interval" json:"mdsDownloadStartupRetryInterval"`
        Hints                           []string           `schema:"hints" json:"hints"`
        EnterpriseAttestation           bool               `schema:"enterprise_attestation" json:"enterpriseAttestation"`
        AttestationMode                 string             `schema:"attestation_mode" json:"attestationMode"`
        AllowedTopOrigins               []string           `schema:"allowed_top_origins" json:"allowedTopOrigins"`
}

// JansFido2DynConfiguration defines the Fido2 dynamic configuration
// of the Janssen server.
type JansFido2DynConfiguration struct {
        Issuer                             string             `schema:"issuer" json:"issuer,omitempty"`
        BaseEndpoint                       string             `schema:"base_endpoint" json:"baseEndpoint,omitempty"`
        CleanServiceInterval               int                `schema:"clean_service_interval" json:"cleanServiceInterval,omitempty"`
        CleanServiceBatchChunkSize         int                `schema:"clean_service_batch_chunk_size" json:"cleanServiceBatchChunkSize,omitempty"`
        UseLocalCache                      bool               `schema:"use_local_cache" json:"useLocalCache,omitempty"`
        DisableJdkLogger                   bool               `schema:"disable_jdk_logger" json:"disableJdkLogger,omitempty"`
        DisableExternalLoggerConfiguration bool               `schema:"disable_external_logger_configuration" json:"disableExternalLoggerConfiguration,omitempty"`
        LoggingLevel                       string             `schema:"logging_level" json:"loggingLevel,omitempty"`
        LoggingLayout                      string             `schema:"logging_layout" json:"loggingLayout,omitempty"`
        ExternalLoggerConfiguration        string             `schema:"external_logger_configuration" json:"externalLoggerConfiguration,omitempty"`
        MetricReporterEnabled              bool               `schema:"metric_reporter_enabled" json:"metricReporterEnabled,omitempty"`
        MetricReporterInterval             int                `schema:"metric_reporter_interval" json:"metricReporterInterval,omitempty"`
        MetricReporterKeepDataDays         int                `schema:"metric_reporter_keep_data_days" json:"metricReporterKeepDataDays,omitempty"`
        Fido2MetricsEnabled                bool               `schema:"fido2_metrics_enabled" json:"fido2MetricsEnabled,omitempty"`
        Fido2MetricsRetentionDays          int                `schema:"fido2_metrics_retention_days" json:"fido2MetricsRetentionDays,omitempty"`
        Fido2DeviceInfoCollection          bool               `schema:"fido2_device_info_collection" json:"fido2DeviceInfoCollection,omitempty"`
        Fido2ErrorCategorization           bool               `schema:"fido2_error_categorization" json:"fido2ErrorCategorization,omitempty"`
        Fido2PerformanceMetrics            bool               `schema:"fido2_performance_metrics" json:"fido2PerformanceMetrics,omitempty"`
        Fido2MetricsAggregationEnabled     bool               `schema:"fido2_metrics_aggregation_enabled" json:"fido2MetricsAggregationEnabled,omitempty"`
        TrustedProxyEnabled                bool               `schema:"trusted_proxy_enabled" json:"trustedProxyEnabled,omitempty"`
        TrustedProxyIpRanges               []string           `schema:"trusted_proxy_ip_ranges" json:"trustedProxyIpRanges,omitempty"`
        PersonCustomObjectClassList        []string           `schema:"person_custom_object_class_list" json:"personCustomObjectClassList,omitempty"`
        Fido2Configuration                 Fido2Configuration `schema:"fido2_configuration" json:"fido2Configuration,omitempty"`
}

// GetFido2Configuration returns the current Fido2 configuration.
func (c *Client) GetFido2Configuration(ctx context.Context) (*JansFido2DynConfiguration, error) {

        scope := "https://jans.io/oauth/config/fido2.readonly"
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        ret := &JansFido2DynConfiguration{}

        if err := c.get(ctx, "/jans-config-api/fido2/fido2-config", token, scope, ret); err != nil {
                return nil, fmt.Errorf("get request failed: %w", err)
        }

        return ret, nil
}

// UpdateFido2Configuration updates Fido2 configuration for the Janssen server.
func (c *Client) UpdateFido2Configuration(ctx context.Context, fido2Config *JansFido2DynConfiguration) (*JansFido2DynConfiguration, error) {

        scope := "https://jans.io/oauth/config/fido2.write"
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        ret := &JansFido2DynConfiguration{}

        if err := c.put(ctx, "/jans-config-api/fido2/fido2-config", token, scope, fido2Config, ret); err != nil {
                return nil, fmt.Errorf("put request failed: %w", err)
        }

        return ret, nil
}
