package jans

import (
	"context"
	"testing"
)

func TestApiAppConfigMapping(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetApiAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

}

func TestPatchApiAppConfig(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetApiAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	oldCount := cfg.MaxCount

	cfg.MaxCount = 5

	_, err = client.UpdateApiAppConfiguration(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		cfg.MaxCount = oldCount
		_, _ = client.UpdateApiAppConfiguration(ctx, cfg)
	})

	cfg, err = client.GetApiAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if cfg.MaxCount != 5 {
		t.Fatalf("%v", cfg.MaxCount)
	}

}
