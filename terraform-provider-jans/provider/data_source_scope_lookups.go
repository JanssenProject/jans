package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

// scopeLookupResultSchema is the (read-only) summary returned by the scope
// lookup data sources. It exposes the identifying fields callers need to then
// reference a scope by inum; it intentionally does not mirror the full scope
// resource schema.
func scopeLookupResultSchema() map[string]*schema.Schema {
	return map[string]*schema.Schema{
		"inum":         {Type: schema.TypeString, Computed: true, Description: "XRI i-number, used as the unique identifier of the scope."},
		"scope_id":     {Type: schema.TypeString, Computed: true, Description: "The base64url-encoded scope identifier."},
		"dn":           {Type: schema.TypeString, Computed: true, Description: "Distinguished name of the scope."},
		"display_name": {Type: schema.TypeString, Computed: true, Description: "Display name of the scope."},
		"description":  {Type: schema.TypeString, Computed: true, Description: "Description of the scope."},
		"scope_type":   {Type: schema.TypeString, Computed: true, Description: "The type of the scope."},
		"creator_id":   {Type: schema.TypeString, Computed: true, Description: "Id of the scope creator."},
		"creator_type": {Type: schema.TypeString, Computed: true, Description: "Type of the scope creator."},
	}
}

func flattenScopeLookup(scopes []jans.Scope) []interface{} {
	out := make([]interface{}, 0, len(scopes))
	for _, s := range scopes {
		out = append(out, map[string]interface{}{
			"inum":         s.Inum,
			"scope_id":     s.Id,
			"dn":           s.Dn,
			"display_name": s.DisplayName,
			"description":  s.Description,
			"scope_type":   s.ScopeType,
			"creator_id":   s.CreatorId,
			"creator_type": s.CreatorType,
		})
	}
	return out
}

func dataSourceScopesByCreator() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for looking up OAuth scopes by their creator id (GET /scopes/creator/{creatorId}).",
		ReadContext: dataSourceScopesByCreatorRead,
		Schema: map[string]*schema.Schema{
			"creator_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The creator id to look up scopes for.",
			},
			"scopes": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "The scopes created by the given creator.",
				Elem:        &schema.Resource{Schema: scopeLookupResultSchema()},
			},
		},
	}
}

func dataSourceScopesByCreatorRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	creatorId := d.Get("creator_id").(string)
	scopes, err := c.GetScopesByCreator(ctx, creatorId)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("scopes", flattenScopeLookup(scopes)); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(creatorId)

	return nil
}

func dataSourceScopesByType() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for looking up OAuth scopes by their scope type (GET /scopes/type/{type}).",
		ReadContext: dataSourceScopesByTypeRead,
		Schema: map[string]*schema.Schema{
			"scope_type": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The scope type to look up (e.g. openid, dynamic, spontaneous, uma).",
			},
			"scopes": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "The scopes of the given type.",
				Elem:        &schema.Resource{Schema: scopeLookupResultSchema()},
			},
		},
	}
}

func dataSourceScopesByTypeRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	scopeType := d.Get("scope_type").(string)
	scopes, err := c.GetScopesByType(ctx, scopeType)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("scopes", flattenScopeLookup(scopes)); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(scopeType)

	return nil
}
