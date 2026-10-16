package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAssetServices() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving the asset services configured on the server (GET /jans-assets/services).",
		ReadContext: dataSourceAssetServicesRead,
		Schema: map[string]*schema.Schema{
			"services": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of asset services.",
				Elem:        &schema.Schema{Type: schema.TypeString},
			},
		},
	}
}

func dataSourceAssetServicesRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	services, err := c.GetJansAssetServices(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("services", services); err != nil {
		return diag.FromErr(err)
	}
	d.SetId("asset_services")

	return nil
}
