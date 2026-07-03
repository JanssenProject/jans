package provider

import (
	"context"
	"testing"
)

// 978307200 == 2001-01-01T00:00:00Z
const epoch2001 = float64(978307200)

func TestUpgradeOidcClientTimestampsV0(t *testing.T) {
	raw := map[string]interface{}{
		"last_access_time": epoch2001,
		"last_logon_time":  float64(0),
		"dn":               "inum=1,ou=clients,o=jans",
	}
	out, err := upgradeOidcClientTimestampsV0(context.Background(), raw, nil)
	if err != nil {
		t.Fatal(err)
	}
	if out["last_access_time"] != "2001-01-01T00:00:00Z" {
		t.Errorf("last_access_time: got %v", out["last_access_time"])
	}
	if out["last_logon_time"] != "" {
		t.Errorf("zero epoch should map to empty string, got %v", out["last_logon_time"])
	}
	if out["dn"] != "inum=1,ou=clients,o=jans" {
		t.Errorf("dn must be untouched, got %v", out["dn"])
	}
}

func TestUpgradeScopeClientsTimestampsV0(t *testing.T) {
	raw := map[string]interface{}{
		"clients": []interface{}{
			map[string]interface{}{"last_access_time": epoch2001, "client_name": "c1"},
		},
	}
	out, err := upgradeScopeClientsTimestampsV0(context.Background(), raw, nil)
	if err != nil {
		t.Fatal(err)
	}
	c := out["clients"].([]interface{})[0].(map[string]interface{})
	if c["last_access_time"] != "2001-01-01T00:00:00Z" {
		t.Errorf("nested last_access_time: got %v", c["last_access_time"])
	}
	if c["client_name"] != "c1" {
		t.Errorf("other nested fields must be untouched, got %v", c["client_name"])
	}
}

func TestV0TypesBuild(t *testing.T) {
	if ot := oidcClientV0Type(resourceOidcClient().Schema); !ot.HasAttribute("last_access_time") {
		t.Error("oidc_client v0 type missing last_access_time")
	}
	if st := scopeV0Type(resourceScope().Schema); !st.HasAttribute("clients") {
		t.Error("scope v0 type missing clients")
	}
}
