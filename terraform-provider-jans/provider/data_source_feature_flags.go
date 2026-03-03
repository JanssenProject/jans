package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceFeatureFlags() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving Janssen Auth Server feature flags.",
		ReadContext: dataSourceFeatureFlagsRead,
		Schema: map[string]*schema.Schema{
			"flags": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of feature flags enabled in the Janssen Auth Server.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func dataSourceFeatureFlagsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	flags, err := c.GetFeatureFlags(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId("feature_flags")
	if err := d.Set("flags", flags); err != nil {
		return diag.FromErr(err)
	}

	return nil
}
