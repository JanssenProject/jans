package jans

import (
	"context"
	"fmt"
)

type HealthCheck struct {
	Name   string `json:"name"`
	Status string `json:"status"`
}

type HealthStatus struct {
	Status     string                 `schema:"status" json:"status,omitempty"`
	Checks     []HealthCheck          `schema:"checks" json:"checks,omitempty"`
	DbType     string                 `schema:"db_type" json:"dbType,omitempty"`
	LastUpdate string                 `schema:"last_update" json:"lastUpdate,omitempty"`
	FacterData map[string]interface{} `schema:"facter_data" json:"facterData,omitempty"`
}

type StatsData struct {
	DbType     string                 `schema:"db_type" json:"dbType,omitempty"`
	LastUpdate string                 `schema:"last_update" json:"lastUpdate,omitempty"`
	FacterData map[string]interface{} `schema:"facter_data" json:"facterData,omitempty"`
}

func (c *Client) GetHealthStatus(ctx context.Context) ([]HealthStatus, error) {
	var healthStatus []HealthStatus
	if err := c.get(ctx, "/api/v1/health", "", &healthStatus); err != nil {
		return nil, fmt.Errorf("get health status: %w", err)
	}

	return healthStatus, nil
}

func (c *Client) GetServerStats(ctx context.Context) (*StatsData, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/data.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	var stats StatsData
	if err := c.get(ctx, "/api/v1/health/server-stat", token, &stats); err != nil {
		return nil, fmt.Errorf("get server stats: %w", err)
	}

	return &stats, nil
}
