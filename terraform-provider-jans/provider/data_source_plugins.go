package provider

import (
	"context"
	"strconv"
	"time"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourcePlugins() *schema.Resource {

	return &schema.Resource{
		Description: "Data source for retrieving the plugins that are configured in the Janssen server",
		ReadContext: dataSourcePluginsRead,
		Schema: map[string]*schema.Schema{
			"enabled": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of all enabled plugins",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"name": {
							Type:        schema.TypeString,
							Description: "Name of the plugin",
							Computed:    true,
						},
						"description": {
							Type:        schema.TypeString,
							Description: "Description of the plugin",
							Computed:    true,
						},
						"class_name": {
							Type:        schema.TypeString,
							Description: "Class name of the plugin",
							Computed:    true,
						},
					},
				},
			},
		},
	}
}

func dataSourcePluginsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	pluginConfig, err := c.GetPlugins(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	plugins := jans.Plugins{
		Enabled: pluginConfig,
	}

	if err := toSchemaResource(d, plugins); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(strconv.FormatInt(time.Now().Unix(), 10))

	return nil
}
