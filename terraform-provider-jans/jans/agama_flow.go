package jans

import (
	"context"
	"fmt"
)

type FlowMetadata struct {
	FuncName    string            `schema:"func_name" json:"funcName"`
	Inputs      []string          `schema:"inputs" json:"inputs"`
	Timeout     int               `schema:"timeout" json:"timeout"`
	DisplayName string            `schema:"display_name" json:"displayName"`
	Author      string            `schema:"author" json:"author"`
	Timestamp   int               `schema:"timestamp" json:"timestamp"`
	Description string            `schema:"description" json:"description"`
	Properties  map[string]string `schema:"properties" json:"properties"`
}

type AgamaFlow struct {
	Dn        string       `schema:"dn" json:"dn"`
	Qname     string       `schema:"qname" json:"qname"`
	Revision  int          `schema:"revision" json:"revision"`
	Enabled   bool         `schema:"enabled" json:"enabled"`
	Metadata  FlowMetadata `schema:"metadata" json:"metadata"`
	Source    string       `schema:"source" json:"source"`
	CodeError string       `schema:"code_error" json:"codeError"`
}

// GetAgamaFlows returns all currently configured Agama flows.
func (c *Client) GetAgamaFlows(ctx context.Context) ([]AgamaFlow, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type response struct {
		Data       []AgamaFlow `json:"data"`
		Count      int         `json:"entriesCount"`
		TotalItems int         `json:"totalItems"`
	}
	ret := response{}

	if err := c.get(ctx, "/jans-config-api/api/v1/agama", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Data, nil
}

// GetAgamaFlow returns the Agama flow with the given qname.
func (c *Client) GetAgamaFlow(ctx context.Context, qname string) (*AgamaFlow, error) {

	if qname == "" {
		return nil, fmt.Errorf("qname is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &AgamaFlow{}

	if err := c.get(ctx, "/jans-config-api/api/v1/agama/"+qname, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateAgamaFlow creates a new Agama flow.
func (c *Client) CreateAgamaFlow(ctx context.Context, flow *AgamaFlow) (*AgamaFlow, error) {

	if flow == nil {
		return nil, fmt.Errorf("agama flow is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &AgamaFlow{}

	if err := c.post(ctx, "/jans-config-api/api/v1/agama", token, flow, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// // UpdateAgamaFlow updates an already existing OIDC client.
func (c *Client) UpdateAgamaFlow(ctx context.Context, flow *AgamaFlow) error {

	if flow == nil {
		return fmt.Errorf("agama flow is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	orig, err := c.GetAgamaFlow(ctx, flow.Qname)
	if err != nil {
		return fmt.Errorf("failed to get original agama flow: %w", err)
	}

	patches, err := createPatches(flow, orig)
	if err != nil {
		return fmt.Errorf("failed to create patches: %w", err)
	}

	if len(patches) == 0 {
		return fmt.Errorf("no patches provided")
	}

	// first update the flow attributes
	if err := c.patch(ctx, "/jans-config-api/api/v1/agama/"+flow.Qname, token, patches); err != nil {
		return fmt.Errorf("patch request failed: %w", err)
	}

	// then update the flow source code
	if err := c.putText(ctx, "/jans-config-api/api/v1/agama/source/"+flow.Qname, token, flow.Source, nil); err != nil {
		return fmt.Errorf("put request failed: %w", err)
	}

	return nil
}

// DeleteAgamaFlow deletes an already existing Agama flow.
func (c *Client) DeleteAgamaFlow(ctx context.Context, qname string) error {

	if qname == "" {
		return fmt.Errorf("qname is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/agama.delete")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/api/v1/agama/"+qname, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
