package jans

import (
	"context"
	"errors"
	"fmt"
)

// AttributeValidation defines a validation that will be applied on the
// relating attribute.
type AttributeValidation struct {
	Regexp    string `schema:"regexp" json:"regexp,omitempty"`
	MinLength int    `schema:"min_length" json:"minLength,omitempty"`
	MaxLength int    `schema:"max_length" json:"maxLength,omitempty"`
}

// Attribute represents a single Gluu attribute.
type Attribute struct {
	Dn                     string               `schema:"dn" json:"dn,omitempty"`
	BaseDn                 string               `schema:"base_dn" json:"baseDn,omitempty"`
	Selected               bool                 `schema:"selected" json:"selected,omitempty"`
	Inum                   string               `schema:"inum" json:"inum,omitempty"`
	SourceAttribute        string               `schema:"source_attribute" json:"sourceAttribute,omitempty"`
	NameIdType             string               `schema:"name_id_type" json:"nameIdType,omitempty"`
	Name                   string               `schema:"name" json:"name,omitempty"`
	DisplayName            string               `schema:"display_name" json:"displayName,omitempty"`
	Origin                 string               `schema:"origin" json:"origin,omitempty"`
	DataType               string               `schema:"data_type" json:"dataType,omitempty"`
	Description            string               `schema:"description" json:"description,omitempty"`
	Status                 string               `schema:"status" json:"status,omitempty"`
	Lifetime               string               `schema:"lifetime" json:"lifetime,omitempty"`
	Salt                   string               `schema:"salt" json:"salt,omitempty"`
	EditType               []string             `schema:"edit_type" json:"editType,omitempty"`
	ViewType               []string             `schema:"view_type" json:"viewType,omitempty"`
	UsageType              []string             `schema:"usage_type" json:"usageType,omitempty"`
	ClaimName              string               `schema:"claim_name" json:"claimName,omitempty"`
	SeeAlso                string               `schema:"see_also" json:"seeAlso,omitempty"`
	Saml1Uri               string               `schema:"saml1_uri" json:"saml1Uri,omitempty"`
	Saml2Uri               string               `schema:"saml2_uri" json:"saml2Uri,omitempty"`
	Urn                    string               `schema:"urn" json:"urn,omitempty"`
	ScimCustomAttr         bool                 `schema:"scim_custom_attr" json:"scimCustomAttr,omitempty"`
	OxMultiValuedAttribute bool                 `schema:"ox_multi_valued_attribute" json:"oxMultiValuedAttribute,omitempty"`
	AttributeValidation    *AttributeValidation `schema:"attribute_validation" json:"attributeValidation,omitempty"`
	Tooltip                string               `schema:"tooltip" json:"tooltip,omitempty"`
	JansHideOnDiscovery    bool                 `schema:"jans_hide_on_discovery" json:"jansHideOnDiscovery,omitempty"`
	Custom                 bool                 `schema:"custom" json:"custom,omitempty"`
	Required               bool                 `schema:"required" json:"requred,omitempty"`
	AdminCanAccess         bool                 `schema:"admin_can_access" json:"adminCanAccess,omitempty"`
	AdminCanView           bool                 `schema:"admin_can_view" json:"adminCanView,omitempty"`
	AdminCanEdit           bool                 `schema:"admin_can_edit" json:"adminCanEdit,omitempty"`
	UserCanAccess          bool                 `schema:"user_can_access" json:"userCanAccess,omitempty"`
	UserCanView            bool                 `schema:"user_can_view" json:"userCanView,omitempty"`
	UserCanEdit            bool                 `schema:"user_can_edit" json:"userCanEdit,omitempty"`
	WhitePagesCanView      bool                 `schema:"white_pages_can_view" json:"whitePagesCanView,omitempty"`
}

// GetAttributes returns a list of all Gluu attributes currently configured
// in the server.
func (c *Client) GetAttributes(ctx context.Context) ([]Attribute, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/attributes.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Data       []Attribute `json:"data"`
		Count      int         `json:"entriesCount"`
		TotalItems int         `json:"totalItems"`
	}

	resp := response{}

	if err := c.get(ctx, "/jans-config-api/api/v1/attributes", token, &resp); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return resp.Data, nil
}

// GetAttribute returns a single attribute, identified by its inum.
func (c *Client) GetAttribute(ctx context.Context, inum string) (*Attribute, error) {

	if inum == "" {
		return nil, fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/attributes.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Attribute{}

	err = c.get(ctx, "/jans-config-api/api/v1/attributes/"+inum, token, ret)
	if err != nil && !errors.Is(err, ErrorNotFound) {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateAttribute creates a new attribute.
func (c *Client) CreateAttribute(ctx context.Context, attr *Attribute) (*Attribute, error) {

	if attr == nil {
		return nil, fmt.Errorf("attribute is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/attributes.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Attribute{}

	if err := c.post(ctx, "/jans-config-api/api/v1/attributes/", token, attr, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateAttribute updates an existing attribute.
func (c *Client) UpdateAttribute(ctx context.Context, attr *Attribute) (*Attribute, error) {

	if attr == nil {
		return nil, fmt.Errorf("attribute is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/attributes.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	// we can either use PUT on /attributes or PATCH on /attributes/{inum}

	ret := &Attribute{}

	if err := c.put(ctx, "/jans-config-api/api/v1/attributes/", token, attr, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteAttribute deletes the attribute with the given inum.
func (c *Client) DeleteAttribute(ctx context.Context, inum string) error {

	if inum == "" {
		return fmt.Errorf("inum is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/attributes.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/attributes/"+inum, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
