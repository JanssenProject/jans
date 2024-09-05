package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestCreateConfig(t *testing.T) {
	c, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	config := &KCSAMLConfiguration{
		ApplicationName: "SomeAPP",
		Enabled:         true,
	}

	_, err = c.CreateKCSAMLConfiguration(ctx, config)
	if err != nil {
		t.Fatal(err)
	}

	pr := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/applicationName",
			Value: "UpdatedAPP",
		},
	}
	config, err = c.PatchKCSAMLConfiguration(ctx, pr)
	if err != nil {
		t.Fatal(err)
	}

	gotConfig, err := c.GetKCSAMLConfiguration(ctx)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(config, gotConfig); diff != "" {
		t.Errorf("Config mismatch (-want +got):\n%s", diff)
	}
}
