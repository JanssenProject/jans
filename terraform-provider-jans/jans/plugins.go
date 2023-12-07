package jans

import (
	"context"
	"fmt"
)

// PluginConf represents a single plugin enabled on the server.
type PluginConf struct {
	Name        string `schema:"name" json:"name,omitempty"`
	Description string `schema:"description" json:"description,omitempty"`
	ClassName   string `schema:"class_name" json:"className,omitempty"`
}

// Plugins holds the list of all plugins currently enabled on the server.
type Plugins struct {
	Enabled []PluginConf `schema:"enabled"`
}

// GetPlugins returns the list of plugins currently enabled on the server.
func (c *Client) GetPlugins(ctx context.Context) ([]PluginConf, error) {

	token, err := c.getToken(ctx, "https://jans.io/oauth/config/plugin.readonly")
	if err != nil {
		return nil, fmt.Errorf("failed to get token: %w", err)
	}

	ret := []PluginConf{}

	if err := c.get(ctx, "/jans-config-api/api/v1/plugin", token, &ret); err != nil {
		return nil, fmt.Errorf("get request failed: %w", err)
	}

	return ret, nil
}
