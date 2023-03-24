package jans

import (
	"context"
	"fmt"
)

type CustomUser struct {
	Dn                  string            `schema:"dn" json:"dn,omitempty"`
	BaseDn              string            `schema:"base_dn" json:"baseDn,omitempty"`
	JansStatus          string            `schema:"jans_status" json:"jansStatus,omitempty"`
	UserID              string            `schema:"user_id" json:"userId,omitempty"`
	CreatedAt           string            `schema:"created_at" json:"createdAt,omitempty"`
	UpdatedAt           string            `schema:"updated_at" json:"updatedAt,omitempty"`
	OxAuthPersistentJwt []string          `schema:"ox_auth_persistent_jwt" json:"oxAuthPersistentJwt,omitempty"`
	CustomAttributes    []CustomAttribute `schema:"custom_attributes" json:"customAttributes,omitempty"`
	CustomObjectClasses []string          `schema:"custom_object_classes" json:"customObjectClasses,omitempty"`
	Mail                string            `schema:"mail" json:"mail,omitempty"`
	DisplayName         string            `schema:"display_name" json:"displayName,omitempty"`
	GivenName           string            `schema:"given_name" json:"givenName,omitempty"`
	UserPassword        string            `schema:"user_password" json:"userPassword,omitempty"`
	Inum                string            `schema:"inum" json:"inum,omitempty"`
}

// GetCustomUsers returns all currently configured custom users.
func (c *Client) GetCustomUsers(ctx context.Context) ([]CustomUser, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/user.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Start             int          `json:"start"`
		TotalEntriesCount int          `json:"totalEntriesCount"`
		EntriesCount      int          `json:"entriesCount"`
		Entries           []CustomUser `json:"entries"`
	}

	ret := response{}

	if err := c.get(ctx, "/jans-config-api/mgt/configuser", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Entries, nil
}

// GetCustomUser returns the custom user with the given inum.
func (c *Client) GetCustomUser(ctx context.Context, inum string) (*CustomUser, error) {

	if inum == "" {
		return nil, fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/user.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &CustomUser{}

	if err := c.get(ctx, "/jans-config-api/mgt/configuser/"+inum, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateCustomUser creates a new custom user.
func (c *Client) CreateCustomUser(ctx context.Context, user *CustomUser) (*CustomUser, error) {

	if user == nil {
		return nil, fmt.Errorf("user is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/user.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &CustomUser{}

	if err := c.post(ctx, "/jans-config-api/mgt/configuser", token, user, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// // UpdateCustomUser updates an already existing custom user.
func (c *Client) UpdateCustomUser(ctx context.Context, user *CustomUser) (*CustomUser, error) {

	if user == nil {
		return nil, fmt.Errorf("user is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/user.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	// we can either use PUT on /configuser or PATCH on /configuser/{inum}

	ret := &CustomUser{}

	if err := c.put(ctx, "/jans-config-api/mgt/configuser", token, user, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteCustomUser deletes an already existing custom user.
func (c *Client) DeleteCustomUser(ctx context.Context, inum string) error {

	if inum == "" {
		return fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/user.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/mgt/configuser/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
