package jans

import (
	"context"
	"fmt"
)

// GrpcConfiguration holds the gRPC transport settings of the Lock server.
type GrpcConfiguration struct {
	ServerMode            string `schema:"server_mode" json:"serverMode"`
	GrpcPort              int    `schema:"grpc_port" json:"grpcPort"`
	UseTls                bool   `schema:"use_tls" json:"useTls"`
	TlsCertChainFilePath  string `schema:"tls_cert_chain_file_path" json:"tlsCertChainFilePath"`
	TlsPrivateKeyFilePath string `schema:"tls_private_key_file_path" json:"tlsPrivateKeyFilePath"`
}

// TraceConfiguration holds the Lock server trace and evidence pipeline settings.
// The update is a full replace (PUT), so scalars are serialized without omitempty.
type TraceConfiguration struct {
	Enabled                      bool                       `schema:"enabled" json:"enabled"`
	DefaultEvidenceDomainId      string                     `schema:"default_evidence_domain_id" json:"defaultEvidenceDomainId"`
	DefaultAllowedProducerIds    []string                   `schema:"default_allowed_producer_ids" json:"defaultAllowedProducerIds"`
	ClientDomainBindings         []TraceClientDomainBinding `schema:"client_domain_bindings" json:"clientDomainBindings"`
	LatenessThresholdSeconds     int                        `schema:"lateness_threshold_seconds" json:"latenessThresholdSeconds"`
	PolicyMismatchGraceSeconds   int                        `schema:"policy_mismatch_grace_seconds" json:"policyMismatchGraceSeconds"`
	PolicyReleaseRetentionDays   int                        `schema:"policy_release_retention_days" json:"policyReleaseRetentionDays"`
	MaxFutureSkewSeconds         int                        `schema:"max_future_skew_seconds" json:"maxFutureSkewSeconds"`
	MaxRequestBytes              int                        `schema:"max_request_bytes" json:"maxRequestBytes"`
	MaxJsonDepth                 int                        `schema:"max_json_depth" json:"maxJsonDepth"`
	MaxStringLength              int                        `schema:"max_string_length" json:"maxStringLength"`
	MaxArrayLength               int                        `schema:"max_array_length" json:"maxArrayLength"`
	MaxObjectMembers             int                        `schema:"max_object_members" json:"maxObjectMembers"`
	ReceiptAllocationRetryLimit  int                        `schema:"receipt_allocation_retry_limit" json:"receiptAllocationRetryLimit"`
	CorrelationRetryLimit        int                        `schema:"correlation_retry_limit" json:"correlationRetryLimit"`
	ReceiptRepairIntervalSeconds int                        `schema:"receipt_repair_interval_seconds" json:"receiptRepairIntervalSeconds"`
	PendingReceiptTimeoutSeconds int                        `schema:"pending_receipt_timeout_seconds" json:"pendingReceiptTimeoutSeconds"`
	MaxBulkRecords               int                        `schema:"max_bulk_records" json:"maxBulkRecords"`
	MaxBulkRequestBytes          int                        `schema:"max_bulk_request_bytes" json:"maxBulkRequestBytes"`
	ChainRegistrationMode        string                     `schema:"chain_registration_mode" json:"chainRegistrationMode"`
	CryptoProvider               string                     `schema:"crypto_provider" json:"cryptoProvider"`
}

// TraceClientDomainBinding binds a Lock client to an evidence domain and the
// producer ids it may act on behalf of.
type TraceClientDomainBinding struct {
	ClientId           string   `schema:"client_id" json:"clientId"`
	EvidenceDomainId   string   `schema:"evidence_domain_id" json:"evidenceDomainId"`
	AllowedProducerIds []string `schema:"allowed_producer_ids" json:"allowedProducerIds"`
}

// LockConfiguration is the application configuration of the Jans Lock plugin.
// CedarlingConfiguration and PolicySource are shared with the config-api app
// configuration, since the Lock spec defines them identically.
type LockConfiguration struct {
	BaseDN                             string                 `schema:"base_dn" json:"baseDN"`
	BaseEndpoint                       string                 `schema:"base_endpoint" json:"baseEndpoint"`
	OpenIdIssuer                       string                 `schema:"open_id_issuer" json:"openIdIssuer"`
	ProtectionMode                     string                 `schema:"protection_mode" json:"protectionMode"`
	AuditPersistenceMode               string                 `schema:"audit_persistence_mode" json:"auditPersistenceMode"`
	CedarlingConfiguration             CedarlingConfiguration `schema:"cedarling_configuration" json:"cedarlingConfiguration,omitempty"`
	GrpcConfiguration                  GrpcConfiguration      `schema:"grpc_configuration" json:"grpcConfiguration,omitempty"`
	StatEnabled                        bool                   `schema:"stat_enabled" json:"statEnabled"`
	StatTimerIntervalInSeconds         int                    `schema:"stat_timer_interval_in_seconds" json:"statTimerIntervalInSeconds"`
	TokenChannels                      []string               `schema:"token_channels" json:"tokenChannels,omitempty"`
	ClientId                           string                 `schema:"client_id" json:"clientId"`
	ClientPassword                     string                 `schema:"client_password" json:"clientPassword"`
	DisableJdkLogger                   bool                   `schema:"disable_jdk_logger" json:"disableJdkLogger"`
	DisableExternalLoggerConfiguration bool                   `schema:"disable_external_logger_configuration" json:"disableExternalLoggerConfiguration"`
	LoggingLevel                       string                 `schema:"logging_level" json:"loggingLevel"`
	LoggingLayout                      string                 `schema:"logging_layout" json:"loggingLayout"`
	ExternalLoggerConfiguration        string                 `schema:"external_logger_configuration" json:"externalLoggerConfiguration"`
	MetricReporterInterval             int                    `schema:"metric_reporter_interval" json:"metricReporterInterval"`
	MetricReporterKeepDataDays         int                    `schema:"metric_reporter_keep_data_days" json:"metricReporterKeepDataDays"`
	MetricReporterEnabled              bool                   `schema:"metric_reporter_enabled" json:"metricReporterEnabled"`
	CleanServiceInterval               int                    `schema:"clean_service_interval" json:"cleanServiceInterval"`
	MessageConsumerType                string                 `schema:"message_consumer_type" json:"messageConsumerType"`
	ErrorReasonEnabled                 bool                   `schema:"error_reason_enabled" json:"errorReasonEnabled"`
	CleanServiceBatchChunkSize         int                    `schema:"clean_service_batch_chunk_size" json:"cleanServiceBatchChunkSize"`
	TraceConfiguration                 TraceConfiguration     `schema:"trace_configuration" json:"traceConfiguration,omitempty"`
}

// GetLockConfiguration returns the current Lock plugin configuration.
func (c *Client) GetLockConfiguration(ctx context.Context) (*LockConfiguration, error) {

	scope := "https://jans.io/oauth/lock-config.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &LockConfiguration{}

	if err := c.get(ctx, "/jans-config-api/lock/lockConfig", token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// UpdateLockConfiguration updates the Lock plugin configuration. The update is
// a full replace (PUT), so callers must send a fully populated configuration.
func (c *Client) UpdateLockConfiguration(ctx context.Context, config *LockConfiguration) (*LockConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	scope := "https://jans.io/oauth/lock-config.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &LockConfiguration{}

	if err := c.put(ctx, "/jans-config-api/lock/lockConfig", token, scope, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}
