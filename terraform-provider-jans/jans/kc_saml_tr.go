package jans

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
)

type TrustRelationshipForm struct {
	TrustRelationship TrustRelationship `schema:"trustRelationship" json:"trustRelationship"`
	MetaDataFile      []byte            `schema:"metaDataFile" json:"metaDataFile"`
}

type TrustRelationship struct {
	DN                       string                `schema:"dn" json:"dn"`
	Inum                     string                `schema:"inum" json:"inum"`
	Owner                    string                `schema:"owner" json:"owner"`
	Name                     string                `schema:"name" json:"name"`
	DisplayName              string                `schema:"display_name" json:"displayName"`
	Description              string                `schema:"description" json:"description"`
	RootUrl                  string                `schema:"root_url" json:"rootUrl"`
	Enabled                  bool                  `schema:"enabled" json:"enabled"`
	AlwaysDisplayInConsole   bool                  `schema:"always_display_in_console" json:"alwaysDisplayInConsole"`
	ClientAuthenticatorType  string                `schema:"client_authenticator_type" json:"clientAuthenticatorType"`
	Secret                   string                `schema:"secret" json:"secret"`
	RegisterationAccessToken string                `schema:"registration_access_token" json:"registrationAccessToken"`
	ConsetRequired           bool                  `schema:"consent_required" json:"consentRequired"`
	SPMetaDataSourceType     string                `schema:"sp_meta_data_source_type" json:"spMetaDataSourceType"`
	SamlMetadata             SAMLMetadata          `schema:"saml_metadata" json:"samlMetadata"`
	RedirectUris             []string              `schema:"redirect_uris" json:"redirectUris"`
	SPMetaDataURL            string                `schema:"sp_meta_data_url" json:"spMetaDataURL"`
	MetaLocation             string                `schema:"meta_location" json:"metaLocation"`
	ReleasedAttributes       []string              `schema:"released_attributes" json:"releasedAttributes"`
	SPLogoutURL              string                `schema:"sp_logout_url" json:"spLogoutURL"`
	Status                   string                `schema:"status" json:"status"`
	ValidationStatus         string                `schema:"validation_status" json:"validationStatus"`
	ValidationLog            []string              `schema:"validation_log" json:"validationLog"`
	ProfileConfigurations    ProfileConfigurations `schema:"profile_configurations" json:"profileConfigurations"`
	BaseDn                   string                `schema:"base_dn" json:"baseDn"`
}

type SAMLMetadata struct {
	NameIDPolicyFormat                  string `schema:"name_id_policy_format" json:"nameIDPolicyFormat"`
	EntityId                            string `schema:"entity_id" json:"entityId"`
	SingleLogoutServiceURL              string `schema:"single_logout_service_url" json:"singleLogoutServiceUrl"`
	JansAssertionConsumerServiceGetURL  string `schema:"jans_assertion_consumer_service_get_url" json:"jansAssertionConsumerServiceGetURL"`
	JansAssertionConsumerServicePostURL string `schema:"jans_assertion_consumer_service_post_url" json:"jansAssertionConsumerServicePostURL"`
}

type AdditionalProp struct {
	Name          string `schema:"name" json:"name"`
	SignResponses string `schema:"sign_responses" json:"signResponses"`
}

type ProfileConfigurations struct {
	AddtionalProp1 AdditionalProp `schema:"additional_prop1" json:"additionalProp1"`
	AddtionalProp2 AdditionalProp `schema:"additional_prop2" json:"additionalProp2"`
	AddtionalProp3 AdditionalProp `schema:"additional_prop3" json:"additionalProp3"`
}

func (c *Client) createTRFormData(tr *TrustRelationship, file io.Reader) (map[string]FormField, error) {
	data := map[string]FormField{}

	tr.SPMetaDataSourceType = "manual"
	if file != nil {
		data["assetFile"] = FormField{
			Typ:  "file",
			Data: file,
		}
		tr.SPMetaDataSourceType = "file"
	}

	b, err := json.Marshal(tr)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	r := bytes.NewReader(b)

	data["trustRelationship"] = FormField{
		Typ:  "json",
		Data: r,
	}

	return data, nil
}

func (c *Client) CreateTR(ctx context.Context, tr *TrustRelationship, file io.Reader) (*TrustRelationship, error) {

	data, err := c.createTRFormData(tr, file)
	if err != nil {
		return nil, fmt.Errorf("failed to create form data: %w", err)
	}

	resp := &TrustRelationship{}
	req, err := c.newParams("POST", "/jans-config-api/kc/saml/trust-relationship/upload", resp,
		c.withToken(ctx, "https://jans.io/oauth/config/saml.write"),
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

func (c *Client) UpdateTR(ctx context.Context, tr *TrustRelationship, file io.Reader) (*TrustRelationship, error) {
	data, err := c.createTRFormData(tr, file)
	if err != nil {
		return nil, fmt.Errorf("failed to create form data: %w", err)
	}

	resp := &TrustRelationship{}
	req, err := c.newParams("PUT", "/jans-config-api/kc/saml/trust-relationship/upload", resp,
		c.withToken(ctx, "https://jans.io/oauth/config/saml.write"),
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

func (c *Client) DeleteTR(ctx context.Context, inum string) error {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/saml.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/kc/saml/trust-relationship/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}

func (c *Client) GetTRs(ctx context.Context) ([]TrustRelationship, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/saml.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	resp := []TrustRelationship{}
	if err = c.get(ctx, "/jans-config-api/kc/saml/trust-relationship", token, &resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp, nil
}

func (c *Client) GetTR(ctx context.Context, inum string) (*TrustRelationship, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/saml.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	resp := &TrustRelationship{}
	if err = c.get(ctx, "/jans-config-api/kc/saml/trust-relationship/id/"+inum, token, resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp, nil
}
