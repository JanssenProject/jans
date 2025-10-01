package jans

import (
	"context"
	"fmt"
)

type KCSAMLConfiguration struct {
	ApplicationName                string   `schema:"application_name" json:"applicationName"`
	SamlTrustRelationshipDn        string   `schema:"saml_trust_relationship_dn" json:"samlTrustRelationshipDn"`
	TrustIdpDn                     string   `schema:"trusted_idp_dn" json:"trustedIdpDn"`
	Enabled                        bool     `schema:"enabled" json:"enabled"`
	SelectedIdp                    string   `schema:"selected_idp" json:"selectedIdp"`
	ServerUrl                      string   `schema:"server_url" json:"serverUrl"`
	Realm                          string   `schema:"realm" json:"realm"`
	ClientId                       string   `schema:"client_id" json:"clientId"`
	ClientSecret                   string   `schema:"client_secret" json:"clientSecret"`
	GrantType                      string   `schema:"grant_type" json:"grantType"`
	Scope                          string   `schema:"scope" json:"scope"`
	Username                       string   `schema:"username" json:"username"`
	Password                       string   `schema:"password" json:"password"`
	SpMetadataUrl                  string   `schema:"sp_metadata_url" json:"spMetadataUrl"`
	TokenUrl                       string   `schema:"token_url" json:"tokenUrl"`
	IdpUrl                         string   `schema:"idp_url" json:"idpUrl"`
	ExtIDPTokenUrl                 string   `schema:"ext_idp_token_url" json:"extIDPTokenUrl"`
	ExtIDPRedirectUrl              string   `schema:"ext_idp_redirect_url" json:"extIDPRedirectUrl"`
	IdpMetadataImportUrl           string   `schema:"idp_metadata_import_url" json:"idpMetadataImportUrl"`
	IdpRootDir                     string   `schema:"idp_root_dir" json:"idpRootDir"`
	IdpMetadataDir                 string   `schema:"idp_metadata_dir" json:"idpMetadataDir"`
	IdpMetadataTempDir             string   `schema:"idp_metadata_temp_dir" json:"idpMetadataTempDir"`
	IdpMetadataFile                string   `schema:"idp_metadata_file" json:"idpMetadataFile"`
	SpMetadataDir                  string   `schema:"sp_metadata_dir" json:"spMetadataDir"`
	SpMetadataTempDir              string   `schema:"sp_metadata_temp_dir" json:"spMetadataTempDir"`
	SpMetadataFile                 string   `schema:"sp_metadata_file" json:"spMetadataFile"`
	IgnoreValidation               bool     `schema:"ignore_validation" json:"ignoreValidation"`
	SetConfigDefaultValue          bool     `schema:"set_config_default_value" json:"setConfigDefaultValue"`
	IdpMetadataMandatoryAttributes []string `schema:"idp_metadata_mandatory_attributes" json:"idpMetadataMandatoryAttributes"`
	KcAttributes                   []string `schema:"kc_attributes" json:"kcAttributes"`
	KcSamlConfig                   []string `schema:"kc_saml_config" json:"kcSamlConfig"`
}

func (c *Client) CreateKCSAMLConfiguration(ctx context.Context, config *KCSAMLConfiguration) (*KCSAMLConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/saml-config.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &KCSAMLConfiguration{}

	if err := c.put(ctx, "/jans-config-api/kc/samlConfig", token, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

func (c *Client) GetKCSAMLConfiguration(ctx context.Context) (*KCSAMLConfiguration, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/saml-config.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &KCSAMLConfiguration{}

	if err := c.get(ctx, "/jans-config-api/kc/samlConfig", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

func (c *Client) PatchKCSAMLConfiguration(ctx context.Context, patches []PatchRequest) (*KCSAMLConfiguration, error) {

	if len(patches) == 0 {
		return c.GetKCSAMLConfiguration(ctx)
	}

	orig, err := c.GetKCSAMLConfiguration(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get fido2 configuration: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/saml-config.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/kc/samlConfig", token, updates); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return c.GetKCSAMLConfiguration(ctx)
}
