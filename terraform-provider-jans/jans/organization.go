package jans

import (
	"context"
	"fmt"
)

// Organization manages the global organizational settings for the
// Janssen instance.
type Organization struct {
	Dn           string `schema:"dn" json:"dn"`
	DisplayName  string `schema:"display_name" json:"displayName"`
	Description  string `schema:"description" json:"description"`
	Member       string `schema:"member" json:"member"`
	Organization string `schema:"organization" json:"organization"`
	ManagerGroup string `schema:"manager_group" json:"managerGroup"`
	ThemeColor   string `schema:"theme_color" json:"themeColor"`
	ShortName    string `schema:"short_name" json:"shortName"`
	BaseDn       string `schema:"base_dn" json:"baseDn"`

	// Don't exist in backend data model
	// CountryName string `schema:"country_name" json:"countryName"`
	// Status string `schema:"status" json:"status"`
	// CustomMessages []string `schema:"custom_messages" json:"customMessages"`
	// Title             string `schema:"title" json:"title"`
	// JsLogoPath        string `schema:"js_logo_path" json:"jsLogoPath"`
	// JsFaviconPath string `schema:"js_favicon_path" json:"jsFaviconPath"`
}

// GetOrganization returns the current organization configuration.
func (c *Client) GetOrganization(ctx context.Context) (*Organization, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/organization.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Organization{}

	if err := c.get(ctx, "/jans-config-api/api/v1/org", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// // UpdateOrganization updates the organization configuration.
func (c *Client) UpdateOrganization(ctx context.Context, orga *Organization) (*Organization, error) {

	if orga == nil {
		return nil, fmt.Errorf("organization is nil")
	}

	// get the original so we don't have to send all fields
	// as patch requests
	orig, err := c.GetOrganization(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get organization: %w", err)
	}

	patches, err := createPatches(orga, orig)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/organization.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/org", token, patches); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return c.GetOrganization(ctx)
}
