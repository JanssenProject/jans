package jans

import (
	"context"
	"fmt"
)

type Member struct {
	Ref     string `schema:"ref" json:"$ref,omitempty"`
	Type    string `schema:"type" json:"type,omitempty"`
	Display string `schema:"display" json:"display,omitempty"`
	Value   string `schema:"value" json:"value,omitempty"`
}

type Group struct {
	ID          string   `schema:"id" json:"id,omitempty"`
	Schemas     []string `schema:"schemas" json:"schemas,omitempty"`
	Meta        Meta     `schema:"meta" json:"meta,omitempty"`
	DisplayName string   `schema:"display_name" json:"displayName,omitempty"`
	Members     []Member `schema:"members" json:"members,omitempty"`
}

// GetGroups returns all currently configured groups within SCIM.
func (c *Client) GetGroups(ctx context.Context) ([]Group, error) {

	token, err := c.getToken(ctx, "https://jans.io/scim/groups.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type Response struct {
		Schemas      []string `json:"schemas"`
		TotalResults int      `json:"totalResults"`
		StartIndex   int      `json:"startIndex"`
		ItemsPerPage int      `json:"itemsPerPage"`
		Resources    []Group  `json:"Resources"`
	}

	ret := Response{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/Groups", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Resources, nil
}

// GetGroup returns the SCIM group with the given ID.
func (c *Client) GetGroup(ctx context.Context, id string) (*Group, error) {

	if id == "" {
		return nil, fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/groups.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Group{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/Groups/"+id, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateGroup creates a new SCIM group.
func (c *Client) CreateGroup(ctx context.Context, group *Group) (*Group, error) {

	if group == nil {
		return nil, fmt.Errorf("group is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/groups.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Group{}

	if err := c.post(ctx, "/jans-scim/restv1/v2/Groups", token, group, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// // UpdateGroup updates an already existing SCIM group.
func (c *Client) UpdateGroup(ctx context.Context, group *Group) (*Group, error) {

	if group == nil {
		return nil, fmt.Errorf("group is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/groups.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Group{}

	if err := c.put(ctx, "/jans-scim/restv1/v2/Groups/"+group.ID, token, group, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteGroup deletes an already existing SCIM group.
func (c *Client) DeleteGroup(ctx context.Context, id string) error {

	if id == "" {
		return fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/groups.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-scim/restv1/v2/Groups/"+id, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
