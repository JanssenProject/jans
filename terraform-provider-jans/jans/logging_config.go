package jans

import (
        "context"
        "fmt"
)

// LoggingConfiguration represents the configuration of the server loggers.
type LoggingConfiguration struct {
        LoggingLevel                string   `schema:"logging_level" json:"loggingLevel,omitempty"`
        LoggingLayout               string   `schema:"logging_layout" json:"loggingLayout,omitempty"`
        HttpLoggingEnabled          bool     `schema:"http_logging_enabled" json:"httpLoggingEnabled,omitempty"`
        DisableJdkLogger            bool     `schema:"disable_jdk_logger" json:"disableJdkLogger,omitempty"`
        EnabledOAuthAuditLogging    bool     `schema:"enabled_oauth_audit_logging" json:"enabledOAuthAuditLogging,omitempty"`
        ExternalLoggerConfiguration string   `schema:"external_logger_configuration" json:"externalLoggerConfiguration,omitempty"`
        HttpLoggingExcludePaths     []string `schema:"http_logging_exclude_paths" json:"httpLoggingExcludePaths,omitempty"`
}

// GetLoggingConfiguration returns the Logging configuration.
func (c *Client) GetLoggingConfiguration(ctx context.Context) (*LoggingConfiguration, error) {

        scope := "https://jans.io/oauth/config/logging.readonly"
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        ret := &LoggingConfiguration{}

        if err := c.get(ctx, "/jans-config-api/api/v1/logging", token, scope, ret); err != nil {
                return nil, fmt.Errorf("get request failed: %w", err)
        }

        return ret, nil
}

// UpdateLoggingConfiguration updates the Logging configuration.
func (c *Client) UpdateLoggingConfiguration(ctx context.Context, config *LoggingConfiguration) (*LoggingConfiguration, error) {

        scope := "https://jans.io/oauth/config/logging.write"
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        ret := &LoggingConfiguration{}

        if err := c.put(ctx, "/jans-config-api/api/v1/logging", token, scope, config, ret); err != nil {
                return nil, fmt.Errorf("put request failed: %w", err)
        }

        return ret, nil
}
