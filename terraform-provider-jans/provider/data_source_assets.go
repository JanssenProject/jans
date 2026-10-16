package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

// assetDocumentSchema is the read-only representation of a Jans asset
// (config-api Document) returned by the asset data sources.
func assetDocumentSchema() map[string]*schema.Schema {
	return map[string]*schema.Schema{
		"dn":            {Type: schema.TypeString, Computed: true, Description: "Distinguished name of the asset."},
		"inum":          {Type: schema.TypeString, Computed: true, Description: "XRI i-number, unique identifier of the asset."},
		"file_name":     {Type: schema.TypeString, Computed: true, Description: "File name of the asset."},
		"file_path":     {Type: schema.TypeString, Computed: true, Description: "File path of the asset on the server."},
		"description":   {Type: schema.TypeString, Computed: true, Description: "Description of the asset."},
		"document":      {Type: schema.TypeString, Computed: true, Description: "Contents of the asset."},
		"creation_date": {Type: schema.TypeString, Computed: true, Description: "Creation date of the asset."},
		"service":       {Type: schema.TypeString, Computed: true, Description: "Service the asset belongs to."},
		"level":         {Type: schema.TypeInt, Computed: true, Description: "Level of the asset."},
		"revision":      {Type: schema.TypeInt, Computed: true, Description: "Revision of the asset."},
		"enabled":       {Type: schema.TypeBool, Computed: true, Description: "Whether the asset is enabled."},
		"base_dn":       {Type: schema.TypeString, Computed: true, Description: "Base distinguished name of the asset."},
	}
}

func flattenAssetDocuments(docs []jans.Document) []interface{} {
	out := make([]interface{}, 0, len(docs))
	for _, doc := range docs {
		out = append(out, map[string]interface{}{
			"dn":            doc.Dn,
			"inum":          doc.Inum,
			"file_name":     doc.FileName,
			"file_path":     doc.FilePath,
			"description":   doc.Description,
			"document":      doc.Document,
			"creation_date": doc.CreationDate,
			"service":       doc.Service,
			"level":         doc.Level,
			"revision":      doc.Revision,
			"enabled":       doc.Enabled,
			"base_dn":       doc.BaseDn,
		})
	}
	return out
}

func dataSourceAssets() *schema.Resource {
	return &schema.Resource{
		Description: "Data source for retrieving all Jans assets (GET /jans-assets).",
		ReadContext: dataSourceAssetsRead,
		Schema: map[string]*schema.Schema{
			"assets": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "List of all assets.",
				Elem:        &schema.Resource{Schema: assetDocumentSchema()},
			},
		},
	}
}

func dataSourceAssetsRead(ctx context.Context, d *schema.ResourceData, meta interface{}) diag.Diagnostics {
	c := meta.(*jans.Client)

	assets, err := c.GetJansAssets(ctx)
	if err != nil {
		return diag.FromErr(err)
	}

	if err := d.Set("assets", flattenAssetDocuments(assets)); err != nil {
		return diag.FromErr(err)
	}
	d.SetId("jans_assets")

	return nil
}
