package jans

import (
	"context"
	"fmt"
)

// WebhookEntry is an AdminUI webhook registration.
type WebhookEntry struct {
	Dn                    string            `schema:"dn" json:"dn,omitempty"`
	Inum                  string            `schema:"inum" json:"inum,omitempty"`
	DisplayName           string            `schema:"display_name" json:"displayName,omitempty"`
	Description           string            `schema:"description" json:"description,omitempty"`
	Url                   string            `schema:"url" json:"url,omitempty"`
	HttpRequestBodyString string            `schema:"http_request_body_string" json:"httpRequestBodyString,omitempty"`
	HttpMethod            string            `schema:"http_method" json:"httpMethod,omitempty"`
	JansEnabled           bool              `schema:"jans_enabled" json:"jansEnabled,omitempty"`
	HttpHeaders           []KeyValuePair    `schema:"http_headers" json:"httpHeaders,omitempty"`
	AuiFeatureIds         []string          `schema:"aui_feature_ids" json:"auiFeatureIds,omitempty"`
	HttpRequestBody       map[string]string `schema:"http_request_body" json:"httpRequestBody,omitempty"`
	BaseDn                string            `schema:"base_dn" json:"baseDn,omitempty"`
}

// GetAdminUIWebhook returns a single AdminUI webhook by its id (inum).
func (c *Client) GetAdminUIWebhook(ctx context.Context, webhookId string) (*WebhookEntry, error) {

	if webhookId == "" {
		return nil, fmt.Errorf("webhookId is empty")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.readonly"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &WebhookEntry{}

	if err := c.get(ctx, "/jans-config-api/admin-ui/webhook/"+webhookId, token, scope, ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}

// CreateAdminUIWebhook creates a new AdminUI webhook and returns the created entry.
func (c *Client) CreateAdminUIWebhook(ctx context.Context, webhook *WebhookEntry) (*WebhookEntry, error) {

	if webhook == nil {
		return nil, fmt.Errorf("webhook is nil")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &WebhookEntry{}

	if err := c.post(ctx, "/jans-config-api/admin-ui/webhook", token, scope, webhook, ret); err != nil {
		return nil, fmt.Errorf("post request failed: %w", err)
	}

	return ret, nil
}

// UpdateAdminUIWebhook updates an existing AdminUI webhook and returns the
// updated entry.
func (c *Client) UpdateAdminUIWebhook(ctx context.Context, webhook *WebhookEntry) (*WebhookEntry, error) {

	if webhook == nil {
		return nil, fmt.Errorf("webhook is nil")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.write"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := &WebhookEntry{}

	if err := c.put(ctx, "/jans-config-api/admin-ui/webhook", token, scope, webhook, ret); err != nil {
		return nil, fmt.Errorf("put request failed: %w", err)
	}

	return ret, nil
}

// DeleteAdminUIWebhook deletes an existing AdminUI webhook by its id (inum).
func (c *Client) DeleteAdminUIWebhook(ctx context.Context, webhookId string) error {

	if webhookId == "" {
		return fmt.Errorf("webhookId is empty")
	}

	scope := "https://jans.io/oauth/jans-auth-server/config/adminui/webhook.delete"
	token, err := c.ensureToken(ctx, scope)
	if err != nil {
		return fmt.Errorf("failed to get token: %w", err)
	}

	if err := c.delete(ctx, "/jans-config-api/admin-ui/webhook/"+webhookId, token, scope); err != nil {
		return fmt.Errorf("delete request failed: %w", err)
	}

	return nil
}
