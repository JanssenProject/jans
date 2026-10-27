package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAssetDirMapping() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving the valid asset type to server directory mappings (GET /jans-assets/asset-dir-mapping).",
		ReadContext: dataSourceAssetDirMappingRead,
		Schema: map[string]*schema.Schema{
			"asset_dir_mappings": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of asset directory mappings.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"directory": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Relative path to the asset base directory.",
						},
						"type": {
							Type:        schema.TypeList,
							Computed:    true,
							Description: "File extensions stored in the directory.",
							Elem:        &schema.Schema{Type: schema.TypeString},
						},
						"description": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "Description of the assets stored in the directory.",
						},
					},
				},
			},
		},
	}
}

func dataSourceAssetDirMappingRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	mappings, err := c.GetJansAssetDirMapping(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	out := make([]interface{}, 0, len(mappings))
	for _, m := range mappings {
		out = append(out, map[string]interface{}{
			"directory":   m.Directory,
			"type":        m.Type,
			"description": m.Description,
		})
	}

	if err := d.Set("asset_dir_mappings", out); err != nil {
		return diag.FromErr(err)
	}
	d.SetId("asset_dir_mapping")

	return nil
}
