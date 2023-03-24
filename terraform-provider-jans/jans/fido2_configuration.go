package jans

import (
	"context"
	"fmt"
)

type Fido2Endpoint struct {
	BasePath        string `schema:"base_path" json:"basePath"`
	OptionsEndpoint string `schema:"options_endpoint" json:"optionsEndpoint"`
	ResultEndpoint  string `schema:"result_endpoint" json:"resultEndpoint"`
}

type Fido2Config struct {
	Version     string          `schema:"version" json:"version"`
	Issuer      string          `schema:"issuer" json:"issuer"`
	Attestation []Fido2Endpoint `schema:"attestation" json:"attestation"`
	Assertion   []Fido2Endpoint `schema:"assertion" json:"assertion"`
}

// GetDefaultAuthenticationMethod returns the current default authentication method.
func (c *Client) GetFido2Config(ctx context.Context) (*Fido2Config, error) {

	// token, err := c.getToken(ctx, "https://jans.io/oauth/config/acrs.readonly")
	// if err != nil {
	// 	return nil, fmt.Errorf("failed to get token: %w", err)
	// }

	token := ""

	ret := &Fido2Config{}

	if err := c.get(ctx, "/jans-fido2/fido2/configuration", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}
