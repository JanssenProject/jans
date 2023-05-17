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

	origSuperGluuEnabled := cfg.SuperGluuEnabled
	cfg.SuperGluuEnabled = true

	origOldU2fMigrationEnabled := cfg.OldU2fMigrationEnabled
	cfg.OldU2fMigrationEnabled = true

	ret := &JansFido2DynConfiguration{
		BaseEndpoint:           "newbasepoint",
		SuperGluuEnabled:       true,
		OldU2fMigrationEnabled: true,
	}
	updatedConfig, err := client.PutFido2Configuration(ctx, ret)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		ret.BaseEndpoint = origBasepoint
		ret.SuperGluuEnabled = origSuperGluuEnabled
		ret.OldU2fMigrationEnabled = origOldU2fMigrationEnabled
		_, _ = client.PutFido2Configuration(ctx, ret)
	})

	if diff := cmp.Diff(cfg, updatedConfig); diff != "" {
		t.Errorf("Got different configuration after update: %s", diff)
	}

	if updatedConfig.BaseEndpoint != "newbasepoint" {
		t.Fatal("updatedConfig.BaseEndpoint was not updated")
	}

}
