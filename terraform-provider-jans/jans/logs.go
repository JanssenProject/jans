
package jans

import (
        "context"
        "strconv"
)

type LogPagedResult struct {
        Start             int      `json:"start,omitempty"`
        TotalEntriesCount int      `json:"totalEntriesCount,omitempty"`
        EntriesCount      int      `json:"entriesCount,omitempty"`
        Entries           []string `json:"entries,omitempty"`
}

func (c *Client) GetAuditLogs(ctx context.Context, pattern string, startIndex, limit int, startDate, endDate string) (*LogPagedResult, error) {
        scope := "https://jans.io/oauth/config/audit-logging.readonly"
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, err
        }

        queryParams := make(map[string]string)
        if pattern != "" {
                queryParams["pattern"] = pattern
        }
        if startIndex > 0 {
                queryParams["startIndex"] = strconv.Itoa(startIndex)
        }
        if limit > 0 {
                queryParams["limit"] = strconv.Itoa(limit)
        }
        if startDate != "" {
                queryParams["start_date"] = startDate
        }
        if endDate != "" {
                queryParams["end_date"] = endDate
        }

        var logs LogPagedResult
        err = c.get(ctx, "/jans-config-api/api/v1/audit", token, scope, &logs, queryParams)
        if err != nil {
                return nil, err
        }

        return &logs, nil
}
