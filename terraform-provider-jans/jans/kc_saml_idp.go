package jans

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
)

type IdentityProviderPagedResult struct {
	Start             int                `json:"start"`
	TotalEntriesCount int                `json:"total_entries_count"`
	EntriesCount      int                `json:"entries_count"`
	Entries           []IdentityProvider `json:"entries"`
}

type BrokerIdentityProviderForm struct {
	IdentityProvider *IdentityProvider `schema:"identity_provider" json:"identityProvider"`
	MetaDataFile     []byte            `schema:"metadata_file" json:"metadataFile"`
}

type IdentityProvider struct {
	DN                        string   `schema:"dn" json:"dn"`
	Inum                      string   `schema:"inum" json:"inum"`
	CreatorId                 string   `schema:"creator_id" json:"creatorId"`
	Name                      string   `schema:"name" json:"name"`
	DisplayName               string   `schema:"display_name" json:"displayName"`
	Description               string   `schema:"description" json:"description"`
	Realm                     string   `schema:"realm" json:"realm"`
	Enabled                   bool     `schema:"enabled" json:"enabled"`
	SigningCertificate        string   `schema:"signing_certificate" json:"signingCertificate"`
	ValidateSignature         string   `schema:"validate_signature" json:"validateSignature"`
	SingleLogoutServiceUrl    string   `schema:"single_logout_service_url" json:"singleLogoutServiceUrl"`
	NameIDPolicyFormat        string   `schema:"name_id_policy_format" json:"nameIDPolicyFormat"`
	PrincipalAttribute        string   `schema:"principal_attribute" json:"principalAttribute"`
	PrincipalType             string   `schema:"principal_type" json:"principalType"`
	IdpEntityId               string   `schema:"idp_entity_id" json:"idpEntityId"`
	SingleSignOnServiceUrl    string   `schema:"single_sign_on_service_url" json:"singleSignOnServiceUrl"`
	EncryptionPublicKey       string   `schema:"encryption_public_key" json:"encryptionPublicKey"`
	ProviderId                string   `schema:"provider_id" json:"providerId"`
	TrustEmail                bool     `schema:"trust_email" json:"trustEmail"`
	StoreToken                bool     `schema:"store_token" json:"storeToken"`
	AddReadTokenRoleOnCreate  bool     `schema:"add_read_token_role_on_create" json:"addReadTokenRoleOnCreate"`
	AuthenticateByDefault     bool     `schema:"authenticate_by_default" json:"authenticateByDefault"`
	LinkOnly                  bool     `schema:"link_only" json:"linkOnly"`
	FirstBrokerLoginFlowAlias string   `schema:"first_broker_login_flow_alias" json:"firstBrokerLoginFlowAlias"`
	PostBrokerLoginFlowAlias  string   `schema:"post_broker_login_flow_alias" json:"postBrokerLoginFlowAlias"`
	SpMetaDataURL             string   `schema:"sp_meta_data_url" json:"spMetaDataURL"`
	SpMetaDataLocation        string   `schema:"sp_meta_data_location" json:"spMetaDataLocation"`
	IdpMetaDataURL            string   `schema:"idp_meta_data_url" json:"idpMetaDataURL"`
	IdpMetaDataLocation       string   `schema:"idp_meta_data_location" json:"idpMetaDataLocation"`
	Status                    string   `schema:"status" json:"status"`
	ValidationStatus          string   `schema:"validation_status" json:"validationStatus"`
	ValidationLog             []string `schema:"validation_log" json:"validationLog"`
	BaseDn                    string   `schema:"base_dn" json:"baseDn"`
	ValidUntil                string   `schema:"valid_until" json:"validUntil"`
	CacheDuration             string   `schema:"cache_duration" json:"cacheDuration"`
}

func (c *Client) createIDPFormData(idp *IdentityProvider, file io.Reader) (map[string]FormField, error) {
	data := map[string]FormField{}

	if file != nil {
		data["metaDataFile"] = FormField{
			Typ:  "file",
			Data: file,
		}
	}

	b, err := json.Marshal(idp)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	r := bytes.NewReader(b)

	data["identityProvider"] = FormField{
		Typ:  "json",
		Data: r,
	}

	return data, nil
}

func (c *Client) CreateIDP(ctx context.Context, idp *IdentityProvider, file io.Reader) (*IdentityProvider, error) {

	data, err := c.createIDPFormData(idp, file)
	if err != nil {
		return nil, fmt.Errorf("failed to create form data: %w", err)
	}

	resp := &IdentityProvider{}
	req, err := c.newParams("POST", "/jans-config-api/kc/saml/idp/upload", resp,
		c.withToken(ctx, "https://jans.io/idp/saml.write"),
		c.withFormData(data),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	if err := c.request(ctx, *req); err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) UpdateIDP(ctx context.Context, idp *IdentityProvider, file io.Reader) (*IdentityProvider, error) {

	data, err := c.createIDPFormData(idp, file)
	if err != nil {
		return nil, fmt.Errorf("failed to create form data: %w", err)
	}

	resp := &IdentityProvider{}
	req, err := c.newParams("PUT", "/jans-config-api/kc/saml/idp/upload", resp,
		c.withToken(ctx, "https://jans.io/idp/saml.write"),
		c.withFormData(data),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	if err := c.request(ctx, *req); err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) GetIDP(ctx context.Context, inum string) (*IdentityProvider, error) {

	token, err := c.getToken(ctx, "https://jans.io/idp/saml.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	resp := &IdentityProvider{}

	if err = c.get(ctx, "/jans-config-api/kc/saml/idp/"+inum, token, resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) DeleteIDP(ctx context.Context, inum string) error {

	token, err := c.getToken(ctx, "https://jans.io/idp/saml.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/kc/saml/idp/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
