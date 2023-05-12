package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestFido2Config(t *testing.T) {

	if skipKnownFailures {
		t.SkipNow()
	}

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

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/baseEndpoint",
			Value: "newbasepoint",
		},
	}

	updatedConfig, err := client.PatchFido2Configuration(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = origBasepoint
		_, _ = client.PatchFido2Configuration(ctx, patches)
	})

	if diff := cmp.Diff(cfg, updatedConfig); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if updatedConfig.BaseEndpoint != "newbasepoint" {
		t.Fatal("updatedConfig.BaseEndpoint was not updated")
	}

}
