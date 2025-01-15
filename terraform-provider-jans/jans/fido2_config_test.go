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

	ret := &JansFido2DynConfiguration{
		BaseEndpoint: "newbasepoint",
	}
	updatedConfig, err := client.UpdateFido2Configuration(ctx, ret)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		ret.BaseEndpoint = origBasepoint
		_, _ = client.UpdateFido2Configuration(ctx, ret)
	})

	if diff := cmp.Diff(cfg, updatedConfig); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if updatedConfig.BaseEndpoint != "newbasepoint" {
		t.Fatal("updatedConfig.BaseEndpoint was not updated")
	}

}
