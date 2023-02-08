package jans

import (
	"context"
	"fmt"
)

// JsonWebKey is the representation of a JSON Web Key.
type JsonWebKey struct {
	Name  string   `schema:"name" json:"name"`
	Descr string   `schema:"descr" json:"descr"`
	Kid   string   `schema:"kid" json:"kid"`
	Kty   string   `schema:"kty" json:"kty"`
	Use   string   `schema:"use" json:"use"`
	Alg   string   `schema:"alg" json:"alg"`
	Crv   string   `schema:"crv" json:"crv"`
	Exp   int      `schema:"exp" json:"exp"`
	X5c   []string `schema:"x5c" json:"x5c"`
	N     string   `schema:"n" json:"n"`
	E     string   `schema:"e" json:"e"`
	X     string   `schema:"x" json:"x"`
	Y     string   `schema:"y" json:"y"`
}

// WebKeysConfiguration is a list of JSON Web Keys, configured in the server.
type WebKeysConfiguration struct {
	Keys []JsonWebKey `schema:"keys" json:"keys"`
}

// GetWebKeysConfiguration returns a list of all JSON web keys currently configured
// in the server.
func (c *Client) GetWebKeysConfiguration(ctx context.Context) (*WebKeysConfiguration, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jwks.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &WebKeysConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/jwks", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetJsonWebKey returns a single JSON web key, identified by its kid.
func (c *Client) GetJsonWebKey(ctx context.Context, kid string) (*JsonWebKey, error) {

	if kid == "" {
		return nil, fmt.Errorf("kid is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jwks.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &JsonWebKey{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/jwks/"+kid, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateJsonWebKey creates a new JSON web key.
func (c *Client) CreateJsonWebKey(ctx context.Context, jwk *JsonWebKey) (*JsonWebKey, error) {

	if jwk == nil {
		return nil, fmt.Errorf("jwk is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jwks.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &JsonWebKey{}

	if err := c.post(ctx, "/jans-config-api/api/v1/config/jwks/key", token, jwk, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateJsonWebKey updates an existing JSON web key.
func (c *Client) UpdateJsonWebKey(ctx context.Context, jwk *JsonWebKey) (*JsonWebKey, error) {

	if jwk == nil {
		return nil, fmt.Errorf("jwk is nil")
	}

	// we have to use patch requests here, since PUT operation on the `/jwk`
	// endpoint expects the complete list of  JWKs. But first we have to get
	// the current version of the JWKs, so we keep the patch list small.

	orig, err := c.GetJsonWebKey(ctx, jwk.Kid)
	if err != nil {
		return nil, fmt.Errorf("failed to get JWK: %w", err)
	}

	patches, err := createPatches(jwk, orig)
	if err != nil {
		return nil, fmt.Errorf("failed to create patches: %w", err)
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jwks.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.patch(ctx, "/jans-config-api/api/v1/config/jwks/"+jwk.Kid, token, patches); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return c.GetJsonWebKey(ctx, jwk.Kid)
}

// DeleteJsonWebKey deletes the JSON web key with the given kid.
func (c *Client) DeleteJsonWebKey(ctx context.Context, kid string) error {

	if kid == "" {
		return fmt.Errorf("kid is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/jwks.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/config/jwks/"+kid, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
