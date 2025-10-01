package jans

import (
	"context"
	"fmt"
)

type Fido2Device struct {
	ID           string   `schema:"id" json:"id,omitempty"`
	Schemas      []string `schema:"schemas" json:"schemas,omitempty"`
	Meta         Meta     `schema:"meta" json:"meta,omitempty"`
	UserID       string   `schema:"userId" json:"userId,omitempty"`
	CreationDate string   `schema:"creation_date" json:"creationDate,omitempty"`
	Counter      int      `schema:"counter" json:"counter,omitempty"`
	Status       string   `schema:"status" json:"status,omitempty"`
	DisplayName  string   `schema:"display_name" json:"displayName,omitempty"`
}

// GetFido2Devices returns all currently configured Fido2 devices.
func (c *Client) GetFido2Devices(ctx context.Context) ([]Fido2Device, error) {

	token, err := c.getToken(ctx, "https://jans.io/scim/fido2.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type Response struct {
		Schemas      []string      `json:"schemas"`
		TotalResults int           `json:"totalResults"`
		StartIndex   int           `json:"startIndex"`
		ItemsPerPage int           `json:"itemsPerPage"`
		Resources    []Fido2Device `json:"Resources"`
	}

	ret := Response{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/Fido2Devices", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Resources, nil
}

// GetFido2Device returns the Fido2 device with the given ID.
func (c *Client) GetFido2Device(ctx context.Context, id string) (*Fido2Device, error) {

	if id == "" {
		return nil, fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/fido2.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Fido2Device{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/Fido2Devices/"+id, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// // UpdateFido2Device updates an already existing Fido2 device.
func (c *Client) UpdateFido2Device(ctx context.Context, device *Fido2Device) (*Fido2Device, error) {

	if device == nil {
		return nil, fmt.Errorf("user is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/fido2.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &Fido2Device{}

	if err := c.put(ctx, "/jans-scim/restv1/v2/Fido2Devices/"+device.ID, token, device, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteFido2Device deletes an already existing Fido2 device.
func (c *Client) DeleteFido2Device(ctx context.Context, id string) error {

	if id == "" {
		return fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/fido2.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-scim/restv1/v2/Fido2Devices/"+id, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
