package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestOrganization(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	orga, err := client.GetOrganization(ctx)
	if err != nil {
		t.Fatal(err)
	}

	origOrga, err := client.GetOrganization(ctx)
	if err != nil {
		t.Fatal(err)
	}

	patches := []PatchRequest{
		{
			Op:    "replace",
			Path:  "/displayName",
			Value: "TestDisplayName",
		},
		{
			Op:    "replace",
			Path:  "/description",
			Value: "TestDescription",
		},
		{
			Op:    "replace",
			Path:  "/CountryName",
			Value: "TestCountry",
		},
	}

	updatedOrga, err := client.PatchOrganization(ctx, patches)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		patches[0].Value = origOrga.DisplayName
		patches[1].Value = origOrga.Description
		patches[2].Value = origOrga.CountryName
		_, _ = client.PatchOrganization(ctx, patches)
	})

	orga.DisplayName = "TestDisplayName"
	orga.Description = "TestDescription"
	if diff := cmp.Diff(orga, updatedOrga); diff != "" {
		t.Errorf("Got different organization after update: %s", diff)
	}

}
