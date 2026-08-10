package provider

import (
	"context"
	"time"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
)

// last_access_time / last_logon_time changed from an epoch integer (schema v0,
// shipped in 1.x/2.x) to an RFC3339 string. The state upgraders below convert
// existing state so users don't hit a type error on the first plan after upgrade.

// epochToRFC3339 converts a v0 epoch-seconds value (a JSON number in prior state)
// to the RFC3339 string the v1 schema expects. Zero/empty -> ""; an already-string
// value (re-run / already migrated) passes through unchanged.
func epochToRFC3339(v interface{}) interface{} {
	var sec int64
	switch n := v.(type) {
	case float64:
		sec = int64(n)
	case int:
		sec = int64(n)
	case int64:
		sec = n
	case string:
		return n
	default:
		return ""
	}
	if sec <= 0 {
		return ""
	}
	return time.Unix(sec, 0).UTC().Format(time.RFC3339)
}

// timestampSchemaV0 is the pre-v1 shape of the two timestamp fields (epoch int).
func timestampSchemaV0() *schema.Schema {
	return &schema.Schema{Type: schema.TypeInt, Optional: true}
}

// oidcClientV0Type is the oidc_client schema type before the timestamp fields
// became strings. It is derived from the live schema map (passed in) so it can
// never drift from it, and without recursively constructing the resource.
func oidcClientV0Type(s map[string]*schema.Schema) cty.Type {
	v0 := make(map[string]*schema.Schema, len(s))
	for k, v := range s {
		v0[k] = v
	}
	v0["last_access_time"] = timestampSchemaV0()
	v0["last_logon_time"] = timestampSchemaV0()
	return (&schema.Resource{Schema: v0}).CoreConfigSchema().ImpliedType()
}

func upgradeOidcClientTimestampsV0(_ context.Context, rawState map[string]interface{}, _ interface{}) (map[string]interface{}, error) {
	migrateTimestamps(rawState)
	return rawState, nil
}

// scopeV0Type mirrors oidcClientV0Type, but the timestamp fields live inside the
// embedded oidc_client schema of the nested "clients" block.
func scopeV0Type(s map[string]*schema.Schema) cty.Type {
	v0 := make(map[string]*schema.Schema, len(s))
	for k, v := range s {
		v0[k] = v
	}
	if clients := s["clients"]; clients != nil {
		if res, ok := clients.Elem.(*schema.Resource); ok {
			nested := make(map[string]*schema.Schema, len(res.Schema))
			for k, v := range res.Schema {
				nested[k] = v
			}
			nested["last_access_time"] = timestampSchemaV0()
			nested["last_logon_time"] = timestampSchemaV0()
			v0["clients"] = &schema.Schema{
				Type:     clients.Type,
				Optional: clients.Optional,
				Computed: clients.Computed,
				Elem:     &schema.Resource{Schema: nested},
			}
		}
	}
	return (&schema.Resource{Schema: v0}).CoreConfigSchema().ImpliedType()
}

func upgradeScopeClientsTimestampsV0(_ context.Context, rawState map[string]interface{}, _ interface{}) (map[string]interface{}, error) {
	if rawState != nil {
		if clients, ok := rawState["clients"].([]interface{}); ok {
			for _, c := range clients {
				if cm, ok := c.(map[string]interface{}); ok {
					migrateTimestamps(cm)
				}
			}
		}
	}
	return rawState, nil
}

func migrateTimestamps(m map[string]interface{}) {
	if m == nil {
		return
	}
	for _, k := range []string{"last_access_time", "last_logon_time"} {
		if v, ok := m[k]; ok {
			m[k] = epochToRFC3339(v)
		}
	}
}
