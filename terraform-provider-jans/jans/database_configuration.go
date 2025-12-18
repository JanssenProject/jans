package jans

import (
	"context"
	"encoding/json"
	"fmt"
)

type DatabaseSchemaField struct {
	Name        string `json:"name,omitempty"`
	DefName     string `json:"defName,omitempty"`
	Type        string `json:"type,omitempty"`
	MultiValued bool   `json:"multiValued,omitempty"`
}

type DatabaseSchema map[string]map[string]DatabaseSchemaField

func (c *Client) GetDatabaseSchema(ctx context.Context) (DatabaseSchema, error) {

	token, err := c.ensureToken(ctx, "https://jans.io/oauth/config/database.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var rawSchema json.RawMessage

	if err := c.get(ctx, "/jans-config-api/api/v1/config/database", token, "https://jans.io/oauth/config/database.readonly", &rawSchema); err != nil {
		return nil, fmt.Errorf("failed to get database schema: %w", err)
	}

	var schema DatabaseSchema
	if err := json.Unmarshal(rawSchema, &schema); err != nil {
		return nil, fmt.Errorf("failed to unmarshal database schema: %w", err)
	}

	return schema, nil
}
