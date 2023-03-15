package jans

import (
	"context"
	"fmt"
)

type Name struct {
	FamilyName      string `schema:"family_name" json:"familyName,omitempty"`
	GivenName       string `schema:"given_name" json:"givenName,omitempty"`
	MiddleName      string `schema:"middle_name" json:"middleName,omitempty"`
	HonorificPrefix string `schema:"honorific_prefix" json:"honorificPrefix,omitempty"`
	HonorificSuffix string `schema:"honorific_suffix" json:"honorificSuffix,omitempty"`
	Formatted       string `schema:"formatted" json:"formatted,omitempty"`
}

type Email struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type PhoneNumber struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type InstantMessagingAddress struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type Photo struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type Address struct {
	Formatted     string `schema:"formatted" json:"formatted,omitempty"`
	StreetAddress string `schema:"street_address" json:"streetAddress,omitempty"`
	Locality      string `schema:"locality" json:"locality,omitempty"`
	Region        string `schema:"region" json:"region,omitempty"`
	PostalCode    string `schema:"postal_code" json:"postalCode,omitempty"`
	Country       string `schema:"country" json:"country,omitempty"`
	Type          string `schema:"type" json:"type,omitempty"`
	Primary       bool   `schema:"primary" json:"primary,omitempty"`
}

type Entitlement struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type Role struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type X509Certificate struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Primary bool   `schema:"primary" json:"primary,omitempty"`
}

type GroupReference struct {
	Value   string `schema:"value" json:"value,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Ref     string `schema:"ref" json:"$ref,omitempty"`
}

type User struct {
	ID                string                    `schema:"id" json:"id,omitempty"`
	Schemas           []string                  `schema:"schemas" json:"schemas,omitempty"`
	Meta              Meta                      `schema:"meta" json:"meta,omitempty"`
	ExternalId        string                    `schema:"external_id" json:"externalId,omitempty"`
	UserName          string                    `schema:"user_name" json:"userName,omitempty"`
	Name              Name                      `schema:"name" json:"name,omitempty"`
	DisplayName       string                    `schema:"display_name" json:"displayName,omitempty"`
	NickName          string                    `schema:"nick_name" json:"nickName,omitempty"`
	ProfileUrl        string                    `schema:"profile_url" json:"profileUrl,omitempty"`
	Title             string                    `schema:"title" json:"title,omitempty"`
	UserType          string                    `schema:"user_type" json:"userType,omitempty"`
	PreferredLanguage string                    `schema:"preferred_language" json:"preferredLanguage,omitempty"`
	Locale            string                    `schema:"locale" json:"locale,omitempty"`
	Timezone          string                    `schema:"timezone" json:"timezone,omitempty"`
	Active            bool                      `schema:"active" json:"active,omitempty"`
	Password          string                    `schema:"password" json:"password,omitempty"`
	Emails            []Email                   `schema:"emails" json:"emails,omitempty"`
	PhoneNumbers      []PhoneNumber             `schema:"phone_numbers" json:"phoneNumbers,omitempty"`
	Ims               []InstantMessagingAddress `schema:"ims" json:"ims,omitempty"`
	Photos            []Photo                   `schema:"photos" json:"photos,omitempty"`
	Addresses         []Address                 `schema:"addresses" json:"addresses,omitempty"`
	Groups            []GroupReference          `schema:"groups" json:"groups,omitempty"`
	Entitlements      []Entitlement             `schema:"entitlements" json:"entitlements,omitempty"`
	Roles             []Role                    `schema:"roles" json:"roles,omitempty"`
	X509Certificates  []X509Certificate         `schema:"x509_certificates" json:"x509Certificates,omitempty"`
	// Extensions         map[string]any            `schema:"extensions" json:"urn:ietf:params:scim:schemas:extension:gluu:2.0:User"`
}

// GetUsers returns all currently configured users within SCIM.
func (c *Client) GetUsers(ctx context.Context) ([]User, error) {

	token, err := c.getToken(ctx, "https://jans.io/scim/users.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type Response struct {
		Schemas      []string `json:"schemas"`
		TotalResults int      `json:"totalResults"`
		StartIndex   int      `json:"startIndex"`
		ItemsPerPage int      `json:"itemsPerPage"`
		Resources    []User   `json:"Resources"`
	}

	ret := Response{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/Users", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Resources, nil
}

// GetUser returns the SCIM user with the given ID.
func (c *Client) GetUser(ctx context.Context, id string) (*User, error) {

	if id == "" {
		return nil, fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/users.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &User{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/Users/"+id, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateUser creates a new SCIM user.
func (c *Client) CreateUser(ctx context.Context, user *User) (*User, error) {

	if user == nil {
		return nil, fmt.Errorf("user is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/users.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &User{}

	if err := c.post(ctx, "/jans-scim/restv1/v2/Users", token, user, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// // UpdateUser updates an already existing SCIM user.
func (c *Client) UpdateUser(ctx context.Context, user *User) (*User, error) {

	if user == nil {
		return nil, fmt.Errorf("user is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/users.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &User{}

	if err := c.put(ctx, "/jans-scim/restv1/v2/Users/"+user.ID, token, user, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteUser deletes an already existing SCIM user.
func (c *Client) DeleteUser(ctx context.Context, id string) error {

	if id == "" {
		return fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/users.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-scim/restv1/v2/Users/"+id, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
