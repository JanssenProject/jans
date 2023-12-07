package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/jans/terraform-provider-jans/jans"
)

func resourceFido2Device() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceBlockCreate,
		ReadContext:   resourceFido2DeviceRead,
		UpdateContext: resourceFido2DeviceUpdate,
		DeleteContext: resourceFido2DeviceDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"id": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "The unique identifier for the fido2 device.",
			},
			"schemas": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "A list of URIs of the schemas used to define the attributes of the fido2 device.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"meta": {
				Type:        schema.TypeList,
				Computed:    true,
				Description: "A complex type that contains meta attributes associated with the fido2 device.",
				Elem: &schema.Resource{
					Schema: map[string]*schema.Schema{
						"resource_type": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The resource type of the fido2 device.",
						},
						"location": {
							Type:        schema.TypeString,
							Computed:    true,
							Description: "The URI of the fido2 device.",
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
			"counter": {
				Type:        schema.TypeInt,
				Optional:    true,
				Description: "Value used in the Fido U2F endpoints",
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
		},
	}
}

func resourceFido2DeviceRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	id := d.Id()
	device, err := c.GetFido2Device(ctx, id)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, device); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(device.ID)

	return diags
}

func resourceFido2DeviceUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var device jans.Fido2Device
	if err := fromSchemaResource(d, &device); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateFido2Device(ctx, &device); err != nil {
		return diag.FromErr(err)
	}

	return resourceFido2DeviceRead(ctx, d, meta)
}

func resourceFido2DeviceDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	id := d.Id()
	if err := c.DeleteFido2Device(ctx, id); err != nil {
		return diag.FromErr(err)
	}

	return resourceFido2DeviceRead(ctx, d, meta)
}
