package jans

import (
	"context"
	"encoding/json"
	"fmt"
)

type AgamaRepository struct {
	Name        string          `json:"repository-name,omitempty"`
	Description string          `json:"description,omitempty"`
	URL         string          `json:"download-link,omitempty"`
	Metadata    json.RawMessage `json:"metadata,omitempty"`
}

type agamaRepoResponse struct {
	Result   bool              `json:"result"`
	Projects []AgamaRepository `json:"projects"`
	Error    string            `json:"error"`
}

func (c *Client) GetAgamaRepositories(ctx context.Context) ([]AgamaRepository, error) {

	token, err := c.ensureToken(ctx, "https://jans.io/oauth/config/agama-repo.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var resp agamaRepoResponse

	if err := c.get(ctx, "/jans-config-api/api/v1/agama-repo", token, "https://jans.io/oauth/config/agama-repo.readonly", &resp); err != nil {
		return nil, fmt.Errorf("failed to get agama repositories: %w", err)
	}

	return resp.Projects, nil
}
