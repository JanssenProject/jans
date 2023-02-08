package jans

import (
	"context"
	"fmt"
)

// UMAResource represents an UMA resources.
type UMAResource struct {
	Dn              string   `schema:"dn" json:"dn,omitempty"`
	Inum            string   `schema:"inum" json:"inum,omitempty"`
	ID              string   `schema:"id" json:"id,omitempty"`
	Name            string   `schema:"name" json:"name,omitempty"`
	IconURI         string   `schema:"icon_uri" json:"iconURI,omitempty"`
	Scopes          []string `schema:"scopes" json:"scopes,omitempty"`
	ScopeExpression string   `schema:"scope_expression" json:"scopeExpression,omitempty"`
	Clients         []string `schema:"clients" json:"clients,omitempty"`
	Resources       []string `schema:"resources" json:"resources,omitempty"`
	Creator         string   `schema:"creator" json:"creator,omitempty"`
	Description     string   `schema:"description" json:"description,omitempty"`
	Type            string   `schema:"type" json:"type,omitempty"`
	CreationDate    string   `schema:"creation_date" json:"creationDate,omitempty"`
	ExpirationDate  string   `schema:"expiration_date" json:"expirationDate,omitempty"`
	Deletable       bool     `schema:"deletable" json:"deletable,omitempty"`
}

// GetUMAResources returns all UMA resources defined in the system.
func (c *Client) GetUMAResources(ctx context.Context) ([]UMAResource, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/uma/resources.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Resources []UMAResource `json:"entries"`
	}
	ret := response{}

	if err := c.get(ctx, "/jans-config-api/api/v1/uma/resources", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Resources, nil
}

// GetUMAResource returns a single UMA resource with the given id.
func (c *Client) GetUMAResource(ctx context.Context, id string) (*UMAResource, error) {

	if id == "" {
		return nil, fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/uma/resources.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &UMAResource{}

	if err := c.get(ctx, "/jans-config-api/api/v1/uma/resources/"+id, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateUMAResource creates a new UMA resource.
func (c *Client) CreateUMAResource(ctx context.Context, resource *UMAResource) (*UMAResource, error) {

	if resource == nil {
		return nil, fmt.Errorf("resource is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/uma/resources.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &UMAResource{}

	if err := c.post(ctx, "/jans-config-api/api/v1/uma/resources", token, resource, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateUMAResource updates an existing UMA resource.
func (c *Client) UpdateUMAResource(ctx context.Context, resource *UMAResource) (*UMAResource, error) {

	if resource == nil {
		return nil, fmt.Errorf("resource is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/uma/resources.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &UMAResource{}

	if err := c.put(ctx, "/jans-config-api/api/v1/uma/resources", token, resource, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteUMAResource deletes the existing UMA resource with the provided ID.
func (c *Client) DeleteUMAResource(ctx context.Context, id string) error {

	if id == "" {
		return fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/uma/resources.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/uma/resources/"+id, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
