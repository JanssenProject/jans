package jans

import (
	"context"
	"fmt"
)

type FidoDevice struct {
	ID                     string   `schema:"id" json:"id,omitempty"`
	Schemas                []string `schema:"schemas" json:"schemas,omitempty"`
	Meta                   Meta     `schema:"meta" json:"meta,omitempty"`
	UserID                 string   `schema:"userId" json:"userId,omitempty"`
	CreationDate           string   `schema:"creation_date" json:"creationDate,omitempty"`
	Application            string   `schema:"application" json:"application,omitempty"`
	Counter                int      `schema:"counter" json:"counter,omitempty"`
	DeviceData             string   `schema:"device_data" json:"deviceData,omitempty"`
	DeviceHashCode         string   `schema:"device_hash_code" json:"deviceHashCode,omitempty"`
	DeviceKeyHandle        string   `schema:"device_key_handle" json:"deviceKeyHandle,omitempty"`
	DeviceRegistrationConf string   `schema:"device_registration_conf" json:"deviceRegistrationConf,omitempty"`
	LastAccessTime         string   `schema:"last_access_time" json:"lastAccessTime,omitempty"`
	Status                 string   `schema:"status" json:"status,omitempty"`
	DisplayName            string   `schema:"display_name" json:"displayName,omitempty"`
	Description            string   `schema:"description" json:"description,omitempty"`
}

// GetFidoDevices returns all currently configured Fido devices.
func (c *Client) GetFidoDevices(ctx context.Context) ([]FidoDevice, error) {

	token, err := c.getToken(ctx, "https://jans.io/scim/fido.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	type Response struct {
		Schemas      []string     `json:"schemas"`
		TotalResults int          `json:"totalResults"`
		StartIndex   int          `json:"startIndex"`
		ItemsPerPage int          `json:"itemsPerPage"`
		Resources    []FidoDevice `json:"Resources"`
	}

	ret := Response{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/FidoDevices", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret.Resources, nil
}

// GetFidoDevice returns the Fido device with the given ID.
func (c *Client) GetFidoDevice(ctx context.Context, id string) (*FidoDevice, error) {

	if id == "" {
		return nil, fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/fido.read")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &FidoDevice{}

	if err := c.get(ctx, "/jans-scim/restv1/v2/FidoDevices/"+id, token, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// // UpdateFidoDevice updates an already existing Fido device.
func (c *Client) UpdateFidoDevice(ctx context.Context, device *FidoDevice) (*FidoDevice, error) {

	if device == nil {
		return nil, fmt.Errorf("user is nil")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/fido.write")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &FidoDevice{}

	if err := c.put(ctx, "/jans-scim/restv1/v2/FidoDevices/"+device.ID, token, device, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteFidoDevice deletes an already existing Fido device.
func (c *Client) DeleteFidoDevice(ctx context.Context, id string) error {

	if id == "" {
		return fmt.Errorf("id is empty")
	}

	token, err := c.getToken(ctx, "https://jans.io/scim/fido.write")
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-scim/restv1/v2/FidoDevices/"+id, token); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
