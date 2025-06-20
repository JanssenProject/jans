package jans

import (
	"context"
	"testing"
)

func TestPlugins(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	plugins, err := client.GetPlugins(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if len(plugins) == 0 {
		t.Fatal("expected at least one plugin, got none")
	}

	// Verify that each plugin has required fields
	for _, plugin := range plugins {
		if plugin.Name == "" {
			t.Fatal("plugin name should not be empty")
		}
		if plugin.Description == "" {
			t.Fatal("plugin description should not be empty")
		}
	}
}
