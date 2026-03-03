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

        scope := "https://jans.io/oauth/config/plugin.readonly"
        token, err := c.ensureToken(ctx, scope)
        if err != nil {
                return nil, fmt.Errorf("failed to get token: %w", err)
        }

        ret := []PluginConf{}

        if err := c.get(ctx, "/jans-config-api/api/v1/plugin", token, scope, &ret); err != nil {
                return nil, fmt.Errorf("get request failed: %w", err)
        }

        return ret, nil
}
