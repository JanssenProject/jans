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

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/maxCount",
			Value: 100,
		},
	}

	updatedCfg, err := client.PatchScimAppConfiguration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = 200
		_, _ = client.PatchScimAppConfiguration(ctx, patches)
	})

	if updatedCfg.MaxCount != 100 {
		t.Fatalf("Expected 100, got %d", updatedCfg.MaxCount)
	}

}
