package jans

import (
	"context"
	"fmt"
)

// PersistenceConfiguration represents the persistence configuration
// of the Janssen server.
type PersistenceConfiguration struct {
	PersistenceType string `schema:"persistence_type" json:"persistenceType,omitempty"`
}

// GetPersistenceConfiguration returns the current persistence configuration.
func (c *Client) GetPersistenceConfiguration(ctx context.Context) (*PersistenceConfiguration, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/jans-auth-server/config/properties.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &PersistenceConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/jans-auth-server/config/persistence", token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}
