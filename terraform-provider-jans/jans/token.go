package jans

import (
	"context"
	"strconv"
)

// TokenEntity represents a token in Janssen
type TokenEntity struct {
	AuthorizationCode string `json:"authorizationCode,omitempty"`
	ClientId          string `json:"clientId,omitempty"`
	CreationDate      string `json:"creationDate,omitempty"`
	ExpirationDate    string `json:"expirationDate,omitempty"`
	GrantType         string `json:"grantType,omitempty"`
	Scope             string `json:"scope,omitempty"`
	TokenCode         string `json:"tokenCode,omitempty"`
	TokenType         string `json:"tokenType,omitempty"`
	UserId            string `json:"userId,omitempty"`
	Dn                string `json:"dn,omitempty"`
}

// TokenEntityPagedResult represents a paged result of tokens
type TokenEntityPagedResult struct {
	Start        int           `json:"start,omitempty"`
	TotalEntries int           `json:"totalEntries,omitempty"`
	EntriesCount int           `json:"entriesCount,omitempty"`
	Entries      []TokenEntity `json:"entries,omitempty"`
}

// SearchTokens searches for tokens using a pattern with pagination
func (c *Client) SearchTokens(ctx context.Context, pattern string, limit, startIndex int) (*TokenEntityPagedResult, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/token.readonly")
	if err != nil {
		return nil, err
	}

	queryParams := make(map[string]string)
	if pattern != "" {
		queryParams["pattern"] = pattern
	}
	if limit > 0 {
		queryParams["limit"] = strconv.Itoa(limit)
	}
	if startIndex > 0 {
		queryParams["startIndex"] = strconv.Itoa(startIndex)
	}

	var result TokenEntityPagedResult
	err = c.get(ctx, "/jans-config-api/api/v1/token/search", token, &result, queryParams)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

// GetTokensByClient returns all tokens for a specific client
func (c *Client) GetTokensByClient(ctx context.Context, clientId string) (*TokenEntityPagedResult, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/token.readonly")
	if err != nil {
		return nil, err
	}

	var result TokenEntityPagedResult
	err = c.get(ctx, "/jans-config-api/api/v1/token/client/"+clientId, token, &result)
	if err != nil {
		return nil, err
	}

	return &result, nil
}

// GetToken returns a specific token by token code
func (c *Client) GetToken(ctx context.Context, tokenCode string) (*TokenEntity, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/token.readonly")
	if err != nil {
		return nil, err
	}

	var tokenEntity TokenEntity
	err = c.get(ctx, "/jans-config-api/api/v1/token/tknCde/"+tokenCode, token, &tokenEntity)
	if err != nil {
		return nil, err
	}

	return &tokenEntity, nil
}

// RevokeToken revokes a specific token
func (c *Client) RevokeToken(ctx context.Context, tokenCode string) error {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/token.delete")
	if err != nil {
		return err
	}

	return c.delete(ctx, "/jans-config-api/api/v1/token/revoke/"+tokenCode, token)
}
