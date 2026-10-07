package jans

import (
	"context"
	"fmt"
)

// PersistenceConfiguration represents the persistence configuration
// of the Janssen server.
type PersistenceConfiguration struct {
	DatabaseName    string `json:"databaseName,omitempty"`
	SchemaName      string `json:"schemaName,omitempty"`
	ProductName     string `json:"productName,omitempty"`
	ProductVersion  string `json:"productVersion,omitempty"`
	DriverName      string `json:"driverName,omitempty"`
	DriverVersion   string `json:"driverVersion,omitempty"`
	PersistenceType string `schema:"persistence_type" json:"persistenceType,omitempty"`
}

// GetPersistenceConfiguration returns the current persistence configuration.
func (c *Client) GetPersistenceConfiguration(ctx context.Context) (*PersistenceConfiguration, error) {

	scope := "https://jans.io/oauth/jans-auth-server/config/properties.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &PersistenceConfiguration{}

	if err := c.get(ctx, "/jans-config-api/api/v1/jans-auth-server/config/persistence", token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	// The /persistence endpoint reports DB product info but not persistenceType;
	// jans config-api only supports SQL backends, so infer it from the product.
	if ret.PersistenceType == "" && ret.ProductName != "" {
		ret.PersistenceType = "sql"
	}

	return ret, nil
}
