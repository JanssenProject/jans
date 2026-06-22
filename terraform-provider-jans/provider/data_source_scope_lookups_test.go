package provider

import (
	"testing"

	"github.com/jans/terraform-provider-jans/jans"
)

func TestFlattenScopeLookup(t *testing.T) {
	scopes := []jans.Scope{
		{Inum: "1800.AB", Id: "openid", Dn: "inum=1800.AB,ou=scopes,o=jans", DisplayName: "OpenID", Description: "openid scope", ScopeType: "openid", CreatorId: "admin", CreatorType: "user"},
		{Inum: "1800.CD", Id: "profile", ScopeType: "openid"},
	}

	out := flattenScopeLookup(scopes)
	if len(out) != 2 {
		t.Fatalf("expected 2 results, got %d", len(out))
	}

	first, ok := out[0].(map[string]interface{})
	if !ok {
		t.Fatalf("expected map[string]interface{}, got %T", out[0])
	}
	if first["inum"] != "1800.AB" {
		t.Errorf("inum: expected 1800.AB, got %v", first["inum"])
	}
	if first["scope_id"] != "openid" {
		t.Errorf("scope_id: expected openid, got %v", first["scope_id"])
	}
	if first["scope_type"] != "openid" {
		t.Errorf("scope_type: expected openid, got %v", first["scope_type"])
	}
	if first["creator_id"] != "admin" {
		t.Errorf("creator_id: expected admin, got %v", first["creator_id"])
	}

	if empty := flattenScopeLookup(nil); len(empty) != 0 {
		t.Errorf("expected empty result for nil input, got %d", len(empty))
	}
}
