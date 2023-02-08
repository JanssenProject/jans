package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func resourceFidoDevice() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceFidoDeviceRead,
		UpdateContext: resourceFidoDeviceUpdate,
		DeleteContext: resourceFidoDeviceDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The unique identifier for the fido device.",
			},
			"schemas": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of URIs of the schemas used to define the attributes of the fido device.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"meta": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that contains meta attributes associated with the fido device.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"resource_type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The resource type of the fido device.",
						},
						"location": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the fido device.",
						},
						"created": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the group was created.",
						},
						"last_modified": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The date and time the group was last modified.",
						},
					},
				},
			},
			"user_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Identifies the owner of the enrollment",
			},
			"creation_date": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Date of enrollment in ISO format",
			},
			"application": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Associated U2F application ID",
			},
			"counter": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Value used in the Fido U2F endpoints",
			},
			"device_data": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "A Json representation of low-level attributes of this enrollment",
			},
			"device_hash_code": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "",
			},
			"device_key_handle": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"device_registration_conf": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
			"last_access_time": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "When this device was last used (eg. in order to log into an application)",
			},
			"status": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {
					enums := []string{"active", "compromised"}
					return validateEnum(v, enums)
				},
			},
			"display_name": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "Device name suitable for display to end-users",
			},
			"description": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "",
			},
		},
	}
}

func resourceFidoDeviceRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	id := d.Id()
	device, err := c.GetFidoDevice(ctx, id)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, device); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(device.ID)

	return diags
}

func resourceFidoDeviceUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var device jans.FidoDevice
	if err := fromSchemaResource(d, &device); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateFidoDevice(ctx, &device); err != nil {
		return diag.FromErr(err)
	}

	return resourceFidoDeviceRead(ctx, d, meta)
}

func resourceFidoDeviceDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	id := d.Id()
	if err := c.DeleteFidoDevice(ctx, id); err != nil {
		return diag.FromErr(err)
	}

	return resourceFidoDeviceRead(ctx, d, meta)
}
