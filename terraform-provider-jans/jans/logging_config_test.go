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

	if cfg.LoggingLevel != "INFO" {
		t.Error("expected INFO level")
	}

	cfg.LoggingLevel = "DEBUG"

	updatedConfig, err := client.UpdateLoggingConfiguration(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}

	if updatedConfig.LoggingLevel != "DEBUG" {
		t.Error("expected DEBUG logging level")
	}

	updatedConfig.LoggingLevel = "INFO"
	if _, err := client.UpdateLoggingConfiguration(ctx, updatedConfig); err != nil {
		t.Errorf("unexpected error: %v", err)
	}
}
