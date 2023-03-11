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

	if err := client.UpdateFido2Configuration(ctx, cfg); err != nil {
		t.Fatal(err)
	}

	updatedConfig, err := client.GetFido2Configuration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, updatedConfig); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if updatedConfig.BaseEndpoint != "newbasepoint" {
		t.Fatal("updatedConfig.BaseEndpoint was not updated")
	}

	cfg.BaseEndpoint = origBasepoint
	if err := client.UpdateFido2Configuration(ctx, cfg); err != nil {
		t.Fatal(err)
	}

	updatedConfig, err = client.GetFido2Configuration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(cfg, updatedConfig); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if updatedConfig.BaseEndpoint != origBasepoint {
		t.Fatal("updatedConfig.BaseEndpoint was not updated")
	}

}
