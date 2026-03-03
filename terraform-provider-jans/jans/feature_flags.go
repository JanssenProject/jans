package jans

import (
	"context"
	"fmt"
)

func (c *Client) GetFeatureFlags(ctx context.Context) ([]string, error) {

	token, err := c.ensureToken(ctx, "https://jans.io/oauth/jans-auth-server/config/properties.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var flags []string

	if err := c.get(ctx, "/jans-config-api/api/v1/jans-auth-server/config/feature-flags", token, "https://jans.io/oauth/jans-auth-server/config/properties.readonly", &flags); err != nil {
		return nil, fmt.Errorf("failed to get feature flags: %w", err)
	}

	return flags, nil
}
