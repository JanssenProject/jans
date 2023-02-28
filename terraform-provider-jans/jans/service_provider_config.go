package jans

import (
	"context"
	"fmt"
)

type Supported struct {
	Supported bool `schema:"supported" json:"supported"`
}

type Filter struct {
	Supported  bool `schema:"supported" json:"supported"`
	MaxResults int  `schema:"max_results" json:"maxResults"`
}

type Bulk struct {
	Supported      bool `schema:"supported" json:"supported"`
	MaxOperations  int  `schema:"max_operations" json:"maxOperations"`
	MaxPayloadSize int  `schema:"max_payload_size" json:"maxPayloadSize"`
}

type AuthenticationSchemes struct {
	Type        string `schema:"type" json:"type"`
	Name        string `schema:"name" json:"name"`
	Description string `schema:"description" json:"description"`
	SpecURI     string `schema:"spec_uri" json:"specUri"`
	DocumentURI string `schema:"document_uri" json:"documentUri"`
	Primary     bool   `schema:"primary" json:"primary"`
}

type ServiceProviderConfig struct {
	Schemas               []string                `schema:"schemas" json:"schemas,omitempty"`
	Meta                  Meta                    `schema:"meta" json:"meta,omitempty"`
	DocumentationUri      string                  `schema:"documentation_uri" json:"documentationUri,omitempty"`
	Patch                 Supported               `schema:"patch" json:"patch,omitempty"`
	Bulk                  Bulk                    `schema:"bulk" json:"bulk,omitempty"`
	Filter                Filter                  `schema:"filter" json:"filter,omitempty"`
	ChangePassword        Supported               `schema:"change_password" json:"changePassword,omitempty"`
	Sort                  Supported               `schema:"sort" json:"sort,omitempty"`
	Etag                  Supported               `schema:"etag" json:"etag,omitempty"`
	AuthenticationSchemes []AuthenticationSchemes `schema:"authentication_schemes" json:"authenticationSchemes,omitempty"`
}

// GetScimAppConfiguration returns the current SCIM App configuration.
func (c *Client) GetServiceProviderConfig(ctx context.Context) (*ServiceProviderConfig, error) {

	ret := &ServiceProviderConfig{}

	if err := c.getScim(ctx, "/jans-scim/restv1/v2/ServiceProviderConfig", "", ret); err != nil {
		return nil, fmt.Errorf("getScim request failed: %w", err)
	}

	return ret, nil
}
