package jans

import (
	"context"
	"fmt"
)

// KeyValuePair is a generic string key/value entry used by several AdminUI
// configuration structures.
type KeyValuePair struct {
	Key   string `schema:"key" json:"key,omitempty"`
	Value string `schema:"value" json:"value,omitempty"`
}

// AdminUIConfiguration is the AdminUI application configuration (AppConfigResponse).
type AdminUIConfiguration struct {
	AuthServerHost        string         `schema:"auth_server_host" json:"authServerHost,omitempty"`
	AuthzBaseUrl          string         `schema:"authz_base_url" json:"authzBaseUrl,omitempty"`
	ClientId              string         `schema:"client_id" json:"clientId,omitempty"`
	ResponseType          string         `schema:"response_type" json:"responseType,omitempty"`
	Scope                 string         `schema:"scope" json:"scope,omitempty"`
	RedirectUrl           string         `schema:"redirect_url" json:"redirectUrl,omitempty"`
	AcrValues             string         `schema:"acr_values" json:"acrValues,omitempty"`
	FrontChannelLogoutUrl string         `schema:"front_channel_logout_url" json:"frontChannelLogoutUrl,omitempty"`
	PostLogoutRedirectUri string         `schema:"post_logout_redirect_uri" json:"postLogoutRedirectUri,omitempty"`
	EndSessionEndpoint    string         `schema:"end_session_endpoint" json:"endSessionEndpoint,omitempty"`
	SessionTimeoutInMins  int            `schema:"session_timeout_in_mins" json:"sessionTimeoutInMins,omitempty"`
	AllowSmtpKeystoreEdit bool           `schema:"allow_smtp_keystore_edit" json:"allowSmtpKeystoreEdit,omitempty"`
	AdditionalParameters  []KeyValuePair `schema:"additional_parameters" json:"additionalParameters,omitempty"`
	CedarlingLogType      string         `schema:"cedarling_log_type" json:"cedarlingLogType,omitempty"`
}

// GetAdminUIConfiguration returns the current AdminUI application configuration.
func (c *Client) GetAdminUIConfiguration(ctx context.Context) (*AdminUIConfiguration, error) {

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/properties.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &AdminUIConfiguration{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/config", token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// UpdateAdminUIConfiguration updates the AdminUI application configuration. The
// update is a full replace (PUT).
func (c *Client) UpdateAdminUIConfiguration(ctx context.Context, config *AdminUIConfiguration) (*AdminUIConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/properties.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &AdminUIConfiguration{}

	if err := c.put(ctx, "/jans-config-api/admin-ui/config", token, scope, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}
