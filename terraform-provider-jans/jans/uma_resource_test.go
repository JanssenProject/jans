package jans

import (
	"context"
	"testing"

	"github.com/google/go-cmp/cmp"
)

func TestUMAResource(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetUMAResources(ctx)
	if err != nil {
		t.Fatal(err)
	}

	newUMA := &UMAResource{
		Name:        "test name",
		Description: "test description",
		Deletable:   true,
	}

	createdUMA, err := client.CreateUMAResource(ctx, newUMA)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteUMAResource(ctx, createdUMA.ID)
	})

	// don't compare generated fields
	filter := cmp.FilterPath(func(p cmp.Path) bool {
		attr := p.String()
		return attr == "ID" || attr == "Dn"
	}, cmp.Ignore())

	if diff := cmp.Diff(createdUMA, newUMA, filter); diff != "" {
		t.Errorf("expected same UMA resource, but got diff:\n %v", diff)
	}

	createdUMA, err = client.GetUMAResource(ctx, createdUMA.ID)
	if err != nil {
		t.Fatal(err)
	} else if diff := cmp.Diff(createdUMA, newUMA, filter); diff != "" {
		t.Errorf("expected same UMA resource, but got diff:\n %v", diff)
	}

	createdUMA.Description = "new description"
	if _, err := client.UpdateUMAResource(ctx, createdUMA); err != nil {
		t.Error(err)
	}

	updatedUMA, err := client.GetUMAResource(ctx, createdUMA.ID)
	if err != nil {
		t.Error(err)
	} else if diff := cmp.Diff(createdUMA, updatedUMA, filter); diff != "" {
		t.Errorf("expected same UMA resource, but got diff:\n %v", diff)
	}

	if err := client.DeleteUMAResource(ctx, createdUMA.ID); err != nil {
		t.Error(err)
	}

}
