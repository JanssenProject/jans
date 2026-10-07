package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAssetTypes() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving the valid asset types (GET /jans-assets/asset-type).",
		ReadContext: dataSourceAssetTypesRead,
		Schema: map[string]*schema.Schema{
			"asset_types": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of valid asset types.",
				Elem:        &schema.Schema{Type: schema.TypeString},
			},
		},
	}
}

func dataSourceAssetTypesRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	types, err := c.GetJansAssetTypes(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("asset_types", types); err != nil {
		return diag.FromErr(err)
	}
	d.SetId("asset_types")

	return nil
}
