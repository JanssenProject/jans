package jans

import (
	"context"
	"fmt"
)

// DefaultAuthenticationMethod represents the default authentication method
// of the Janssen server.
type DefaultAuthenticationMethod struct {
	DefaultAcr string `schema:"default_acr" json:"defaultAcr,omitempty"`
}

// GetDefaultAuthenticationMethod returns the current default authentication method.
func (c *Client) GetDefaultAuthenticationMethod(ctx context.Context) (*DefaultAuthenticationMethod, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/acrs.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &DefaultAuthenticationMethod{}

	if err := c.get(ctx, "/jans-config-api/api/v1/acrs", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// UpdateDefaultAuthenticationMethod updates the default authentication method.
func (c *Client) UpdateDefaultAuthenticationMethod(ctx context.Context, authMethod *DefaultAuthenticationMethod) (*DefaultAuthenticationMethod, error) {

	if authMethod == nil {
		return nil, fmt.Errorf("authMethod is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/acrs.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &DefaultAuthenticationMethod{}

	if err := c.put(ctx, "/jans-config-api/api/v1/acrs", token, authMethod, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}
