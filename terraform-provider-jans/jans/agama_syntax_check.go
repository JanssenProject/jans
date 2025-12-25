package jans

import (
        "context"
        "fmt"
        "net/url"
        "strings"
)

type AgamaSyntaxCheckResult struct {
        FlowName string `json:"flow_name,omitempty"`
        Code     string `json:"code,omitempty"`
        Valid    bool   `json:"valid,omitempty"`
        Message  string `json:"message,omitempty"`
}

func (c *Client) CheckAgamaSyntax(ctx context.Context, flowName string, code string) (*AgamaSyntaxCheckResult, error) {

        flowName = strings.TrimSpace(flowName)
        if flowName == "" {
                return nil, fmt.Errorf("flowName must be provided")
        }

        code = strings.TrimSpace(code)
        if code == "" {
                return nil, fmt.Errorf("code must be provided")
        }

        token, err := c.ensureToken(ctx, "https://jans.io/oauth/config/agama.readonly")
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        path := fmt.Sprintf("/jans-config-api/api/v1/agama/syntax-check/%s", url.PathEscape(flowName))

        var message string
        if err := c.postText(ctx, path, token, code, &message); err != nil {
                return nil, fmt.Errorf("syntax check request failed: %w", err)
        }

        result := &AgamaSyntaxCheckResult{
                FlowName: flowName,
                Code:     code,
                Valid:    message == "" || strings.Contains(message, "Syntax is OK"),
                Message:  message,
        }

        return result, nil
}

func (c *Client) postText(ctx context.Context, path, token, req string, resp any) error {

        params := requestParams{
                method:      "POST",
                path:        path,
                contentType: "text/plain",
                accept:      "application/json",
                token:       token,
                payload:     []byte(req),
                resp:        resp,
        }

        return c.request(ctx, params)
}
