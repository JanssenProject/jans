package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestFido2Config(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	cfg, err := client.GetFido2Configuration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	origBasepoint := cfg.BaseEndpoint
	cfg.BaseEndpoint = "newbasepoint"

	// PUT is a full replace, so send the loaded config, not a bare struct.
	updatedConfig, err := client.UpdateFido2Configuration(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		cfg.BaseEndpoint = origBasepoint
		_, _ = client.UpdateFido2Configuration(ctx, cfg)
	})

	if diff := cmp.Diff(cfg, updatedConfig); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if updatedConfig.BaseEndpoint != "newbasepoint" {
		t.Fatal("updatedConfig.BaseEndpoint was not updated")
	}

}
