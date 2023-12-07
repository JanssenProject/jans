package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceCustomScriptTypes() *schema.Resource {

	return &schema.Resource{
		Description: "Data source for retrieving supported custom script types.",
		ReadContext: dataSourceCustomScriptTypesRead,
		Schema: map[string]*schema.Schema{
			"types": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A list of support custom script types.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
		},
	}
}

func dataSourceCustomScriptTypesRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	providerConfig, err := c.GetServiceProviderConfig(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, providerConfig); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
