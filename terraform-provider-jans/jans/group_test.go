package jans

import (
	"context"
	"testing"
)

func TestGroup(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetGroups(ctx)
	if err != nil {
		t.Fatal(err)
	}

	newGroup := Group{
		Schemas:     []string{"urn:ietf:params:scim:schemas:core:2.0:Group"},
		DisplayName: "test-group",
	}

	createdGroup, err := client.CreateGroup(ctx, &newGroup)
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		_ = client.DeleteGroup(ctx, createdGroup.ID)
	})

	if createdGroup.ID == "" {
		t.Errorf("expected group id, got %s", createdGroup.ID)
	}

	if createdGroup.DisplayName != newGroup.DisplayName {
		t.Errorf("expected group display name %s, got %s", newGroup.DisplayName, createdGroup.DisplayName)
	}

	createdGroup.DisplayName = "test-group-updated"
	updatedGroup, err := client.UpdateGroup(ctx, createdGroup)
	if err != nil {
		t.Fatal(err)
	}

	if updatedGroup.DisplayName != createdGroup.DisplayName {
		t.Errorf("expected group display name %s, got %s", createdGroup.DisplayName, updatedGroup.DisplayName)
	}

	if err := client.DeleteGroup(ctx, createdGroup.ID); err != nil {
		t.Fatal(err)
	}
}
