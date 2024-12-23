package provider

import (
	"context"
	"fmt"
	"os"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceAsset() *schema.Resource {
	return &schema.Resource{
		Description:   "Resource for managing Janssen assets.",
		CreateContext: resourceJansAssetCreate,
		ReadContext:   resourceJansAssetRead,
		UpdateContext: resourceJansAssetUpdate,
		DeleteContext: resourceJansAssetDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The DN of the document.",
			},
			"inum": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The inum of the document.",
			},
			"file_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The file name of the document.",
			},
			"file_path": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The Jans file path of the document.",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The description of the document.",
			},
			"document": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The document.",
			},
			"creation_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The creation date of the document.",
			},
			"service": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The Jans service of the document.",
			},
			"level": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The Jans level of the document.",
			},
			"revision": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The Jans revision of the document.",
			},
			"enabled": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "The Jans enabled of the document.",
			},
			"alias": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The Jans alias of the document.",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "The base DN of the document.",
			},
			"asset": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "The asset file.",
			},
		},
	}
}

func resourceJansAssetRead(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {

	c := m.(*jans.Client)

	doc, err := c.GetJansAsset(ctx, d.Get("inum").(string))
	if err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, doc); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "ResourceJansAssetRead: Read document", map[string]any{"Inum": doc.Inum})

	return nil
}

func handleAsset(d *schema.ResourceData) (*jans.Document, *os.File, error) {
	pathAny, ok := d.GetOk("asset")
	if !ok {
		return nil, nil, fmt.Errorf("asset is required")
	}

	path := pathAny.(string)
	f, err := os.Open(path)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to open file: %w", err)
	}

	doc := &jans.Document{}
	if err := fromSchemaResource(d, doc); err != nil {
		return nil, nil, fmt.Errorf("failed to read document from schema: %w", err)
	}

	return doc, f, nil
}

func resourceJansAssetCreate(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {

	c := m.(*jans.Client)

	doc, f, err := handleAsset(d)
	if err != nil {
		return diag.FromErr(err)
	}
	defer f.Close()

	if doc, err = c.CreateJansAsset(ctx, *doc, f); err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, doc); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func resourceJansAssetUpdate(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {

	c := m.(*jans.Client)

	doc, f, err := handleAsset(d)
	if err != nil {
		return diag.FromErr(err)
	}
	defer f.Close()

	if doc, err = c.UpdateJansAsset(ctx, *doc, f); err != nil {
		return diag.FromErr(err)
	}

	if err := toSchemaResource(d, doc); err != nil {
		return diag.FromErr(err)
	}

	return nil
}

func resourceJansAssetDelete(ctx context.Context, d *schema.ResourceData, m any) diag.Diagnostics {

	c := m.(*jans.Client)

	if err := c.DeleteJansAsset(ctx, d.Get("inum").(string)); err != nil {
		return diag.FromErr(err)
	}

	d.SetId("")

	return nil
}
