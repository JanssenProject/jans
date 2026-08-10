package jans

import (
	"context"
	"testing"
)

func TestLoggingConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetLoggingConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	// the initial level is environment-dependent (CI runs the server at TRACE),
	// so assert the update round-trip rather than a fixed starting level.
	original := cfg.LoggingLevel

	cfg.LoggingLevel = "DEBUG"

	updatedConfig, err := client.UpdateLoggingConfiguration(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}

	if updatedConfig.LoggingLevel != "DEBUG" {
		t.Error("expected DEBUG logging level")
	}

	updatedConfig.LoggingLevel = original
	if _, err := client.UpdateLoggingConfiguration(ctx, updatedConfig); err != nil {
		t.Errorf("unexpected error: %v", err)
	}
}
