package jans

import (
	"context"
	"fmt"
)

type AuditLogConf struct {
	Enabled          bool     `schema:"enabled" json:"enabled"`
	IgnoreHttpMethod []string `schema:"ignore_http_method" json:"ignoreHttpMethod"`
	HeaderAttributes []string `schema:"header_attributes" json:"headerAttributes"`
}

type DataFormatConversionConf struct {
	Enabled          bool     `schema:"enabled" json:"enabled"`
	IgnoreHttpMethod []string `schema:"ignore_http_method" json:"ignoreHttpMethod"`
}

type AgamaConfiguration struct {
	MandatoryAttributes []string `schema:"mandatory_attributes" json:"mandatoryAttributes"`
	OptionalAttributes  []string `schema:"optional_attributes" json:"optionalAttributes"`
}

type AssetDirMapping struct {
	Directory   string   `schema:"directory" json:"directory"`
	Type        []string `schema:"type" json:"type"`
	Description string   `schema:"description" json:"description"`
}

type AssetMgtConfiguration struct {
	AssetMgtEnabled                bool              `schema:"asset_mgt_enabled" json:"assetMgtEnabled"`
	AssetServerUploadEnabled       bool              `schema:"asset_server_upload_enabled" json:"assetServerUploadEnabled"`
	FileExtensionValidationEnabled bool              `schema:"file_extension_validation_enabled" json:"fileExtensionValidationEnabled"`
	ModuleNameValidationEnabled    bool              `schema:"module_name_validation_enabled" json:"moduleNameValidationEnabled"`
	AssetDirMappings               []AssetDirMapping `schema:"asset_dir_mappings" json:"assetDirMapping"`
}

// PersistenceConfiguration represents the persistence configuration
// of the Janssen server.
type ApiAppConfiguration struct {
	ConfigOauthEnabled               bool                      `schema:"config_oauth_enabled" json:"configOauthEnabled"`
	DisableLoggerTimer               bool                      `schema:"disable_logger_timer" json:"disableLoggerTimer"`
	DisableAuditLogger               bool                      `schema:"disable_audit_logger" json:"disableAuditLogger"`
	CustomAttributeValidationEnabled bool                      `schema:"custom_attribute_validation_enabled" json:"customAttributeValidationEnabled"`
	ArcValidationEnabled             bool                      `schema:"acr_validation_enabled" json:"acrValidationEnabled"`
	ApiApprovedIssuer                []string                  `schema:"api_approved_issuer" json:"apiApprovedIssuer"`
	ApiProtectionType                string                    `schema:"api_protection_type" json:"apiProtectionType"`
	ApiClientId                      string                    `schema:"api_client_id" json:"apiClientId"`
	ApiClientPassword                string                    `schema:"api_client_password" json:"apiClientPassword"`
	EndpointInjectionEnabled         bool                      `schema:"endpoint_injection_enabled" json:"endpointInjectionEnabled"`
	AuthIssuerUrl                    string                    `schema:"auth_issuer_url" json:"authIssuerUrl"`
	AuthOpenidConfigurationUrl       string                    `schema:"auth_openid_configuration_url" json:"authOpenidConfigurationUrl"`
	AuthOpenidIntrospectionUrl       string                    `schema:"auth_openid_introspection_url" json:"authOpenidIntrospectionUrl"`
	AuthOpenidTokenUrl               string                    `schema:"auth_openid_token_url" json:"authOpenidTokenUrl"`
	AuthOpenidRevokeUrl              string                    `schema:"auth_openid_revoke_url" json:"authOpenidRevokeUrl"`
	ExclusiveAuthScopes              []string                  `schema:"exclusive_auth_scopes" json:"exclusiveAuthScopes"`
	CorsConfigurationFilters         []CorsConfigurationFilter `schema:"cors_configuration_filters" json:"corsConfigurationFilters"`
	LoggingLevel                     string                    `schema:"logging_level" json:"loggingLevel"`
	LoggingLayout                    string                    `schema:"logging_layout" json:"loggingLayout"`
	ExternalLoggerConfiguration      string                    `schema:"external_logger_configuration" json:"externalLoggerConfiguration"`
	DisableJdkLogger                 bool                      `schema:"disable_jdk_logger" json:"disableJdkLogger"`
	MaxCount                         int                       `schema:"max_count" json:"maxCount"`
	UserExclusionAttributes          []string                  `schema:"user_exclusion_attributes" json:"userExclusionAttributes"`
	UserMandatoryAttributes          []string                  `schema:"user_mandatory_attributes" json:"userMandatoryAttributes"`
	AgamaConfiguration               AgamaConfiguration        `schema:"agama_configuration" json:"agamaConfiguration"`
	AuditLogConf                     AuditLogConf              `schema:"audit_log_conf" json:"auditLogConf"`
	DataFormatConversionConf         DataFormatConversionConf  `schema:"data_format_conversion_conf" json:"dataFormatConversionConf"`
	Plugins                          []PluginConf              `schema:"plugins" json:"plugins"`
	AssetMgtConfiguration            AssetMgtConfiguration     `schema:"asset_mgt_configuration" json:"assetMgtConfiguration"`
}

// GetApiAppConfiguration returns the current API configuration.
func (c *Client) GetApiAppConfiguration(ctx context.Context) (*ApiAppConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/properties.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := ApiAppConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/api-config", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return &ret, nil
}

// PatchApiAppConfiguration  uses the provided list of patch requests to update
// the Janssen api configuration properties.
func (c *Client) PatchApiAppConfiguration(ctx context.Context, patches []PatchRequest) (*ApiAppConfiguration, error) {

	if len(patches) == 0 {
		return c.GetApiAppConfiguration(ctx)
	}

	orig, err := c.GetApiAppConfiguration(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get app configuration: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(updates) == 0 {
		return c.GetApiAppConfiguration(ctx)
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/properties.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/api-config", token, updates); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetApiAppConfiguration(ctx)
}
