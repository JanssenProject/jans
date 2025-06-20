package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceCustomScriptTypes() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving all the custom script types.",
		ReadContext: dataSourceCustomScriptTypesRead,
		Schema: map[string]*schema.Schema{
			"script_types": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of all the custom script types.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func dataSourceCustomScriptTypesRead(ctx context.Context, d *schema.ResourceData, m interface{}) diag.Diagnostics {
	c := m.(*jans.Client)

	scriptTypes, err := c.GetScriptTypes(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if len(scriptTypes) == 0 {
		return diag.Errorf("no script types found")
	}

	d.SetId("custom_script_types")

	if err := d.Set("script_types", scriptTypes); err != nil {
		return diag.FromErr(err)
	}

	return nil
}
