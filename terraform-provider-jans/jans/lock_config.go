package jans

import (
	"context"
	"fmt"
)

// GrpcConfiguration holds the gRPC transport settings of the Lock server.
type GrpcConfiguration struct {
	ServerMode            string `schema:"server_mode" json:"serverMode,omitempty"`
	GrpcPort              int    `schema:"grpc_port" json:"grpcPort,omitempty"`
	UseTls                bool   `schema:"use_tls" json:"useTls,omitempty"`
	TlsCertChainFilePath  string `schema:"tls_cert_chain_file_path" json:"tlsCertChainFilePath,omitempty"`
	TlsPrivateKeyFilePath string `schema:"tls_private_key_file_path" json:"tlsPrivateKeyFilePath,omitempty"`
}

// LockConfiguration is the application configuration of the Jans Lock plugin.
// CedarlingConfiguration and PolicySource are shared with the config-api app
// configuration, since the Lock spec defines them identically.
type LockConfiguration struct {
	BaseDN                             string                 `schema:"base_dn" json:"baseDN,omitempty"`
	BaseEndpoint                       string                 `schema:"base_endpoint" json:"baseEndpoint,omitempty"`
	OpenIdIssuer                       string                 `schema:"open_id_issuer" json:"openIdIssuer,omitempty"`
	ProtectionMode                     string                 `schema:"protection_mode" json:"protectionMode,omitempty"`
	AuditPersistenceMode               string                 `schema:"audit_persistence_mode" json:"auditPersistenceMode,omitempty"`
	CedarlingConfiguration             CedarlingConfiguration `schema:"cedarling_configuration" json:"cedarlingConfiguration,omitempty"`
	GrpcConfiguration                  GrpcConfiguration      `schema:"grpc_configuration" json:"grpcConfiguration,omitempty"`
	StatEnabled                        bool                   `schema:"stat_enabled" json:"statEnabled,omitempty"`
	StatTimerIntervalInSeconds         int                    `schema:"stat_timer_interval_in_seconds" json:"statTimerIntervalInSeconds,omitempty"`
	TokenChannels                      []string               `schema:"token_channels" json:"tokenChannels,omitempty"`
	ClientId                           string                 `schema:"client_id" json:"clientId,omitempty"`
	ClientPassword                     string                 `schema:"client_password" json:"clientPassword,omitempty"`
	DisableJdkLogger                   bool                   `schema:"disable_jdk_logger" json:"disableJdkLogger,omitempty"`
	DisableExternalLoggerConfiguration bool                   `schema:"disable_external_logger_configuration" json:"disableExternalLoggerConfiguration,omitempty"`
	LoggingLevel                       string                 `schema:"logging_level" json:"loggingLevel,omitempty"`
	LoggingLayout                      string                 `schema:"logging_layout" json:"loggingLayout,omitempty"`
	ExternalLoggerConfiguration        string                 `schema:"external_logger_configuration" json:"externalLoggerConfiguration,omitempty"`
	MetricReporterInterval             int                    `schema:"metric_reporter_interval" json:"metricReporterInterval,omitempty"`
	MetricReporterKeepDataDays         int                    `schema:"metric_reporter_keep_data_days" json:"metricReporterKeepDataDays,omitempty"`
	MetricReporterEnabled              bool                   `schema:"metric_reporter_enabled" json:"metricReporterEnabled,omitempty"`
	CleanServiceInterval               int                    `schema:"clean_service_interval" json:"cleanServiceInterval,omitempty"`
	MessageConsumerType                string                 `schema:"message_consumer_type" json:"messageConsumerType,omitempty"`
	ErrorReasonEnabled                 bool                   `schema:"error_reason_enabled" json:"errorReasonEnabled,omitempty"`
	CleanServiceBatchChunkSize         int                    `schema:"clean_service_batch_chunk_size" json:"cleanServiceBatchChunkSize,omitempty"`
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
