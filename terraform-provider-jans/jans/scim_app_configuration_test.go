package jans

import (
	"context"
	"testing"
)

func TestScimAppConfiguration(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetScimAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if cfg.MaxCount != 200 {
		t.Fatalf("Expected 200, got %d", cfg.MaxCount)
	}

	cfg.MaxCount = 100
	updatedCfg, err := client.UpdateScimAppConfiguration(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}

	if updatedCfg.MaxCount != 100 {
		t.Fatalf("Expected 100, got %d", updatedCfg.MaxCount)
	}

	cfg.MaxCount = 200
	if _, err = client.UpdateScimAppConfiguration(ctx, cfg); err != nil {
		t.Fatal(err)
	}

}
