package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourcePlugin() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for checking whether a named plugin is deployed on the Janssen server (GET /plugin/{pluginName}).",
		ReadContext: dataSourcePluginRead,
		Schema: map[string]*schema.Schema{
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Name of the plugin to check.",
			},
			"deployed": {
				Type:        schema.TypeBool,
				Computed:    true,
				Description: "Whether the plugin is deployed on the server.",
			},
		},
	}
}

func dataSourcePluginRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	name := d.Get("name").(string)
	deployed, err := c.GetPlugin(ctx, name)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("deployed", deployed); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(name)

	return nil
}
