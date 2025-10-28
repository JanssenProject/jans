package jans

import (
	"context"
)

// StatisticsResponse represents the statistics response
// The API returns an array of JsonNode, which can be any JSON structure
type StatisticsResponse []map[string]interface{}

// GetStatistics retrieves statistics from the Janssen server
func (c *Client) GetStatistics(ctx context.Context, month, startMonth, endMonth, format string) (*StatisticsResponse, error) {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/stats.readonly")
	if err != nil {
		return nil, err
	}

	queryParams := make(map[string]string)
	if month != "" {
		queryParams["month"] = month
	}
	if startMonth != "" {
		queryParams["start_month"] = startMonth
	}
	if endMonth != "" {
		queryParams["end_month"] = endMonth
	}
	if format != "" {
		queryParams["format"] = format
	}

	var stats StatisticsResponse
	err = c.get(ctx, "/jans-config-api/api/v1/stat", token, &stats, queryParams)
	if err != nil {
		return nil, err
	}

	return &stats, nil
}
