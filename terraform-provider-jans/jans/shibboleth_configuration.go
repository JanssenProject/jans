package jans

import (
	"context"
	"fmt"
)

// ShibbolethIdpConfiguration represents the Shibboleth IDP configuration
type ShibbolethIdpConfiguration struct {
	EntityId                  string                   `schema:"entity_id" json:"entityId"`
	Scope                     string                   `schema:"scope" json:"scope"`
	Enabled                   bool                     `schema:"enabled" json:"enabled"`
	MetadataProviders         []string                 `schema:"metadata_providers" json:"metadataProviders,omitempty"`
	TrustedServiceProviders   []TrustedServiceProvider `schema:"trusted_service_providers" json:"trustedServiceProviders,omitempty"`
	AttributeMappings         []AttributeMapping       `schema:"attribute_mappings" json:"attributeMappings,omitempty"`
	Revision                  int                      `schema:"revision" json:"revision"`
}

// TrustedServiceProvider represents a SAML Service Provider trust relationship
type TrustedServiceProvider struct {
	EntityId           string   `schema:"entity_id" json:"entityId"`
	Name               string   `schema:"name" json:"name"`
	Description        string   `schema:"description" json:"description,omitempty"`
	MetadataUrl        string   `schema:"metadata_url" json:"metadataUrl,omitempty"`
	MetadataXml        string   `schema:"metadata_xml" json:"metadataXml,omitempty"`
	Enabled            bool     `schema:"enabled" json:"enabled"`
	ReleasedAttributes []string `schema:"released_attributes" json:"releasedAttributes,omitempty"`
	NameIdFormat       string   `schema:"name_id_format" json:"nameIdFormat,omitempty"`
}

// AttributeMapping represents a SAML attribute mapping
type AttributeMapping struct {
	Id                string `schema:"id" json:"id"`
	JansAttribute     string `schema:"jans_attribute" json:"jansAttribute"`
	SamlAttribute     string `schema:"saml_attribute" json:"samlAttribute"`
	SamlAttributeOid  string `schema:"saml_attribute_oid" json:"samlAttributeOid,omitempty"`
	FriendlyName      string `schema:"friendly_name" json:"friendlyName,omitempty"`
	NameFormat        string `schema:"name_format" json:"nameFormat,omitempty"`
	Enabled           bool   `schema:"enabled" json:"enabled"`
}

// GetShibbolethConfiguration returns the current Shibboleth IDP configuration
func (c *Client) GetShibbolethConfiguration(ctx context.Context) (*ShibbolethIdpConfiguration, error) {

	scope := "https://jans.io/oauth/config/shibboleth.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &ShibbolethIdpConfiguration{}

	if err := c.get(ctx, "/jans-config-api/shibboleth/config", token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// UpdateShibbolethConfiguration updates the Shibboleth IDP configuration
func (c *Client) UpdateShibbolethConfiguration(ctx context.Context, config *ShibbolethIdpConfiguration) (*ShibbolethIdpConfiguration, error) {

	scope := "https://jans.io/oauth/config/shibboleth.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &ShibbolethIdpConfiguration{}

	if err := c.put(ctx, "/jans-config-api/shibboleth/config", token, scope, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// GetTrustedServiceProviders returns all trusted service providers
func (c *Client) GetTrustedServiceProviders(ctx context.Context) ([]TrustedServiceProvider, error) {

	scope := "https://jans.io/oauth/config/shibboleth.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var ret []TrustedServiceProvider

	if err := c.get(ctx, "/jans-config-api/shibboleth/trust", token, scope, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetTrustedServiceProvider returns a specific trusted service provider by entity ID
func (c *Client) GetTrustedServiceProvider(ctx context.Context, entityId string) (*TrustedServiceProvider, error) {

	scope := "https://jans.io/oauth/config/shibboleth.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &TrustedServiceProvider{}

	if err := c.get(ctx, "/jans-config-api/shibboleth/trust/"+entityId, token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateTrustedServiceProvider creates a new trusted service provider
func (c *Client) CreateTrustedServiceProvider(ctx context.Context, sp *TrustedServiceProvider) (*TrustedServiceProvider, error) {

	scope := "https://jans.io/oauth/config/shibboleth.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &TrustedServiceProvider{}

	if err := c.post(ctx, "/jans-config-api/shibboleth/trust", token, scope, sp, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateTrustedServiceProvider updates an existing trusted service provider
func (c *Client) UpdateTrustedServiceProvider(ctx context.Context, sp *TrustedServiceProvider) (*TrustedServiceProvider, error) {

	scope := "https://jans.io/oauth/config/shibboleth.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &TrustedServiceProvider{}

	if err := c.put(ctx, "/jans-config-api/shibboleth/trust/"+sp.EntityId, token, scope, sp, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteTrustedServiceProvider deletes a trusted service provider
func (c *Client) DeleteTrustedServiceProvider(ctx context.Context, entityId string) error {

	scope := "https://jans.io/oauth/config/shibboleth.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/shibboleth/trust/"+entityId, token, scope); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
