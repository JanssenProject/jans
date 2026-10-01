package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func dataSourceAssetByName() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for looking up Jans assets by name (GET /jans-assets/name/{name}).",
		ReadContext: dataSourceAssetByNameRead,
		Schema: map[string]*schema.Schema{
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The asset name to look up.",
			},
			"assets": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "Assets matching the given name.",
				Elem:        &schema.Resource{Schema: assetDocumentSchema()},
			},
		},
	}
}

func dataSourceAssetByNameRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	name := d.Get("name").(string)
	assets, err := c.GetJansAssetByName(ctx, name)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("assets", flattenAssetDocuments(assets)); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(name)

	return nil
}
