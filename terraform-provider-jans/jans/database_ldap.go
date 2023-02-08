package jans

import (
	"context"
	"errors"
	"fmt"
)

// LDAPDBConfiguration represents a single LDAP configuration
type LDAPDBConfiguration struct {
	ConfigId         string   `schema:"config_id" json:"configId,omitempty"`
	BindDN           string   `schema:"bind_dn" json:"bindDN,omitempty"`
	BindPassword     string   `schema:"bind_password" json:"bindPassword,omitempty"`
	Servers          []string `schema:"servers" json:"servers,omitempty"`
	MaxConnections   int      `schema:"max_connections" json:"maxConnections,omitempty"`
	UseSSL           bool     `schema:"use_ssl" json:"useSSL,omitempty"`
	BaseDNs          []string `schema:"base_dns" json:"baseDNs,omitempty"`
	PrimaryKey       string   `schema:"primary_key" json:"primaryKey,omitempty"`
	LocalPrimaryKey  string   `schema:"local_primary_key" json:"localPrimaryKey,omitempty"`
	UseAnonymousBind bool     `schema:"use_anonymous_bind" json:"useAnonymousBind,omitempty"`
	Enabled          bool     `schema:"enabled" json:"enabled,omitempty"`
	Version          int      `schema:"version" json:"version,omitempty"`
	Level            int      `schema:"level" json:"level,omitempty"`
}

// GetLDAPDBConfigurations returns all LDAP configurations
func (c *Client) GetLDAPDBConfigurations(ctx context.Context) ([]LDAPDBConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/database/ldap.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := []LDAPDBConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/config/database/ldap", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetLDAPDBConfiguration returns a single LDAP configuration
func (c *Client) GetLDAPDBConfiguration(ctx context.Context, name string) (*LDAPDBConfiguration, error) {

	if name == "" {
		return nil, fmt.Errorf("name is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/database/ldap.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &LDAPDBConfiguration{}

	err = c.get(ctx, "/jans-config-api/api/v1/config/database/ldap/"+name, token, ret)
	if err != nil && !errors.Is(err, ErrorNotFound) {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateLDAPDBConfiguration creates a new LDAP configuration
func (c *Client) CreateLDAPDBConfiguration(ctx context.Context, config *LDAPDBConfiguration) (*LDAPDBConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/database/ldap.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &LDAPDBConfiguration{}

	if err := c.post(ctx, "/jans-config-api/api/v1/config/database/ldap/", token, config, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateLDAPDBConfiguration updates an existing LDAP configuration
func (c *Client) UpdateLDAPDBConfiguration(ctx context.Context, config *LDAPDBConfiguration) (*LDAPDBConfiguration, error) {

	if config == nil {
		return nil, fmt.Errorf("config is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/database/ldap.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	// we can either use PUT on /database/ldap or PATCH on /database/ldap/{name}

	ret := &LDAPDBConfiguration{}

	if err := c.put(ctx, "/jans-config-api/api/v1/config/database/ldap/", token, config, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteLDAPDBConfiguration deletes an existing LDAP configuration
func (c *Client) DeleteLDAPDBConfiguration(ctx context.Context, name string) error {

	if name == "" {
		return fmt.Errorf("name is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/database/ldap.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/config/database/ldap/"+name, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
