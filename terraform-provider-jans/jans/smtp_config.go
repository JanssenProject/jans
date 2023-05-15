package jans

import (
	"context"
	"fmt"
)

// SMTPConfiguration is the configuration for the SMTP server.
type SMTPConfiguration struct {
	Valid                             bool   `schema:"valid" json:"valid,omitempty"`
	Host                              string `schema:"host" json:"host,omitempty"`
	Port                              int    `schema:"port" json:"port,omitempty"`
	ConnectProtection                 string `schema:"connect_protection" json:"connect_protection,omitempty"`
	TrustHost                         bool   `schema:"trust_host" json:"trust_host,omitempty"`
	FromName                          string `schema:"from_name" json:"from_name,omitempty"`
	FromEmailAddress                  string `schema:"from_email_address" json:"from_email_address,omitempty"`
	RequiresAuthentication            bool   `schema:"requires_authentication" json:"requires_authentication,omitempty"`
	SmtpAuthenticationAccountUsername string `schema:"smtp_authentication_account_username" json:"smtp_authentication_account_username,omitempty"`
	SmtpAuthenticationAccountPassword string `schema:"smtp_authentication_account_password" json:"smtp_authentication_account_password,omitempty"`
	KeyStore                          string `schema:"key_store" json:"key_store,omitempty"`
	KeyStorePassword                  string `schema:"key_store_password" json:"key_store_password,omitempty"`
	KeyStoreAlias                     string `schema:"key_store_alias" json:"key_store_alias,omitempty"`
	SigningAlgorithm                  string `schema:"signing_algorithm" json:"signing_algorithm,omitempty"`
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
