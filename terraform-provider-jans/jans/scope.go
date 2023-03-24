package jans

import (
	"context"
	"fmt"
)

type ScopeAttribute struct {
	SpontaneousClientScopes     []string `schema:"spontaneous_client_scopes" json:"spontaneousClientScopes,omitempty"`
	ShowInConfigurationEndpoint bool     `schema:"show_in_configuration_endpoint" json:"showInConfigurationEndpoint,omitempty"`
}

type Scope struct {
	Dn                       string            `schema:"dn" json:"dn,omitempty"`
	Inum                     string            `schema:"inum" json:"inum,omitempty"`
	DisplayName              string            `schema:"display_name" json:"displayName,omitempty"`
	Id                       string            `schema:"scope_id" json:"id,omitempty"`
	IconUrl                  string            `schema:"icon_url" json:"iconUrl,omitempty"`
	Description              string            `schema:"description" json:"description,omitempty"`
	ScopeType                string            `schema:"scope_type" json:"scopeType,omitempty"`
	Claims                   []string          `schema:"claims" json:"claims,omitempty"`
	DefaultScope             bool              `schema:"default_scope" json:"defaultScope,omitempty"`
	GroupClaims              bool              `schema:"group_claims" json:"groupClaims,omitempty"`
	DynamicScopeScripts      []string          `schema:"dynamic_scope_scripts" json:"dynamicScopeScripts,omitempty"`
	UmaAuthorizationPolicies []string          `schema:"uma_authorization_policies" json:"umaAuthorizationPolicies,omitempty"`
	Attributes               ScopeAttribute    `schema:"attributes" json:"attributes,omitempty"`
	CreatorId                string            `schema:"creator_id" json:"creatorId,omitempty"`
	CreatorType              string            `schema:"creator_type" json:"creatorType,omitempty"`
	CreationDate             string            `schema:"creation_date" json:"creationDate,omitempty"`
	CreatorAttributes        map[string]string `schema:"creator_attributes" json:"creatorAttributes,omitempty"` // XXX
	UmaType                  bool              `schema:"uma_type" json:"umaType,omitempty"`
	Deletable                bool              `schema:"deletable" json:"deletable,omitempty"`
	ExpirationDate           string            `schema:"expiration_date" json:"expirationDate,omitempty"`
	BaseDn                   string            `schema:"base_dn" json:"baseDn,omitempty"`
	Clients                  []OidcClient      `schema:"clients" json:"clients,omitempty"`
}

// GetScopes returns all currently configured scopes.
func (c *Client) GetScopes(ctx context.Context) ([]Scope, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scopes.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Scopes []Scope `json:"entries"`
	}

	ret := response{}

	queryParams := map[string]string{
		"limit": "100",
	}

	if err := c.get(ctx, "/jans-config-api/api/v1/scopes", token, &ret, queryParams); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Scopes, nil
}

// GetScope returns the scope with the given inum.
func (c *Client) GetScope(ctx context.Context, inum string) (*Scope, error) {

	if inum == "" {
		return nil, fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scopes.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Scope{}

	if err := c.get(ctx, "/jans-config-api/api/v1/scopes/"+inum, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateScope creates a new scope.
func (c *Client) CreateScope(ctx context.Context, scope *Scope) (*Scope, error) {

	if scope == nil {
		return nil, fmt.Errorf("scope is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scopes.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Scope{}

	if err := c.post(ctx, "/jans-config-api/api/v1/scopes", token, scope, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// // UpdateScope updates an already existing scope.
func (c *Client) UpdateScope(ctx context.Context, scope *Scope) (*Scope, error) {

	if scope == nil {
		return nil, fmt.Errorf("scope is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scopes.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	// we can either use PUT on /scope or PATCH on /scope/{inum}

	ret := &Scope{}

	if err := c.put(ctx, "/jans-config-api/api/v1/scopes", token, scope, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteScope deletes an already existing scope.
func (c *Client) DeleteScope(ctx context.Context, inum string) error {

	if inum == "" {
		return fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/scopes.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/scopes/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
