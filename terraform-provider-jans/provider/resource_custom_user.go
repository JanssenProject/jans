package provider

import (
	"context"

	"github.com/hashicorp/go-cty/cty"
	"github.com/hashicorp/terraform-plugin-sdk/v2/diag"
	"github.com/hashicorp/terraform-plugin-sdk/v2/helper/schema"
	"github.com/moabu/terraform-provider-jans/jans"
)

func resourceCustomUser() *schema.Resource {

	return &schema.Resource{
		CreateContext: resourceCustomUserCreate,
		ReadContext:   resourceCustomUserRead,
		UpdateContext: resourceCustomUserUpdate,
		DeleteContext: resourceCustomUserDelete,
		Importer: &schema.ResourceImporter{
			StateContext: schema.ImportStatePassthroughContext,
		},
		Schema: map[string]*schema.Schema{
			"dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Domain name.",
			},
			"base_dn": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Base distinguished name for the User entity",
			},
			"jans_status": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "User status",
				ValidateDiagFunc: func(v interface{}, p cty.Path) diag.Diagnostics {

					enums := []string{"active", "inactive", "expired", "register"}
					return validateEnum(v, enums)
				},
			},
			"user_id": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "A domain issued and managed identifier for the user.",
			},
			"created_at": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "User creation date.",
			},
			"updated_at": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "Time the information of the person was last updated. Seconds from 1970-01-01T0:0:0Z",
			},
			"ox_auth_persistent_jwt": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "Persistent JWT.",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"custom_attributes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "dn of associated clients with the user.",
				Elem:        resourceCustomAttribute(),
			},
			"custom_object_classes": {
				Type:        schema.TypeList,
				Optional:    true,
				Description: "",
				Elem: &schema.Schema{
					Type: schema.TypeString,
				},
			},
			"mail": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "User mail",
			},
			"display_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "Name of the user suitable for display to end-users",
			},
			"given_name": {
				Type:        schema.TypeString,
				Required:    true,
				Description: "User given Name",
			},
			"user_password": {
				Type:        schema.TypeString,
				Optional:    true,
				Description: "User password",
			},
			"inum": {
				Type:        schema.TypeString,
				Computed:    true,
				Description: "XRI i-number. Identifier to uniquely identify the user.",
			},
		},
	}
}

func resourceCustomUserCreate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var user jans.CustomUser
	if err := fromSchemaResource(d, &user); err != nil {
		return diag.FromErr(err)
	}

	newUser, err := c.CreateCustomUser(ctx, &user)
	if err != nil {
		return diag.FromErr(err)
	}

	d.SetId(newUser.Inum)

	return resourceCustomUserRead(ctx, d, meta)
}

func resourceCustomUserRead(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var diags diag.Diagnostics

	inum := d.Id()
	attr, err := c.GetCustomUser(ctx, inum)
	if err != nil {
		return handleNotFoundError(ctx, err, d)
	}

	if err := toSchemaResource(d, attr); err != nil {
		return diag.FromErr(err)
	}

	d.SetId(attr.Inum)

	return diags
}

func resourceCustomUserUpdate(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	var user jans.CustomUser
	if err := fromSchemaResource(d, &user); err != nil {
		return diag.FromErr(err)
	}

	if _, err := c.UpdateCustomUser(ctx, &user); err != nil {
		return diag.FromErr(err)
	}

	return resourceCustomUserRead(ctx, d, meta)
}

func resourceCustomUserDelete(ctx context.Context, d *schema.ResourceData, meta any) diag.Diagnostics {

	c := meta.(*jans.Client)

	inum := d.Id()
	if err := c.DeleteCustomUser(ctx, inum); err != nil {
		return diag.FromErr(err)
	}

	return resourceCustomUserRead(ctx, d, meta)
}
