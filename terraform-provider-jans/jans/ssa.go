package jans

import (
	"context"
)

// SoftwareStatementAssertion represents an SSA in Janssen
type SoftwareStatementAssertion struct {
	Jti   string `json:"jti,omitempty"`
	OrgId string `json:"orgId,omitempty"`
}

// RevokeSSA revokes a Software Statement Assertion by JWT ID or organization ID
func (c *Client) RevokeSSA(ctx context.Context, jti, orgId string) error {
	token, err := c.getToken(ctx, "https://jans.io/oauth/config/ssa.delete")
	if err != nil {
		return err
	}

	queryParams := make(map[string]string)
	if jti != "" {
		queryParams["jti"] = jti
	}
	if orgId != "" {
		queryParams["org_id"] = orgId
	}

	// Using delete with query params
	params := requestParams{
		method:      "DELETE",
		path:        "/jans-config-api/api/v1/jans-auth-server/ssa",
		contentType: "application/json",
		accept:      "application/json",
		token:       token,
		queryParams: queryParams,
	}

	return c.request(ctx, params)
}
