package jans

import (
	"context"
	"fmt"
)

type ServiceStatus map[string]string

func (c *Client) GetServiceStatus(ctx context.Context, service string) (ServiceStatus, error) {

	token, err := c.ensureToken(ctx, "https://jans.io/oauth/config/data.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var status ServiceStatus

	queryParams := map[string]string{}
	if service != "" && service != "all" {
		queryParams["service"] = service
	}

	if err := c.get(ctx, "/jans-config-api/api/v1/health/service-status", token, "https://jans.io/oauth/config/data.readonly", &status, queryParams); err != nil {
		return nil, fmt.Errorf("failed to get service status: %w", err)
	}

	return status, nil
}
