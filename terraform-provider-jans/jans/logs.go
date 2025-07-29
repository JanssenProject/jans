
package jans

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
)

// LogPagedResult represents the response structure for audit logs
type LogPagedResult struct {
	Start             int      `json:"start,omitempty"`
	TotalEntriesCount int      `json:"totalEntriesCount,omitempty"`
	EntriesCount      int      `json:"entriesCount,omitempty"`
	Entries           []string `json:"entries,omitempty"`
}

// GetAuditLogs retrieves audit log entries
func (c *Client) GetAuditLogs(ctx context.Context, pattern string, startIndex, limit int, startDate, endDate string) (*LogPagedResult, error) {
	if c.token == nil {
		return nil, fmt.Errorf("no token available")
	}

	values := url.Values{}
	if pattern != "" {
		values.Set("pattern", pattern)
	}
	if startIndex > 0 {
		values.Set("startIndex", strconv.Itoa(startIndex))
	}
	if limit > 0 {
		values.Set("limit", strconv.Itoa(limit))
	}
	if startDate != "" {
		values.Set("start_date", startDate)
	}
	if endDate != "" {
		values.Set("end_date", endDate)
	}

	endpoint := "/api/v1/audit"
	if len(values) > 0 {
		endpoint += "?" + values.Encode()
	}

	req, err := http.NewRequest(http.MethodGet, c.baseURL+endpoint, nil)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Authorization", "Bearer "+c.token.AccessToken)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, createError(resp)
	}

	var logs LogPagedResult
	if err := json.NewDecoder(resp.Body).Decode(&logs); err != nil {
		return nil, err
	}

	return &logs, nil
}
