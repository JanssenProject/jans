package jans

import (
	"context"
	"encoding/json"
	"fmt"
)

type AgamaRepository struct {
	Name        string          `json:"name,omitempty"`
	Description string          `json:"description,omitempty"`
	URL         string          `json:"url,omitempty"`
	Metadata    json.RawMessage `json:"metadata,omitempty"`
}

func (c *Client) GetAgamaRepositories(ctx context.Context) ([]AgamaRepository, error) {

	token, err := c.ensureToken(ctx, "https://jans.io/oauth/config/agama-repo.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var repos []AgamaRepository

	if err := c.get(ctx, "/jans-config-api/api/v1/agama-repo", token, "https://jans.io/oauth/config/agama-repo.readonly", &repos); err != nil {
		return nil, fmt.Errorf("failed to get agama repositories: %w", err)
	}

	return repos, nil
}
