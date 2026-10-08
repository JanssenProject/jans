package jans

import (
	"context"
	"fmt"
)

// GluuLdapConfiguration describes a single LDAP source/target/inum connection
// used by the Jans Link plugin.
type GluuLdapConfiguration struct {
	ConfigId         string   `schema:"config_id" json:"configId"`
	BindDN           string   `schema:"bind_dn" json:"bindDN"`
	BindPassword     string   `schema:"bind_password" json:"bindPassword"`
	Servers          []string `schema:"servers" json:"servers,omitempty"`
	MaxConnections   int      `schema:"max_connections" json:"maxConnections"`
	UseSSL           bool     `schema:"use_ssl" json:"useSSL"`
	BaseDNs          []string `schema:"base_dns" json:"baseDNs,omitempty"`
	PrimaryKey       string   `schema:"primary_key" json:"primaryKey"`
	LocalPrimaryKey  string   `schema:"local_primary_key" json:"localPrimaryKey"`
	UseAnonymousBind bool     `schema:"use_anonymous_bind" json:"useAnonymousBind"`
	Enabled          bool     `schema:"enabled" json:"enabled"`
	Version          int      `schema:"version" json:"version"`
	Level            int      `schema:"level" json:"level"`
}

// LinkAttributeMapping maps a source attribute to a destination attribute.
type LinkAttributeMapping struct {
	Source      string `schema:"source" json:"source"`
	Destination string `schema:"destination" json:"destination"`
}

// LinkConfiguration is the application configuration of the Jans Link plugin.
type LinkConfiguration struct {
	SourceConfigs                      []GluuLdapConfiguration `schema:"source_configs" json:"sourceConfigs,omitempty"`
	InumConfig                         GluuLdapConfiguration   `schema:"inum_config" json:"inumConfig,omitempty"`
	TargetConfig                       GluuLdapConfiguration   `schema:"target_config" json:"targetConfig,omitempty"`
	LdapSearchSizeLimit                int                     `schema:"ldap_search_size_limit" json:"ldapSearchSizeLimit"`
	KeyAttributes                      []string                `schema:"key_attributes" json:"keyAttributes,omitempty"`
	KeyObjectClasses                   []string                `schema:"key_object_classes" json:"keyObjectClasses,omitempty"`
	SourceAttributes                   []string                `schema:"source_attributes" json:"sourceAttributes,omitempty"`
	CustomLdapFilter                   string                  `schema:"custom_ldap_filter" json:"customLdapFilter"`
	UpdateMethod                       string                  `schema:"update_method" json:"updateMethod"`
	DefaultInumServer                  bool                    `schema:"default_inum_server" json:"defaultInumServer"`
	KeepExternalPerson                 bool                    `schema:"keep_external_person" json:"keepExternalPerson"`
	UseSearchLimit                     bool                    `schema:"use_search_limit" json:"useSearchLimit"`
	AttributeMapping                   []LinkAttributeMapping  `schema:"attribute_mapping" json:"attributeMapping,omitempty"`
	SnapshotFolder                     string                  `schema:"snapshot_folder" json:"snapshotFolder"`
	SnapshotMaxCount                   int                     `schema:"snapshot_max_count" json:"snapshotMaxCount"`
	BaseDN                             string                  `schema:"base_dn" json:"baseDN"`
	PersonObjectClassTypes             []string                `schema:"person_object_class_types" json:"personObjectClassTypes,omitempty"`
	PersonCustomObjectClass            string                  `schema:"person_custom_object_class" json:"personCustomObjectClass"`
	ContactObjectClassTypes            []string                `schema:"contact_object_class_types" json:"contactObjectClassTypes,omitempty"`
	AllowPersonModification            bool                    `schema:"allow_person_modification" json:"allowPersonModification"`
	SupportedUserStatus                []string                `schema:"supported_user_status" json:"supportedUserStatus,omitempty"`
	LoggingLevel                       string                  `schema:"logging_level" json:"loggingLevel"`
	LoggingLayout                      string                  `schema:"logging_layout" json:"loggingLayout"`
	ExternalLoggerConfiguration        string                  `schema:"external_logger_configuration" json:"externalLoggerConfiguration"`
	MetricReporterInterval             int                     `schema:"metric_reporter_interval" json:"metricReporterInterval"`
	MetricReporterKeepDataDays         int                     `schema:"metric_reporter_keep_data_days" json:"metricReporterKeepDataDays"`
	MetricReporterEnabled              bool                    `schema:"metric_reporter_enabled" json:"metricReporterEnabled"`
	DisableJdkLogger                   bool                    `schema:"disable_jdk_logger" json:"disableJdkLogger"`
	DisableExternalLoggerConfiguration bool                    `schema:"disable_external_logger_configuration" json:"disableExternalLoggerConfiguration"`
	CleanServiceInterval               int                     `schema:"clean_service_interval" json:"cleanServiceInterval"`
	LinkEnabled                        bool                    `schema:"link_enabled" json:"linkEnabled"`
	ServerIpAddress                    string                  `schema:"server_ip_address" json:"serverIpAddress"`
	PollingInterval                    string                  `schema:"polling_interval" json:"pollingInterval"`
	LastUpdate                         string                  `schema:"last_update" json:"lastUpdate"`
	LastUpdateCount                    string                  `schema:"last_update_count" json:"lastUpdateCount"`
	ProblemCount                       string                  `schema:"problem_count" json:"problemCount"`
	UseLocalCache                      bool                    `schema:"use_local_cache" json:"useLocalCache"`
}

// GetLinkConfiguration returns the current Jans Link plugin configuration.
func (c *Client) GetLinkConfiguration(ctx context.Context) (*LinkConfiguration, error) {

	scope := "https://jans.io/oauth/config/jans-link.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &LinkConfiguration{}

	if err := c.get(ctx, "/jans-config-api/jans-link/link-config", token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// UpdateLinkConfiguration updates the Jans Link plugin configuration. The update
// is a full replace (PUT), so callers must send a fully populated configuration.
func (c *Client) UpdateLinkConfiguration(ctx context.Context, config *LinkConfiguration) (*LinkConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	scope := "https://jans.io/oauth/config/jans-link.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &LinkConfiguration{}

	if err := c.put(ctx, "/jans-config-api/jans-link/link-config", token, scope, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}
