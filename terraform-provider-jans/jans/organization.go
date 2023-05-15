package jans

import (
	"context"
	"fmt"
)

// Organization manages the global organizational settings for the
// Janssen instance.
type Organization struct {
	Dn             string   `schema:"dn" json:"dn"`
	DisplayName    string   `schema:"display_name" json:"displayName"`
	Description    string   `schema:"description" json:"description"`
	Member         string   `schema:"member" json:"member"`
	Organization   string   `schema:"organization" json:"organization"`
	ManagerGroup   string   `schema:"manager_group" json:"managerGroup"`
	ThemeColor     string   `schema:"theme_color" json:"themeColor"`
	ShortName      string   `schema:"short_name" json:"shortName"`
	CountryName    string   `schema:"country_name" json:"countryName"`
	Status         string   `schema:"status" json:"status"`
	CustomMessages []string `schema:"custom_messages" json:"customMessages"`
	Title          string   `schema:"title" json:"title"`
	JsLogoPath     string   `schema:"js_logo_path" json:"jsLogoPath"`
	JsFaviconPath  string   `schema:"js_favicon_path" json:"jsFaviconPath"`
	BaseDn         string   `schema:"base_dn" json:"baseDn"`
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

// PatchOrganization updates the organization configuration with the
// provided set of patches.
func (c *Client) PatchOrganization(ctx context.Context, patches []PatchRequest) (*Organization, error) {

	if len(patches) == 0 {
		return c.GetOrganization(ctx)
	}

	orig, err := c.GetOrganization(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get fido2 configuration: %w", err)
	}

	updates, err := createPatchesDiff(orig, patches)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	if len(updates) == 0 {
		return c.GetOrganization(ctx)
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/organization.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/org", token, updates); err != nil {
		return nil, fmt.Errorf("patch request failed: %w", err)
	}

	return c.GetOrganization(ctx)
}
