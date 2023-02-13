package provider

import (
	"context"

	"github.com/hashicorp/terraform-plugin-log/tflog"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceUMAResource() *schema.Resource {

	return &schema.Resource{
		Description:   "Resource for managing OAuth UMA resources",
		CreateContext: resourceUmaResourceCreate,
		ReadContext:   resourceUmaResourceRead,
		UpdateContext: resourceUmaResourceUpdate,
		DeleteContext: resourceUmaResourceDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "",
			},
			"type": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Resource type.",
			},
			"inum": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "XRI i-number. Client Identifier to uniquely identify the UMAResource.",
			},
			"description": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Resource description.",
			},
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Resource id.",
			},
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "A human-readable name of the scope.",
			},
			"icon_uri": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "A URL for a graphic icon representing the resource.",
			},
			"scopes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Applicable resource scopes.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"scope_expression": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Resource scope expression.",
			},
			"clients": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of client assosiated with the resource.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"resources": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "List of assosiated resource.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"creator": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Resource creator or owner.",
			},
			"creation_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this resource will created.",
			},
			"expiration_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this resource will expire.",
			},
			"deletable": {
				Type:        schema.TypeBool,
				Optional:    true,
				Description: "Specifies whether client is deletable.",
			},
		},
	}
}

func resourceUmaResourceCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var uma jans.UMAResource
	if err := fromSchemaResource(d, &uma); err != nil {
		return diag.FromErr(err)
	}

	tflog.Debug(ctx, "Creating new UMAResource")
	newUmaResource, err := c.CreateUMAResource(ctx, &uma)
	if err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "New UmaResource created", map[string]interface{}{"id": newUmaResource.ID})

	d.SetId(newUmaResource.ID)

	return resourceUmaResourceRead(ctx, d, meta)
}

func resourceUmaResourceRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	id := d.Id()
	uma, err := c.GetUMAResource(ctx, id)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, uma); err != nil {
		return diag.FromErr(err)
	}
	d.SetId(uma.ID)

	return diags

}

func resourceUmaResourceUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var uma jans.UMAResource
	if err := fromSchemaResource(d, &uma); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "Updating UmaResource", map[string]interface{}{"id": uma.ID})
	if _, err := c.UpdateUMAResource(ctx, &uma); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "UmaResource updated", map[string]interface{}{"id": uma.ID})

	return resourceUmaResourceRead(ctx, d, meta)
}

func resourceUmaResourceDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	id := d.Id()
	tflog.Debug(ctx, "Deleting UmaResource", map[string]interface{}{"id": id})
	if err := c.DeleteUMAResource(ctx, id); err != nil {
		return diag.FromErr(err)
	}
	tflog.Debug(ctx, "UmaResource deleted", map[string]interface{}{"id": id})

	return resourceUmaResourceRead(ctx, d, meta)
}
