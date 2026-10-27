package jans

import (
	"context"
	"fmt"
)

// AdminUIPolicyStore is a Cedarling policy store registered with the AdminUI.
// PolicyStore holds the base64-encoded .cjar archive.
type AdminUIPolicyStore struct {
	Dn           string `schema:"dn" json:"dn,omitempty"`
	Inum         string `schema:"inum" json:"inum,omitempty"`
	Displayname  string `schema:"displayname" json:"displayname,omitempty"`
	Description  string `schema:"description" json:"description,omitempty"`
	PolicyStore  string `schema:"policy_store" json:"policyStore,omitempty"`
	JansUsrDN    string `schema:"jans_usr_dn" json:"jansUsrDN,omitempty"`
	JansStatus   string `schema:"jans_status" json:"jansStatus,omitempty"`
	CreationDate string `schema:"creation_date" json:"creationDate,omitempty"`
	JansLastUpd  string `schema:"jans_last_upd" json:"jansLastUpd,omitempty"`
}

// GetAdminUIPolicyStores returns all AdminUI policy stores.
func (c *Client) GetAdminUIPolicyStores(ctx context.Context) ([]AdminUIPolicyStore, error) {

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/security.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := []AdminUIPolicyStore{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/security/policyStore", token, scope, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// GetAdminUIPolicyStore returns a single AdminUI policy store by its inum. The
// API exposes no per-inum read endpoint, so the full list is filtered locally.
func (c *Client) GetAdminUIPolicyStore(ctx context.Context, inum string) (*AdminUIPolicyStore, error) {

	if inum == "" {
		return nil, fmt.Errorf("inum is empty")
	}

	stores, err := c.GetAdminUIPolicyStores(ctx)
	if err != nil {
		return nil, err
	}

	for i := range stores {
		if stores[i].Inum == inum {
			return &stores[i], nil
		}
	}

	return nil, ErrorNotFound
}

// CreateAdminUIPolicyStore creates a new AdminUI policy store.
func (c *Client) CreateAdminUIPolicyStore(ctx context.Context, store *AdminUIPolicyStore) error {

	if store == nil {
		return fmt.Errorf("store is nil")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/security.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.post(ctx, "/jans-config-api/admin-ui/security/policyStore", token, scope, store, store); err != nil {
		return fmt.Errorf("post request failed: %w", err)
	}

	return nil
}

// UpdateAdminUIPolicyStore updates an existing AdminUI policy store identified
// by its inum.
func (c *Client) UpdateAdminUIPolicyStore(ctx context.Context, store *AdminUIPolicyStore) error {

	if store == nil {
		return fmt.Errorf("store is nil")
	}

	if store.Inum == "" {
		return fmt.Errorf("inum is empty")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/security.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.put(ctx, "/jans-config-api/admin-ui/security/policyStore/"+store.Inum, token, scope, store, nil); err != nil {
		return fmt.Errorf("put request failed: %w", err)
	}

	return nil
}

// DeleteAdminUIPolicyStore deletes an existing AdminUI policy store by its inum.
func (c *Client) DeleteAdminUIPolicyStore(ctx context.Context, inum string) error {

	if inum == "" {
		return fmt.Errorf("inum is empty")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/security.delete"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/admin-ui/security/policyStore/"+inum, token, scope); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
