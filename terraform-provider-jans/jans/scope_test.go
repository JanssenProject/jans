package jans

import (
	"context"
	"testing"
)

func TestScope(t *testing.T) {

	client, err := NewInsecureClient(host, user, pass)
	if err != nil {
		t.Fatal(err)
	}

	ctx := context.Background()

	_, err = client.GetScopes(ctx)
	if err != nil {
		t.Fatal(err)
	}

	scope, err := client.GetScope(ctx, "10B2")
	if err != nil {
		t.Fatal(err)
	}

	if scope.ScopeType != "openid" {
		t.Fatal("wrong scope")
	}

	newScope := &Scope{
		Dn:          "inum=3200.004CD5,ou=scopes,o=jans",
		Inum:        "3200.004BA5",
		DisplayName: "test groups.read",
		Id:          "https://jans.io/test/groups.read",
		Description: "Query test group resources",
		ScopeType:   "oauth",
		Attributes: ScopeAttribute{
			ShowInConfigurationEndpoint: true,
		},
		CreationDate: "2022-09-01T13:42:58",
		UmaType:      false,
		BaseDn:       "inum=3200.004CD5,ou=scopes,o=jans",
	}

	createdScope, err := client.CreateScope(ctx, newScope)
	if err != nil {
		t.Fatal(err)
	}

	if createdScope.DisplayName != "test groups.read" {
		t.Fatal("wrong scope")
	}

	createdScope.DisplayName = "test groups.read updated"
	updatedScope, err := client.UpdateScope(ctx, createdScope)
	if err != nil {
		t.Fatal(err)
	}

	if updatedScope.DisplayName != "test groups.read updated" {
		t.Fatal("wrong scope")
	}

	if err := client.DeleteScope(ctx, updatedScope.Inum); err != nil {
		t.Fatal(err)
	}
}
