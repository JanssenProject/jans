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

	// orga.Dn = "o=janssen"
	// orga.BaseDn = "o=janssen"
	orga.DisplayName = "TestDisplayName"
	orga.Description = "TestDescription"
	orga.Member = "yes"
	orga.Organization = "TestOrga"
	orga.ManagerGroup = "TestManagerGroup"
	orga.ShortName = "TestShortName"

	updatedOrga, err := client.UpdateOrganization(ctx, orga)
	if err != nil {
		t.Fatal(err)
	}

	if diff := cmp.Diff(orga, updatedOrga); diff != "" {
		t.Errorf("Got different organization after update: %s", diff)
	}

	if _, err = client.UpdateOrganization(ctx, origOrga); err != nil {
		t.Fatal(err)
	}

}
