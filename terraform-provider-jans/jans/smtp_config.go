package jans

import (
	"context"
	"fmt"
)

// SMTPConfiguration is the configuration for the SMTP server.
type SMTPConfiguration struct {
	Host                   string `schema:"host" json:"host,omitempty"`
	Port                   int    `schema:"port" json:"port,omitempty"`
	RequiresSSL            bool   `schema:"requires_ssl" json:"requires_ssl,omitempty"`
	TrustHost              bool   `schema:"trust_host" json:"trust_host,omitempty"`
	FromName               string `schema:"from_name" json:"from_name,omitempty"`
	FromEmailAddress       string `schema:"from_email_address" json:"from_email_address,omitempty"`
	RequiresAuthentication bool   `schema:"requires_authentication" json:"requires_authentication,omitempty"`
	UserName               string `schema:"user_name" json:"user_name,omitempty"`
	Password               string `schema:"password" json:"password,omitempty"`
}

// GetSMTPConfiguration returns the SMTP configuration.
func (c *Client) GetSMTPConfiguration(ctx context.Context) (*SMTPConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/smtp.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &SMTPConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/smtp", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateSMTPConfiguration creates a new SMTP configuration.
func (c *Client) CreateSMTPConfiguration(ctx context.Context, config *SMTPConfiguration) (*SMTPConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/smtp.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &SMTPConfiguration{}

	if err := c.post(ctx, "/jans-config-api/api/v1/config/smtp", token, config, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateSMTPConfiguration updates an existing SMTP configuration.
func (c *Client) UpdateSMTPConfiguration(ctx context.Context, config *SMTPConfiguration) (*SMTPConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/smtp.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &SMTPConfiguration{}

	if err := c.put(ctx, "/jans-config-api/api/v1/config/smtp", token, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteSMTPConfiguration deletes an existing SMTP configuration.
func (c *Client) DeleteSMTPConfiguration(ctx context.Context) error {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/smtp.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/config/smtp", token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
