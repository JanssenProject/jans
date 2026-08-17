package jans

import (
	"context"
	"fmt"
)

// ClientAuth mirrors the config-api ClientAuth model: a map keyed by a client
// descriptor to the set of scopes the user authorized for that client.
type ClientAuth struct {
	ClientAuths map[string][]Scope `json:"clientAuths,omitempty"`
}

func (c *Client) GetClientAuthorization(ctx context.Context, userId string) (*ClientAuth, error) {
	scope := "https://jans.io/oauth/client/authorizations.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	path := fmt.Sprintf("/jans-config-api/api/v1/clients/authorizations/%s", userId)

	var clientAuth ClientAuth
	if err := c.get(ctx, path, token, scope, &clientAuth); err != nil {
		return nil, fmt.Errorf("get client authorization: %w", err)
	}

	return &clientAuth, nil
}

func (c *Client) DeleteClientAuthorization(ctx context.Context, userId, clientId, username string) error {
	scope := "https://jans.io/oauth/client/authorizations.delete"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	path := fmt.Sprintf("/jans-config-api/api/v1/clients/authorizations/%s/%s/%s", userId, clientId, username)

	if err := c.delete(ctx, path, token, scope); err != nil {
		return fmt.Errorf("delete client authorization: %w", err)
	}

	return nil
}
