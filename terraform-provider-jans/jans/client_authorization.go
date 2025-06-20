package jans

import (
	"context"
	"fmt"
	"time"
)

type ClientAuth struct {
	ClientID string             `json:"clientId,omitempty"`
	Data     map[string][]Scope `json:"data,omitempty"`
}

func (c *Client) GetClientAuthorization(ctx context.Context, userId string) (*ClientAuth, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/client/authorizations.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	path := fmt.Sprintf("/api/v1/clients/authorizations/%s", userId)

	var clientAuth ClientAuth
	if err := c.get(ctx, path, token, &clientAuth); err != nil {
		return nil, fmt.Errorf("get client authorization: %w", err)
	}

	return &clientAuth, nil
}

func (c *Client) GetClientAuthorizations(ctx context.Context) ([]ClientAuthorization, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/client/authorizations.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var authorizations []ClientAuthorization
	if err := c.get(ctx, "/api/v1/clients/authorizations", token, &authorizations); err != nil {
		return nil, fmt.Errorf("get client authorizations: %w", err)
	}

	return authorizations, nil
}

func (c *Client) CreateClientAuthorization(ctx context.Context, auth *ClientAuthorization) (*ClientAuthorization, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/client/authorizations.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var createdAuth ClientAuthorization
	if err := c.post(ctx, "/api/v1/clients/authorizations", token, auth, &createdAuth); err != nil {
		return nil, fmt.Errorf("create client authorization: %w", err)
	}

	return &createdAuth, nil
}

func (c *Client) UpdateClientAuthorization(ctx context.Context, auth *ClientAuthorization) (*ClientAuthorization, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/client/authorizations.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	path := fmt.Sprintf("/api/v1/clients/authorizations/%s", auth.Inum)

	var updatedAuth ClientAuthorization
	if err := c.put(ctx, path, token, auth, &updatedAuth); err != nil {
		return nil, fmt.Errorf("update client authorization: %w", err)
	}

	return &updatedAuth, nil
}

func (c *Client) DeleteClientAuthorization(ctx context.Context, userId, clientId, username string) error {
	token, err := c.getToken(ctx, "https://jans.io/oauth/client/authorizations.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	path := fmt.Sprintf("/api/v1/clients/authorizations/%s/%s/%s", userId, clientId, username)

	if err := c.delete(ctx, path, token); err != nil {
		return fmt.Errorf("delete client authorization: %w", err)
	}

	return nil
}

// ClientAuthorization struct exactly matching the swagger specification
type ClientAuthorization struct {
	ID             string    `schema:"id" json:"id,omitempty"`
	Inum           string    `schema:"inum" json:"inum,omitempty"`
	Dn             string    `schema:"dn" json:"dn,omitempty"`
	ClientId       string    `schema:"client_id" json:"clientId,omitempty"`
	UserId         string    `schema:"user_id" json:"userId,omitempty"`
	Scopes         []string  `schema:"scopes" json:"scopes,omitempty"`
	RedirectURIs   []string  `schema:"redirect_uris" json:"redirectUris,omitempty"`
	GrantTypes     []string  `schema:"grant_types" json:"grantTypes,omitempty"`
	CreationDate   time.Time `schema:"creation_date" json:"creationDate,omitempty"`
	ExpirationDate time.Time `schema:"expiration_date" json:"expirationDate,omitempty"`
	Deletable      bool      `schema:"deletable" json:"deletable,omitempty"`
}
