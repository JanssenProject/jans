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

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/maxCount",
			Value: 5,
		},
	}

	_, err = client.PatchApiAppConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = oldCount
		_, _ = client.PatchApiAppConfiguration(ctx, patches)
	})

	cfg, err = client.GetApiAppConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if cfg.MaxCount != 5 {
		t.Fatalf("%v", cfg.MaxCount)
	}

}
